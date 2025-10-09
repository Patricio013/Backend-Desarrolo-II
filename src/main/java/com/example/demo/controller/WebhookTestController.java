package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.entity.WebhookEvent;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.MatchingSubscriptionService;
import com.example.demo.service.SolicitudService;
import com.example.demo.service.WebhookEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookTestController {

    private final ModuleResponseFactory responseFactory;
    private final MatchingSubscriptionService subscriptionService;
    private final WebhookEventService webhookEventService;
    private final SolicitudService solicitudService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ModuleResponse<Map<String, Object>>> receive(
            @RequestBody(required = false) byte[] bodyBytes,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    ) {
        String rawBody = (bodyBytes != null && bodyBytes.length > 0)
                ? new String(bodyBytes, StandardCharsets.UTF_8)
                : "";
        Map<String, Object> safePayload = Map.of();

        try {
            // Intento parsear JSON si luce como JSON (sin depender del Content-Type)
            if (!rawBody.isBlank() && looksLikeJson(rawBody)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(rawBody, Map.class);
                safePayload = parsed != null ? parsed : Map.of();
            }

            // Log útil de diagnóstico
            log.info("Webhook received. path={}, contentType={}, headers={}, rawBody(first1k)={}",
                    request.getRequestURI(),
                    request.getContentType(),
                    headers,
                    rawBody.length() > 1000 ? rawBody.substring(0, 1000) + "…(truncated)" : rawBody
            );

            // Intento de ACK si hay datos suficientes
            AckOutcome ackOutcome = attemptAckIfPossible(safePayload);

            String topic = firstNonNull(
                    extractString(safePayload, "topic"),
                    extractNestedString(safePayload, "destination", "channel")
            );
            String eventName = firstNonNull(
                    extractString(safePayload, "eventName"),
                    extractNestedString(safePayload, "destination", "eventName")
            );
            String messageId = firstNonNull(
                    extractString(safePayload, "msgId"),
                    extractString(safePayload, "messageId")
            );
            String subscriptionId = extractString(safePayload, "subscriptionId");


            // Intentamos crear una solicitud si viene payload compatible
            Map<String, Object> payloadSection = extractMap(safePayload, "payload");
            boolean solicitudCreada = false;
            Long solicitudIdCreada = null;
            if (payloadSection != null
                    && topic != null
                    && topic.equalsIgnoreCase("search.solicitud.created")
                    && "created".equalsIgnoreCase(firstNonNull(eventName, ""))) {
                try {
                    SolicitudesCreadasDTO solicitudDto = objectMapper.convertValue(payloadSection, SolicitudesCreadasDTO.class);
                    if (solicitudDto.getSolicitudId() != null) {
                        var creadas = solicitudService.crearDesdeEventos(List.of(solicitudDto));
                        for (var creada : creadas) {
                            if (creada != null) {
                                solicitudCreada = true;
                                solicitudIdCreada = creada.getId();
                                break;
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("No se pudo mapear payload de webhook a SolicitudesCreadasDTO: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("Error procesando creación de solicitud desde webhook", e);
                }
            }

            // Guardamos SIEMPRE lo recibido (parsed + raw + headers + resultado del ACK)
            WebhookEvent stored = webhookEventService.storeEvent(
                    topic, eventName, messageId, subscriptionId,
                    Map.of(
                            "parsed", safePayload,
                            "rawBody", rawBody,
                            "headers", headers,
                            "ack", Map.of(
                                    "performed", ackOutcome.performed(),
                                    "success", ackOutcome.success(),
                                    "status", ackOutcome.statusCode(),
                                    "error", ackOutcome.errorMessage()
                            ),
                            "solicitudCreada", solicitudCreada,
                            "solicitudId", solicitudIdCreada
                    )
            );

            // Armamos respuesta para el proveedor
            Map<String, Object> responsePayload = new java.util.HashMap<>(safePayload);
            responsePayload.put("storedEventId", stored.getId());
            responsePayload.put("receivedContentType", request.getContentType());
            responsePayload.put("receivedHeaders", headers);
            responsePayload.put("solicitudCreada", solicitudCreada);
            if (solicitudIdCreada != null) {
                responsePayload.put("solicitudId", solicitudIdCreada);
            }
            if (ackOutcome.performed()) {
                responsePayload.put("ackStatus", ackOutcome.statusCode());
                responsePayload.put("ackSuccess", ackOutcome.success());
                if (!ackOutcome.success() && ackOutcome.errorMessage() != null) {
                    responsePayload.put("ackError", ackOutcome.errorMessage());
                }
            }

            // Devolvemos 200 a propósito para evitar reintentos agresivos en algunos proveedores
            return ResponseEntity.ok(responseFactory.build("webhooks", "testMessage", responsePayload));

        } catch (Exception ex) {
            // Persistimos un evento de error con toda la info disponible (no perdemos nada)
            String stack = getStackTrace(ex);
            WebhookEvent errorStored = webhookEventService.storeEvent(
                    /*topic*/ null,
                    /*eventName*/ "ingestError",
                    /*messageId*/ null,
                    /*subscriptionId*/ null,
                    Map.of(
                            "rawBody", rawBody,
                            "headers", headers,
                            "errorType", ex.getClass().getName(),
                            "errorMessage", ex.getMessage(),
                            "stackTrace", stack
                    )
            );

            log.error("Error procesando webhook. storedErrorEventId={}", errorStored.getId(), ex);

            Map<String, Object> errorResponse = Map.of(
                    "storedErrorEventId", errorStored.getId(),
                    "error", ex.getMessage()
            );
            // Podés cambiar a badRequest() si querés forzar reintento del proveedor.
            return ResponseEntity.ok(responseFactory.build("webhooks", "ingestError", errorResponse));
        }
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

    // ===== Helpers =====

    private boolean looksLikeJson(String s) {
        String t = s.stripLeading();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
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

    @SuppressWarnings("unchecked")
    private String extractNestedString(Map<String, Object> payload, String key, String nestedKey) {
        if (payload == null) return null;
        Object raw = payload.get(key);
        if (raw instanceof Map<?, ?> nested) {
            Object nestedValue = nested.get(nestedKey);
            if (nestedValue instanceof String str) {
                return str.isBlank() ? null : str;
            }
            return nestedValue != null ? nestedValue.toString() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
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
