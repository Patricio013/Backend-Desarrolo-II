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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    private final PrestadorRepository prestadorRepository;
    private final SolicitudRepository solicitudRepository;
    private final SimulatedCotizacionClient cotizacionClient;
    private final SimulatedSolicitudesClient solicitudesClient;

    public SolicitudService(PrestadorRepository prestadorRepository,
                            SolicitudRepository solicitudRepository,
                            SimulatedCotizacionClient cotizacionClient,
                            SimulatedSolicitudesClient solicitudesClient) {
        this.prestadorRepository = prestadorRepository;
        this.solicitudRepository = solicitudRepository;
        this.cotizacionClient = cotizacionClient;
        this.solicitudesClient = solicitudesClient;
    }

    /**
     * Toma TODAS las solicitudes CREADAS, las pasa a COTIZANDO
     * y envía invitación al Top-3 de prestadores por rubro.
     * Si no hay candidatos, la solicitud se mantiene en CREADA.
     */
    @Transactional
    public List<SolicitudTop3Resultado> procesarTodasLasCreadas() {
        List<Solicitud> creadas = solicitudesClient.obtenerSolicitudesCreadas(); // o usar repo si preferís
        log.info("Procesando {} solicitudes en estado CREADA", creadas.size());

        return creadas.stream()
                .map(this::procesarUnaSolicitud)
                .collect(Collectors.toList());
    }

    // =======================
    // Helpers privados
    // =======================

    private SolicitudTop3Resultado procesarUnaSolicitud(Solicitud solicitud) {
        Long rubroId = Objects.requireNonNull(
                solicitud.getCategoriaId(),
                "categoriaId (rubro) requerido en solicitud " + solicitud.getId()
        );

        // Candidatos por rubro
        List<Prestador> candidatos = prestadorRepository.findByRubroIdNative(rubroId);

        // Ranking por promedio de calificaciones DESC y Top-3
        List<Prestador> top3 = candidatos.stream()
                .sorted(Comparator.comparingDouble(SolicitudService::promedioCalificaciones).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Si no hay candidatos: NO cambiar estado, devolver vacío
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

        // Con candidatos: cambiar a COTIZANDO y enviar invitaciones
        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitudRepository.save(solicitud);

        List<InvitacionCotizacionDTO> invitaciones = top3.stream()
                .map(p -> buildInvitacionDTO(solicitud, rubroId, p))
                .peek(dto -> dto.setEnviado(cotizacionClient.enviarInvitacion(dto)))
                .collect(Collectors.toList());

        SolicitudTop3Resultado out = new SolicitudTop3Resultado();
        out.setSolicitudId(solicitud.getId());
        out.setDescripcion(solicitud.getDescripcion());
        out.setEstado(solicitud.getEstado().name()); // ahora COTIZANDO
        out.setTop3(invitaciones);
        return out;
    }

    private static InvitacionCotizacionDTO buildInvitacionDTO(Solicitud s, Long rubroId, Prestador p) {
        return InvitacionCotizacionDTO.builder()
                .solicitudId(s.getId())
                .rubroId(rubroId)
                .prestadorId(p.getId())
                .prestadorNombre(p.getNombre() + " " + p.getApellido())
                .mensaje("Invitación a cotizar la solicitud " + s.getId())
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
}
