package com.example.demo.service;

import com.example.demo.client.SimulatedCotizacionClient;
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.RubroRepository;
import com.example.demo.repository.SolicitudRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    @Autowired private NotificacionesService notificacionesService;
    @Autowired private PrestadorRepository prestadorRepository;
    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private SimulatedCotizacionClient cotizacionClient;
    @Autowired private SimulatedSolicitudesClient solicitudesClient;
    @Autowired private RubroRepository rubroService;

    @Transactional
    public SolicitudTop3Resultado recotizar(Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.CANCELADA) {
            // podés relajar esto si querés recotizar igual
            throw new IllegalStateException("La solicitud no está CANCELADA");
        }

        Long rubroId = Objects.requireNonNull(solicitud.getRubroId(), "rubroId requerido");

        // Base: excluí todos los que YA COTIZARON esta solicitud
        List<Prestador> candidatos = prestadorRepository.findTopByRubroExcluyendoLosQueCotizaron(
            rubroId, solicitud.getId(), PageRequest.of(0, 10) // traigo 10 por si alguno lo filtro luego
        );

        // Extra: si hubo asignado directo y querés excluirlo aunque no haya cotizado
        Long asignado = obtenerPrestadorAsignadoId(solicitud);
        if (asignado != null) {
            candidatos = candidatos.stream().filter(p -> !p.getId().equals(asignado)).toList();
        }
        candidatos = candidatos.stream()
            .filter(p -> estaLibre(p, solicitud))
            .toList();

        List<Prestador> top3 = candidatos.stream().limit(3).toList();
        if (top3.isEmpty()) {
            var out = new SolicitudTop3Resultado();
            out.setSolicitudId(solicitud.getId());
            out.setDescripcion(solicitud.getDescripcion());
            out.setEstado(solicitud.getEstado().name()); // sigue CANCELADA
            out.setTop3(List.of());
            return out;
        }

        // Volver a COTIZANDO y enviar
        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitudRepository.save(solicitud);

        var invitaciones = top3.stream()
            .map(p -> buildInvitacionDTO(solicitud, rubroId, p, "Recotización: invitación a cotizar"))
            .peek(dto -> {
                dto.setEnviado(cotizacionClient.enviarInvitacion(dto));
                Long ref = dto.getCotizacionId() != null ? dto.getCotizacionId() : dto.getSolicitudId();
                notificacionesService.notificarInvitacionCotizacion(
                    ref,
                    "Recotización: invitación enviada",
                    "Se envió invitación al prestador " + dto.getPrestadorId() + " para la solicitud " + dto.getSolicitudId()
                );
            })
            .collect(Collectors.toList());

        var out = new SolicitudTop3Resultado();
        out.setSolicitudId(solicitud.getId());
        out.setDescripcion(solicitud.getDescripcion());
        out.setEstado(solicitud.getEstado().name()); // COTIZANDO
        out.setTop3(invitaciones);
        return out;
    }


    @Transactional
    public List<SolicitudTop3Resultado> procesarTodasLasCreadas() {
        List<Solicitud> creadas = solicitudesClient.obtenerSolicitudesCreadas();
        log.info("Procesando {} solicitudes en estado CREADA", creadas.size());
        return creadas.stream().map(this::procesarUnaSolicitud).collect(Collectors.toList());
    }

    private SolicitudTop3Resultado procesarUnaSolicitud(Solicitud solicitud) {
        Long prestadorAsignadoId = obtenerPrestadorAsignadoId(solicitud);
        if (prestadorAsignadoId != null) {
            return procesarConPrestadorAsignado(solicitud, prestadorAsignadoId);
        }

        Long rubroId = Objects.requireNonNull(
            solicitud.getRubroId(),
            "categoriaId (rubro) requerido en solicitud " + solicitud.getId()
        );

        List<Prestador> candidatos = prestadorRepository.findTopByRubroRanked(
            rubroId, PageRequest.of(0, 10) // pedí más de 3 para que haya margen al filtrar
        );

        List<Prestador> top3 = candidatos.stream()
            .filter(p -> estaLibre(p, solicitud))
            .limit(3)
            .toList();

        if (top3.isEmpty()) {
            log.warn("Sin candidatos ACTIVO para rubro {} (solicitud {}) — se mantiene en CREADA", rubroId, solicitud.getId());
            SolicitudTop3Resultado out = new SolicitudTop3Resultado();
            out.setSolicitudId(solicitud.getId());
            out.setDescripcion(solicitud.getDescripcion());
            out.setEstado(solicitud.getEstado().name());
            out.setTop3(List.of());
            return out;
        }

        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitudRepository.save(solicitud);

        var invitaciones = top3.stream()
            .map(p -> buildInvitacionDTO(solicitud, rubroId, p, "Invitación a cotizar"))
            .peek(dto -> dto.setEnviado(cotizacionClient.enviarInvitacion(dto)))
            .peek(dto -> {
                Long cotizacionId = dto.getCotizacionId() != null ? dto.getCotizacionId() : dto.getSolicitudId();
                notificacionesService.notificarInvitacionCotizacion(
                    cotizacionId,
                    "Invitación de cotización enviada",
                    "Se envió invitación al prestador " + dto.getPrestadorId() + " para la solicitud " + dto.getSolicitudId()
                );
            })
            .collect(Collectors.toList());

        SolicitudTop3Resultado out = new SolicitudTop3Resultado();
        out.setSolicitudId(solicitud.getId());
        out.setDescripcion(solicitud.getDescripcion());
        out.setEstado(solicitud.getEstado().name()); // COTIZANDO
        out.setTop3(invitaciones);
        return out;
    }

    private SolicitudTop3Resultado procesarConPrestadorAsignado(Solicitud solicitud, Long prestadorId) {
        Prestador p = prestadorRepository.findById(prestadorId).orElse(null);

        SolicitudTop3Resultado out = new SolicitudTop3Resultado();
        out.setSolicitudId(solicitud.getId());
        out.setDescripcion(solicitud.getDescripcion());

        if (p == null) {
            log.error("prestador_asignado_id={} no existe (solicitud {})", prestadorId, solicitud.getId());
            out.setEstado(solicitud.getEstado().name());
            out.setTop3(List.of());
            return out;
        }

        InvitacionCotizacionDTO aviso = buildInvitacionDTO(
            solicitud,
            Objects.requireNonNull(solicitud.getRubroId()),
            p,
            "Asignación directa de la solicitud por favor cotizar"
        );

        aviso.setEnviado(cotizacionClient.enviarInvitacion(aviso));

        Long refCotizacion = (aviso.getCotizacionId() != null) ? aviso.getCotizacionId() : aviso.getSolicitudId();
        notificacionesService.crearNotificacion(
            com.example.demo.entity.Notificaciones.builder()
                .cotizacionId(refCotizacion)
                .titulo("Asignación directa: invitación enviada")
                .mensaje("Se notificó al prestador " + aviso.getPrestadorId()
                        + " por la solicitud " + aviso.getSolicitudId())
                .leida(false)
                .build()
        );

        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitudRepository.save(solicitud);

        out.setEstado(solicitud.getEstado().name());
        out.setTop3(List.of(aviso));
        return out;
    }

    private static InvitacionCotizacionDTO buildInvitacionDTO(Solicitud s, Long rubroId, Prestador p, String mensajeBase) {
        return InvitacionCotizacionDTO.builder()
            .solicitudId(s.getId())
            .rubroId(rubroId)
            .prestadorId(p.getId())
            .prestadorNombre(p.getNombre() + " " + p.getApellido())
            .mensaje(mensajeBase + " " + s.getId())
            .timestamp(LocalDateTime.now())
            .enviado(false)
            .build();
    }

    private Long obtenerPrestadorAsignadoId(Solicitud s) {
        try { if (s.getPrestadorAsignadoId() != null) return s.getPrestadorAsignadoId(); } catch (Exception ignored) {}
        try {
            var m = Solicitud.class.getMethod("getPrestadorAsignadoId");
            Object v = m.invoke(s);
            if (v instanceof Long) return (Long) v;
        } catch (Exception ignored) {}
        return null;
    }

    public void cancelarPorId(Long solicitudId){
        Optional<Solicitud> Opt = solicitudRepository.findById(solicitudId);
        Solicitud s = Opt.get();

        if (Opt.isEmpty()){
            System.out.println("solicitud no encontrada.");
        }else if(s.getEstado() == EstadoSolicitud.COMPLETADA){
            System.out.println("no se puede cancelar una solicitu completada.");
        }else if (s.getEstado() == EstadoSolicitud.CANCELADA){
            System.out.println("la solicitud ya esta cancelada.");
        }else{
            s.setEstado(EstadoSolicitud.CANCELADA);
            System.out.println("solicitud cancelada correctamente");
        }
        s.setEstado(EstadoSolicitud.CANCELADA);
        solicitudRepository.save(s);
    }

    private boolean estaLibre(Prestador prestador, Solicitud solicitud) {
        if (solicitud.getPreferenciaDia() == null ||
            solicitud.getPreferenciaDesde() == null ||
            solicitud.getPreferenciaHasta() == null) {
            return true; // si no hay preferencia, no filtramos
        }
    
        List<Solicitud> asignadas = solicitudRepository.findAsignadasEnDiaYFranja(
            prestador.getId(),
            solicitud.getPreferenciaDia(),
            solicitud.getPreferenciaDesde(),
            solicitud.getPreferenciaHasta()
        );
    
        return asignadas.isEmpty(); // libre si no hay choque
    }

    /**
     * Procesa un lote de solicitudes y las persiste.
     * Si alguna ya existe (mismo ID), la ignora (idempotencia).
     */
    @Transactional
    public List<Solicitud> crearDesdeEventos(List<SolicitudesCreadasDTO> eventos) {
        return eventos.stream()
            .map(this::mapearYGuardar)
            .collect(Collectors.toList());
    }

    private Solicitud mapearYGuardar(SolicitudesCreadasDTO e) {
        if (solicitudRepository.existsById(e.getSolicitudId())) {
            return solicitudRepository.findById(e.getSolicitudId()).orElse(null);
        }

        Solicitud.SolicitudBuilder b = Solicitud.builder()
            .id(e.getSolicitudId())
            .usuarioId(e.getUsuarioId())
            .rubroId(rubroService.findByNombre(e.getRubro()).getId())
            .descripcion(e.getDescripcion())
            .estado(EstadoSolicitud.CREADA)
            .prestadorAsignadoId(e.getPrestadorId());

        // Preferencia horaria
        var ph = e.getPreferenciaHoraria();
        if (ph != null) {
            if (ph.getDia() != null && !ph.getDia().isBlank()) {
                b.preferenciaDia(LocalDate.parse(ph.getDia()));
            }
            if (ph.getVentana() != null && !ph.getVentana().isBlank()) {
                b.preferenciaVentanaStr(ph.getVentana());
                var v = parseVentana(ph.getVentana());
                b.preferenciaDesde(v.desde);
                b.preferenciaHasta(v.hasta);
            }
        }

        return solicitudRepository.save(b.build());
    }

    private Ventana parseVentana(String ventana) {
        String[] p = ventana.split("-");
        if (p.length != 2) throw new IllegalArgumentException("Ventana inválida: " + ventana);
        LocalTime desde = LocalTime.parse(p[0].trim());
        LocalTime hasta = LocalTime.parse(p[1].trim());
        return new Ventana(desde, hasta);
    }

    private record Ventana(LocalTime desde, LocalTime hasta) {}
}
