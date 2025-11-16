package com.example.demo.service;

import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SolicitudAutoCompletionJob {

    private final SolicitudRepository solicitudRepository;
    private final PrestadorRepository prestadorRepository;

    private static final List<EstadoSolicitud> ELIGIBLE_STATES = List.of(
            EstadoSolicitud.ASIGNADA,
            EstadoSolicitud.EN_PROGRESO
    );

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void markCompletedAfterDueDate() {
        List<Solicitud> candidates = solicitudRepository.findAutoCompletionCandidates(ELIGIBLE_STATES);
        if (candidates.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Solicitud solicitud : candidates) {
            LocalDate fecha = solicitud.getFecha();
            if (fecha == null) {
                continue;
            }
            LocalTime horario = solicitud.getHorario() != null ? solicitud.getHorario() : LocalTime.MAX;
            LocalDateTime dueAt = LocalDateTime.of(fecha, horario);
            if (dueAt.isAfter(now)) {
                continue;
            }
            Long prestadorExternalId = solicitud.getPrestadorAsignadoId();
            if (prestadorExternalId == null) {
                continue;
            }
            Prestador prestador = prestadorRepository.findByExternalId(prestadorExternalId).orElse(null);
            if (prestador == null) {
                log.warn("No se encontró prestador {} para auto-completar solicitud {}", prestadorExternalId, solicitud.getId());
                continue;
            }
            Integer trabajos = prestador.getTrabajosFinalizados();
            prestador.setTrabajosFinalizados((trabajos == null ? 0 : trabajos) + 1);
            prestadorRepository.save(prestador);

            solicitud.setAutoCompleted(true);
            solicitud.setAutoCompletedAt(now);
            solicitudRepository.save(solicitud);

            log.info("Solicitud {} vencida auto-completada. Prestador {} ahora tiene {} trabajos finalizados.",
                    solicitud.getId(), prestadorExternalId, prestador.getTrabajosFinalizados());
        }
    }
}
