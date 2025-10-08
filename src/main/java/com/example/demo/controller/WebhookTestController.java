package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.entity.WebhookEvent;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.WebhookEventService;
import com.example.demo.service.MatchingSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookTestController {

    private final ModuleResponseFactory responseFactory;
    private final MatchingSubscriptionService subscriptionService;
    private final WebhookEventService webhookEventService;

    @PostMapping
    public ResponseEntity<ModuleResponse<Map<String, Object>>> receive(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> safePayload = payload != null ? payload : Map.of();
        log.info("Webhook message received: {}", safePayload);

        AckOutcome ackOutcome = attemptAckIfPossible(safePayload);
        String topic = extractString(safePayload, "topic");
        String eventName = extractString(safePayload, "eventName");
        String messageId = extractString(safePayload, "msgId");
        if (messageId == null) {
            messageId = extractString(safePayload, "messageId");
        }
        String subscriptionId = extractString(safePayload, "subscriptionId");

        WebhookEvent stored = webhookEventService.storeEvent(topic, eventName, messageId, subscriptionId, safePayload);
        Map<String, Object> responsePayload = new java.util.HashMap<>(safePayload);
        if (ackOutcome.performed()) {
            responsePayload.put("ackStatus", ackOutcome.statusCode());
            responsePayload.put("ackSuccess", ackOutcome.success());
            if (!ackOutcome.success() && ackOutcome.errorMessage() != null) {
                responsePayload.put("ackError", ackOutcome.errorMessage());
            }
        }
        responsePayload.put("storedEventId", stored.getId());

        return ResponseEntity.ok(responseFactory.build("webhooks", "testMessage", responsePayload));
    }

    @GetMapping
    public ResponseEntity<ModuleResponse<List<WebhookEvent>>> listStoredEvents() {
        List<WebhookEvent> events = webhookEventService.listEvents();
        return ResponseEntity.ok(responseFactory.build("webhooks", "storedEventsListed", events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleResponse<WebhookEvent>> findEvent(@PathVariable Long id) {
        WebhookEvent event = webhookEventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook event not found"));
        return ResponseEntity.ok(responseFactory.build("webhooks", "storedEventDetail", event));
    }

    private AckOutcome attemptAckIfPossible(Map<String, Object> payload) {
        String messageId = extractString(payload, "msgId");
        if (messageId == null) {
            messageId = extractString(payload, "messageId");
        }
        String subscriptionId = extractString(payload, "subscriptionId");

        if (messageId == null || subscriptionId == null) {
            return AckOutcome.notPerformed();
        }

        var result = subscriptionService.acknowledgeMessage(messageId, subscriptionId);
        return result.isSuccess()
                ? AckOutcome.success(result.status().value())
                : AckOutcome.failure(result.status() != null ? result.status().value() : 500, result.errorBody());
    }

    private String extractString(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof String str) {
            return str.isBlank() ? null : str;
        }
        return value.toString();
    }

    private record AckOutcome(boolean performed, boolean success, Integer statusCode, String errorMessage) {
        static AckOutcome notPerformed() {
            return new AckOutcome(false, false, null, null);
        }

        static AckOutcome success(int statusCode) {
            return new AckOutcome(true, true, statusCode, null);
        }

        static AckOutcome failure(int statusCode, String errorMessage) {
            return new AckOutcome(true, false, statusCode, errorMessage);
        }
    }
}
