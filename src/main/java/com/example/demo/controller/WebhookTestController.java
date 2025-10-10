package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.PrestadorAltaWebhookDTO;
import com.example.demo.dto.PrestadorDTO;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.WebhookEvent;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.MatchingSubscriptionService;
import com.example.demo.service.PrestadorSyncService;
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
import java.util.ArrayList;
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
    private final PrestadorSyncService prestadorSyncService;
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
            boolean prestadorProcesado = false;
            Long prestadorIdProcesado = null;
            List<String> prestadorWarnings = new ArrayList<>();
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

            if (payloadSection != null
                    && topic != null
                    && topic.equalsIgnoreCase("catalogue.prestador.alta")
                    && "alta_prestador".equalsIgnoreCase(firstNonNull(eventName, ""))) {
                try {
                    PrestadorAltaWebhookDTO prestadorAlta = objectMapper.convertValue(payloadSection, PrestadorAltaWebhookDTO.class);
                    PrestadorDTO prestadorDTO = buildPrestadorDtoFromAlta(prestadorAlta, prestadorWarnings);
                    prestadorSyncService.upsertDesdeDTO(prestadorDTO);
                    prestadorProcesado = true;
                    prestadorIdProcesado = prestadorDTO.getId();
                } catch (IllegalArgumentException e) {
                    prestadorWarnings.add(e.getMessage());
                    log.warn("Payload de alta de prestador inválido: {}", e.getMessage());
                } catch (Exception e) {
                    prestadorWarnings.add("Error procesando alta de prestador: " + e.getMessage());
                    log.error("Error procesando alta de prestador desde webhook", e);
                }
            }

            // Guardamos SIEMPRE lo recibido (parsed + raw + headers + resultado del ACK)
            Map<String, Object> ackMetadata = new java.util.HashMap<>();
            ackMetadata.put("performed", ackOutcome.performed());
            ackMetadata.put("success", ackOutcome.success());
            if (ackOutcome.statusCode() != null) {
                ackMetadata.put("status", ackOutcome.statusCode());
            }
            if (ackOutcome.errorMessage() != null) {
                ackMetadata.put("error", ackOutcome.errorMessage());
            }

            Map<String, Object> storedPayload = new java.util.HashMap<>();
            storedPayload.put("parsed", safePayload);
            storedPayload.put("rawBody", rawBody);
            storedPayload.put("headers", headers);
            storedPayload.put("ack", ackMetadata);
            storedPayload.put("solicitudCreada", solicitudCreada);
            if (solicitudIdCreada != null) {
                storedPayload.put("solicitudId", solicitudIdCreada);
            }
            storedPayload.put("prestadorProcesado", prestadorProcesado);
            if (prestadorIdProcesado != null) {
                storedPayload.put("prestadorId", prestadorIdProcesado);
            }
            if (!prestadorWarnings.isEmpty()) {
                storedPayload.put("prestadorWarnings", prestadorWarnings);
            }

            WebhookEvent stored = webhookEventService.storeEvent(
                    topic, eventName, messageId, subscriptionId, storedPayload
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
            responsePayload.put("prestadorProcesado", prestadorProcesado);
            if (prestadorIdProcesado != null) {
                responsePayload.put("prestadorId", prestadorIdProcesado);
            }
            if (!prestadorWarnings.isEmpty()) {
                responsePayload.put("prestadorWarnings", prestadorWarnings);
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
                    buildErrorPayload(rawBody, headers, ex, stack)
            );

            log.error("Error procesando webhook. storedErrorEventId={}", errorStored.getId(), ex);

            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("storedErrorEventId", errorStored.getId());
            if (ex.getMessage() != null) {
                errorResponse.put("error", ex.getMessage());
            }
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

    private Map<String, Object> buildErrorPayload(String rawBody,
                                                  Map<String, String> headers,
                                                  Exception ex,
                                                  String stack) {
        Map<String, Object> errorPayload = new java.util.HashMap<>();
        errorPayload.put("rawBody", rawBody);
        errorPayload.put("headers", headers);
        errorPayload.put("errorType", ex.getClass().getName());
        if (ex.getMessage() != null) {
            errorPayload.put("errorMessage", ex.getMessage());
        }
        errorPayload.put("stackTrace", stack);
        return errorPayload;
    }

    private String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private PrestadorDTO buildPrestadorDtoFromAlta(PrestadorAltaWebhookDTO evento, List<String> warnings) {
        if (evento == null) {
            throw new IllegalArgumentException("payload vacío para alta de prestador");
        }
        if (evento.getId() == null) {
            throw new IllegalArgumentException("payload.id requerido para alta de prestador");
        }

        String nombre = sanitizeOrFallback(evento.getNombre(), "nombre", warnings, "Sin nombre");
        String apellido = sanitizeOrFallback(evento.getApellido(), "apellido", warnings, "Sin apellido");
        String email = sanitizeOrFallback(
                evento.getEmail(),
                "email",
                warnings,
                "sin-email-" + evento.getId() + "@desconocido.local"
        );
        String telefono = sanitizeOrFallback(evento.getTelefono(), "telefono", warnings, "-");
        String direccion = sanitizeOrFallback(evento.getDireccion(), "direccion", warnings, "Sin direccion");
        String estado = (evento.getActivo() != null && evento.getActivo() == 1) ? "ACTIVO" : "INACTIVO";
        if (evento.getActivo() == null) {
            warnings.add("payload.activo ausente, se asumió estado INACTIVO");
        }
        warnings.add("payload.precioHora ausente, se utilizó 0.0");

        return PrestadorDTO.builder()
                .id(evento.getId())
                .nombre(nombre)
                .apellido(apellido)
                .email(email)
                .telefono(telefono)
                .direccion(direccion)
                .estado(estado)
                .precioHora(0.0)
                .zonaId(null)
                .habilidades(List.of())
                .calificacion(List.of())
                .trabajosFinalizados(0)
                .build();
    }

    private String sanitizeOrFallback(String value, String fieldName, List<String> warnings, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        warnings.add("payload." + fieldName + " ausente, se usó '" + fallback + "'");
        return fallback;
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
