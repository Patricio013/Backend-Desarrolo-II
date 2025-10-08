package com.example.demo.websocket;

import com.example.demo.entity.Solicitud;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SolicitudEventsPublisher {

    private static final Logger log = LoggerFactory.getLogger(SolicitudEventsPublisher.class);
    private final SimpMessagingTemplate messagingTemplate;

    // Enviar evento completo con el formato solicitado
    public void notifySolicitudEvent(
            Solicitud solicitud,
            String type,
            String title,
            String description,
            Map<String, Object> details
    ) {
        if (solicitud == null || solicitud.getId() == null) return;
        try {
            Map<String, Object> safeDetails = new HashMap<>();
            if (details != null) safeDetails.putAll(details);
            safeDetails.putIfAbsent("solicitudId", solicitud.getId());

            WsEvent payload = new WsEvent(
                    type,
                    solicitud.getEstado() != null ? solicitud.getEstado().name() : null,
                    title,
                    description,
                    Collections.unmodifiableMap(safeDetails)
            );

            log.info("WS -> solicitudId={} type={} status={} detalles={}",
                    solicitud.getId(),
                    payload.type(),
                    payload.status(),
                    payload.details());
            // Notificación general (para listas)
            messagingTemplate.convertAndSend("/topic/solicitudes", payload);
            // Notificación específica por ID (para detalle)
            messagingTemplate.convertAndSend("/topic/solicitudes/" + solicitud.getId(), payload);
        } catch (Exception e) {
            log.warn("No se pudo enviar evento WS para solicitud {}: {}", solicitud.getId(), e.getMessage());
        }
    }

    // Método de compatibilidad mínima
    public void notifyUpdated(Solicitud solicitud) {
        notifySolicitudEvent(solicitud,
                "SOLICITUD_UPDATED",
                "Solicitud actualizada",
                "La solicitud cambió",
                Map.of("solicitudId", solicitud != null ? solicitud.getId() : null));
    }

    public record WsEvent(
            String type,
            String status,
            String title,
            String description,
            Map<String, Object> details
    ) {}
}
