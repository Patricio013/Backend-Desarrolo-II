package com.example.demo.service;

import com.example.demo.client.SimulatedCotizacionClient;
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
            solicitud.getCategoriaId(),
            "categoriaId (rubro) requerido en solicitud " + solicitud.getId()
        );

        List<Prestador> top3 = prestadorRepository.findTopByRubroRanked(
            rubroId, PageRequest.of(0, 3)
        );

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
            Objects.requireNonNull(solicitud.getCategoriaId()),
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
}
