package com.example.demo.service;

import com.example.demo.client.SimulatedCotizacionClient;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private final PrestadorRepository prestadorRepository;
    private final SolicitudRepository solicitudRepository;
    private final SimulatedCotizacionClient cotizacionClient;
    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    public SolicitudService(PrestadorRepository prestadorRepository,
                            SolicitudRepository solicitudRepository,
                            SimulatedCotizacionClient cotizacionClient) {
        this.prestadorRepository = prestadorRepository;
        this.solicitudRepository = solicitudRepository;
        this.cotizacionClient = cotizacionClient;
    }

    // Crea 3 solicitudes para los 3 mejores prestadores del rubro (flujo alternativo)
    @Transactional
    public List<Solicitud> crearSolicitudesParaTop3Prestadores(Long usuarioId,
                                                               Long rubroId,
                                                               Long servicioId,
                                                               String descripcion) {
        // Usar nativa para evitar problemas de mapeo en p.habilidades
        List<Prestador> candidatos = prestadorRepository.findByRubroIdNative(rubroId);
        log.info("Candidatos encontrados para rubro {}: {}", rubroId, candidatos.size());
        List<Prestador> top3 = candidatos.stream()
                .sorted(Comparator.comparingDouble(SolicitudService::promedioCalificaciones).reversed())
                .limit(3)
                .collect(Collectors.toList());

        return top3.stream().map(p -> Solicitud.builder()
                        .usuarioId(Objects.requireNonNull(usuarioId, "usuarioId requerido"))
                        .servicioId(Objects.requireNonNull(servicioId, "servicioId requerido"))
                        .categoriaId(Objects.requireNonNull(rubroId, "rubroId requerido"))
                        .descripcion(descripcion)
                        .estado(EstadoSolicitud.COTIZANDO)
                        .prestadorAsignadoId(p.getId())
                        .build())
                .map(solicitudRepository::save)
                .collect(Collectors.toList());
    }

    // Flujo pedido: llega una solicitud en estado CREADA, se buscan top 3 y se envían invitaciones simuladas
    @Transactional
    public List<InvitacionCotizacionDTO> invitarTop3ParaCotizar(Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.CREADA) {
            throw new IllegalStateException("La solicitud debe estar en estado CREADA para invitar a cotizar");
        }

        Long rubroId = Objects.requireNonNull(solicitud.getCategoriaId(), "categoriaId (rubro) requerido");

        // Usar nativa para evitar problemas de mapeo en p.habilidades
        List<Prestador> candidatos = prestadorRepository.findByRubroIdNative(rubroId);
        log.info("Candidatos encontrados para rubro {} (solicitud {}): {}", rubroId, solicitudId, candidatos.size());
        List<Prestador> top3 = candidatos.stream()
                .sorted(Comparator.comparingDouble(SolicitudService::promedioCalificaciones).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Actualizar estado a COTIZANDO al iniciar el proceso de invitación
        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitudRepository.save(solicitud);

        // Enviar invitaciones simuladas
        return top3.stream().map(p -> {
            InvitacionCotizacionDTO dto = InvitacionCotizacionDTO.builder()
                    .solicitudId(solicitud.getId())
                    .rubroId(rubroId)
                    .prestadorId(p.getId())
                    .prestadorNombre(p.getNombre() + " " + p.getApellido())
                    .mensaje("Invitación a cotizar la solicitud " + solicitud.getId())
                    .timestamp(LocalDateTime.now())
                    .enviado(false)
                    .build();
            boolean ok = cotizacionClient.enviarInvitacion(dto);
            dto.setEnviado(ok);
            return dto;
        }).collect(Collectors.toList());
    }

    // Expuesto para el endpoint de debug
    @Transactional(readOnly = true)
    public List<Prestador> buscarPrestadoresPorRubro(Long rubroId) {
        List<Prestador> lista = prestadorRepository.findByRubroIdNative(rubroId);
        log.info("[DEBUG] findByRubroIdNative({}) -> {} prestadores", rubroId, lista.size());
        return lista;
    }

    private static double promedioCalificaciones(Prestador p) {
        List<com.example.demo.entity.Calificacion> cals = p.getCalificaciones();
        if (cals == null || cals.isEmpty()) return 0.0;
        return cals.stream()
                .filter(c -> c.getPuntuacion() != null)
                .mapToInt(Calificacion::getPuntuacion)
                .average().orElse(0.0);
    }
}
