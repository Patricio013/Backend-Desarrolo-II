package com.example.demo.service;

import com.example.demo.entity.WebhookEvent;
import com.example.demo.repository.WebhookEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
        WebhookEvent entity = WebhookEvent.builder()
                .topic(trimToNull(topic))
                .eventName(trimToNull(eventName))
                .messageId(trimToNull(messageId))
                .subscriptionId(trimToNull(subscriptionId))
                .rawPayload(raw)
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
}
