package com.example.demo.service;

import com.example.demo.client.SimulatedCotizacionClient;
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.entity.Calificacion;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    private final NotificacionesService notificacionesService;
    private final PrestadorRepository prestadorRepository;
    private final SolicitudRepository solicitudRepository;
    private final SimulatedCotizacionClient cotizacionClient;
    private final SimulatedSolicitudesClient solicitudesClient;

    public SolicitudService(PrestadorRepository prestadorRepository,
                            SolicitudRepository solicitudRepository,
                            SimulatedCotizacionClient cotizacionClient,
                            SimulatedSolicitudesClient solicitudesClient,
                            NotificacionesService notificacionesService) {
        this.prestadorRepository = prestadorRepository;
        this.solicitudRepository = solicitudRepository;
        this.cotizacionClient = cotizacionClient;
        this.solicitudesClient = solicitudesClient;
        this.notificacionesService = notificacionesService;
    }

    /**
     * Toma TODAS las solicitudes CREADAS.
     * - Si tienen un prestador asignado, se marcan como ASIGNADA y se notifica solo a ese.
     * - Si no, se pasan a COTIZANDO y se envía invitación al Top-3.
     * Si no hay candidatos, la solicitud se mantiene en CREADA.
     */
    @Transactional
    public List<SolicitudTop3Resultado> procesarTodasLasCreadas() {
        List<Solicitud> creadas = solicitudesClient.obtenerSolicitudesCreadas();
        log.info("Procesando {} solicitudes en estado CREADA", creadas.size());

        return creadas.stream()
                .map(this::procesarUnaSolicitud)
                .collect(Collectors.toList());
    }

    // =======================
    // Helpers privados
    // =======================

    private SolicitudTop3Resultado procesarUnaSolicitud(Solicitud solicitud) {
        // Caso 1: viene con prestador asignado → flujo directo
        Long prestadorAsignadoId = obtenerPrestadorAsignadoId(solicitud);
        if (prestadorAsignadoId != null) {
            return procesarConPrestadorAsignado(solicitud, prestadorAsignadoId);
        }

        // Caso 2: flujo normal Top-3
        Long rubroId = Objects.requireNonNull(
                solicitud.getCategoriaId(),
                "categoriaId (rubro) requerido en solicitud " + solicitud.getId()
        );

        List<Prestador> candidatos = prestadorRepository.findByRubroIdNative(rubroId);
        
        List<Prestador> top3 = candidatos.stream()
                .sorted(Comparator.comparingDouble(SolicitudService::promedioCalificaciones).reversed())
                .limit(3)
                .collect(Collectors.toList());

        if (top3.isEmpty()) {
            log.warn("Sin candidatos para rubro {} (solicitud {}) — se mantiene en CREADA",
                    rubroId, solicitud.getId());

            SolicitudTop3Resultado out = new SolicitudTop3Resultado();
            out.setSolicitudId(solicitud.getId());
            out.setDescripcion(solicitud.getDescripcion());
            out.setEstado(solicitud.getEstado().name()); // seguirá CREADA
            out.setTop3(List.of());
            return out;
        }

        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitudRepository.save(solicitud);

        List<InvitacionCotizacionDTO> invitaciones = top3.stream()
                .map(p -> buildInvitacionDTO(solicitud, rubroId, p, "Invitación a cotizar"))
                .peek(dto -> dto.setEnviado(cotizacionClient.enviarInvitacion(dto)))
                .peek(dto -> {
            // Si ya tenés cotizacionId real, usalo. Si no, podés usar solicitudId temporalmente.
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
                Objects.requireNonNull(solicitud.getCategoriaId()),
                p,
                "Asignación directa de la solicitud por favor cotizar"
        );

        // 1) Enviar invitación (simulado)
        aviso.setEnviado(cotizacionClient.enviarInvitacion(aviso));

        // 2) Crear notificación interna para otros módulos
        //    Usamos cotizacionId si viene; si no, usamos solicitudId como referencia.
        Long refCotizacion = (aviso.getCotizacionId() != null)
                ? aviso.getCotizacionId()
                : aviso.getSolicitudId();

            notificacionesService.crearNotificacion(
                com.example.demo.entity.Notificaciones.builder()
                        .cotizacionId(refCotizacion) // si tu columna es NOT NULL y no tenés cotización creada, usar solicitudId como referencia
                        .titulo("Asignación directa: invitación enviada")
                        .mensaje("Se notificó al prestador " + aviso.getPrestadorId()
                                + " por la solicitud " + aviso.getSolicitudId())
                        .leida(false)
                        .build()
        );

        // 3) Actualizar estado y persistir
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

    private static double promedioCalificaciones(Prestador p) {
        List<Calificacion> cals = p.getCalificaciones();
        if (cals == null || cals.isEmpty()) return 0.0;
        return cals.stream()
                .filter(c -> c.getPuntuacion() != null)
                .mapToInt(Calificacion::getPuntuacion)
                .average().orElse(0.0);
    }

    private Long obtenerPrestadorAsignadoId(Solicitud s) {
        try {
            if (s.getPrestadorAsignadoId() != null) {
                return s.getPrestadorAsignadoId();
            }
        } catch (Exception ignored) {}
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
}
