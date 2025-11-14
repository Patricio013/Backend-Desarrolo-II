package com.example.demo.service;

import com.example.demo.entity.WebhookEvent;
import com.example.demo.repository.WebhookEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventService {

    private final WebhookEventRepository repository;
    private final ObjectMapper objectMapper;

    public WebhookEvent storeEvent(String topic,
                                   String eventName,
                                   String messageId,
                                   String subscriptionId,
                                   Map<String, Object> payload) {
        String raw = serialize(payload);
        String action = determineAction(payload);
        boolean processed = action != null;

        WebhookEvent entity = WebhookEvent.builder()
                .topic(trimToNull(topic))
                .eventName(trimToNull(eventName))
                .messageId(trimToNull(messageId))
                .subscriptionId(trimToNull(subscriptionId))
                .rawPayload(raw)
                .processed(processed)
                .processingAction(action)
                .processedAt(processed ? LocalDateTime.now() : null)
                .build();
        WebhookEvent saved = repository.save(entity);
        log.info("Webhook event stored id={} topic={} eventName={} messageId={} subscriptionId={}",
                saved.getId(),
                saved.getTopic(),
                saved.getEventName(),
                saved.getMessageId(),
                saved.getSubscriptionId());
        return saved;
    }

    public List<WebhookEvent> listEvents() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "receivedAt"));
    }

    public Optional<WebhookEvent> findById(Long id) {
        return repository.findById(id);
    }

    public List<WebhookEvent> findUnprocessed(int limit) {
        int pageSize = Math.max(1, Math.min(limit, 500));
        return repository.findByProcessedFalse(PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "receivedAt"))).getContent();
    }

    public void markProcessed(Long id, String action) {
        repository.findById(id).ifPresent(event -> {
            event.setProcessed(true);
            event.setProcessedAt(LocalDateTime.now());
            if (action != null && !action.isBlank()) {
                event.setProcessingAction(action);
            }
            repository.save(event);
        });
    }

    private String serialize(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar payload de webhook: {}", e.getMessage());
            return payload.toString();
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String determineAction(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        if (Boolean.TRUE.equals(payload.get("solicitudCreada"))) {
            return "solicitudCreada";
        }
        if (Boolean.TRUE.equals(payload.get("prestadorUpsert"))) {
            return "prestadorUpsert";
        }
        if (Boolean.TRUE.equals(payload.get("cotizacionAceptada"))) {
            return "cotizacionAceptada";
        }
        if (Boolean.TRUE.equals(payload.get("cotizacionRecibida"))) {
            return "cotizacionRecibida";
        }
        if (Boolean.TRUE.equals(payload.get("cotizacionRechazada"))) {
            return "cotizacionRechazada";
        }
        if (Boolean.TRUE.equals(payload.get("calificacionProcesada"))) {
            return "calificacionProcesada";
        }
        if (Boolean.TRUE.equals(payload.get("habilidadProcesada"))) {
            return "habilidadProcesada";
        }
        if (Boolean.TRUE.equals(payload.get("habilidadModificada"))) {
            return "habilidadModificada";
        }
        if (Boolean.TRUE.equals(payload.get("rubroProcesado"))) {
            return "rubroProcesado";
        }
        if (Boolean.TRUE.equals(payload.get("rubroModificado"))) {
            return "rubroModificado";
        }
        if (Boolean.TRUE.equals(payload.get("zonaProcesada"))) {
            return "zonaProcesada";
        }
        if (Boolean.TRUE.equals(payload.get("zonaModificada"))) {
            return "zonaModificada";
        }
        if (Boolean.TRUE.equals(payload.get("prestadorDesactivado"))) {
            return "prestadorDesactivado";
        }
        return null;
    }
}
