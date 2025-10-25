package com.example.demo.controller;

import com.example.demo.dto.CotizacionWebhookDTO;
import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.HabilidadAltaWebhookDTO;
import com.example.demo.dto.RubroAltaWebhookDTO;
import com.example.demo.dto.RubroModificacionWebhookDTO;
import com.example.demo.dto.ZonaAltaWebhookDTO;
import com.example.demo.dto.ZonaModificacionWebhookDTO;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.WebhookEvent;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.CotizacionService;
import com.example.demo.service.MatchingSubscriptionService;
import com.example.demo.service.RubroSyncService;
import com.example.demo.service.ZonaSyncService;
import com.example.demo.service.SolicitudService;
import com.example.demo.service.WebhookEventService;
import com.example.demo.service.HabilidadSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
    private final CotizacionService cotizacionService;
    private final RubroSyncService rubroSyncService;
    private final ZonaSyncService zonaSyncService;
    private final HabilidadSyncService habilidadSyncService;
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
            AckOutcome ackOutcome = attemptAckIfPossible(safePayload, headers);

            String topic = firstNonNull(
                    extractString(safePayload, "topic"),
                    firstNonNull(
                            extractString(safePayload, "topico"),
                            firstNonNull(
                                    extractNestedString(safePayload, "destination", "topic"),
                                    extractNestedString(safePayload, "destination", "channel")
                            )
                    )
            );
            String eventName = firstNonNull(
                    extractString(safePayload, "eventName"),
                    firstNonNull(
                        extractString(safePayload, "evento"),
                        extractNestedString(safePayload, "destination", "eventName")
                    )
            );
            String messageId = firstNonNull(
                    extractString(safePayload, "msgId"),
                    extractString(safePayload, "messageId")
            );
            String subscriptionId = extractString(safePayload, "subscriptionId");


            // Intentamos crear una solicitud si viene payload compatible
            Map<String, Object> payloadSection = extractMap(safePayload, "payload");
            if (payloadSection == null) {
                payloadSection = extractMap(safePayload, "cuerpo");
            }
            boolean solicitudCreada = false;
            Long solicitudIdCreada = null;
            boolean solicitudCancelada = false;
            Long solicitudIdCancelada = null;
            List<String> solicitudCancelWarnings = new ArrayList<>();
            boolean habilidadProcesada = false;
            Long habilidadIdProcesada = null;
            List<String> habilidadWarnings = new ArrayList<>();
            boolean habilidadModificada = false;
            Long habilidadIdModificada = null;
            List<String> habilidadModWarnings = new ArrayList<>();
            boolean cotizacionAceptada = false;
            Map<String, Object> cotizacionAceptadaDetails = null;
            List<String> cotizacionAceptadaWarnings = new ArrayList<>();
            boolean cotizacionRecibida = false;
            Map<String, Object> cotizacionRecibidaDetails = null;
            List<String> cotizacionRecibidaWarnings = new ArrayList<>();
            boolean rubroProcesado = false;
            Long rubroIdProcesado = null;
            List<String> rubroWarnings = new ArrayList<>();
            boolean rubroModificado = false;
            Long rubroIdModificado = null;
            List<String> rubroModificacionWarnings = new ArrayList<>();
            boolean zonaProcesada = false;
            Long zonaIdProcesada = null;
            List<String> zonaWarnings = new ArrayList<>();
            boolean zonaModificada = false;
            Long zonaIdModificada = null;
            List<String> zonaModificacionWarnings = new ArrayList<>();
            boolean isSolicitudCreadaEvento = eventName != null
                && (eventName.equalsIgnoreCase("creada") || eventName.equalsIgnoreCase("solicitud.creada"));
            boolean isSolicitudCreadaTopic = topic != null
                && (topic.equalsIgnoreCase("search.solicitud.creada") || topic.equalsIgnoreCase("solicitud"));

            if (payloadSection != null && isSolicitudCreadaEvento && isSolicitudCreadaTopic) {
                // TODO(suscripciones): revisar combinación final de topic/evento cuando el proveedor lo defina
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
                    && eventMatches(eventName, "solicitud", "cancelada", "solicitud.cancelada")) {
                // TODO(suscripciones): ajustar filtros/topic definitivos cuando esté definido el canal real
                Long solicitudId = extractLong(payloadSection, "solicitud_id");
                if (solicitudId == null) {
                    solicitudCancelWarnings.add("solicitud_id ausente en evento de cancelación");
                    log.warn("Evento solicitud.cancelada sin solicitud_id: {}", payloadSection);
                } else {
                    try {
                        solicitudService.cancelarPorId(solicitudId);
                        solicitudCancelada = true;
                        solicitudIdCancelada = solicitudId;
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        solicitudCancelWarnings.add(e.getMessage());
                        log.warn("No se pudo cancelar la solicitud {}: {}", solicitudId, e.getMessage());
                    } catch (Exception e) {
                        solicitudCancelWarnings.add("Error inesperado al cancelar: " + e.getMessage());
                        log.error("Error cancelando solicitud {} desde webhook", solicitudId, e);
                    }
                }
            }

            if (payloadSection != null
                    && eventName != null
                    && eventName.equalsIgnoreCase("cotizacion.aceptada")) {
                // TODO(suscripciones): ajustar canal/tópico definitivo para eventos de cotización
                Long solicitudId = extractLong(payloadSection, "solicitud_id");
                Long prestadorId = extractLong(payloadSection, "prestador_id");
                BigDecimal monto = extractBigDecimal(payloadSection, "monto");
                if (solicitudId == null || prestadorId == null) {
                    cotizacionAceptadaWarnings.add("solicitud_id o prestador_id ausentes en evento de cotizacion.aceptada");
                    log.warn("Evento cotizacion.aceptada incompleto: {}", payloadSection);
                } else {
                    try {
                        var dto = com.example.demo.dto.SolicitudAsignarDTO.builder()
                            .solicitudId(solicitudId)
                            .prestadorId(prestadorId)
                            .monto(monto)
                            .build();
                        var pago = cotizacionService.aceptarYAsignar(dto);
                        cotizacionAceptada = true;
                        cotizacionAceptadaDetails = Map.of(
                            "solicitudId", pago.getSolicitudId(),
                            "prestadorId", pago.getPrestadorId(),
                            "pagoId", pago.getId(),
                            "cotizacionId", pago.getCotizacionId()
                        );
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        cotizacionAceptadaWarnings.add(e.getMessage());
                        log.warn("Error validando cotizacion.aceptada {}: {}", solicitudId, e.getMessage());
                    } catch (Exception e) {
                        cotizacionAceptadaWarnings.add("Error inesperado al aceptar cotización: " + e.getMessage());
                        log.error("Error aceptando cotización {}-{} desde webhook", solicitudId, prestadorId, e);
                    }
                }
            }

            if (payloadSection != null
                    && topicMatches(topic, "pedido", "pedidos", "catalogue.pedidos.cotizacion_enviada", "matching.pedidos.cotizacion_enviada")
                    && eventMatches(eventName, "pedido", "cotizacion_enviada")) {
                try {
                    CotizacionWebhookDTO cotizacionDto = objectMapper.convertValue(payloadSection, CotizacionWebhookDTO.class);
                    if (cotizacionDto.getIdPedido() == null) {
                        cotizacionRecibidaWarnings.add("id_pedido ausente en cotización");
                        log.warn("Cotizacion enviada sin id_pedido: {}", payloadSection);
                    } else if (cotizacionDto.getIdPrestador() == null) {
                        cotizacionRecibidaWarnings.add("id_prestador ausente en cotización");
                        log.warn("Cotizacion enviada sin id_prestador: {}", payloadSection);
                    } else if (cotizacionDto.getTarifa() == null) {
                        cotizacionRecibidaWarnings.add("tarifa ausente en cotización");
                        log.warn("Cotizacion enviada sin tarifa: {}", payloadSection);
                    } else {
                        var submit = CotizacionesSubmit.builder()
                            .solicitudId(cotizacionDto.getIdPedido())
                            .prestadorId(cotizacionDto.getIdPrestador())
                            .monto(BigDecimal.valueOf(cotizacionDto.getTarifa()))
                            .build();
                        cotizacionService.recibirCotizacion(submit);
                        cotizacionRecibida = true;
                        cotizacionRecibidaDetails = Map.of(
                            "cotizacionIdExterna", cotizacionDto.getId(),
                            "solicitudId", cotizacionDto.getIdPedido(),
                            "prestadorId", cotizacionDto.getIdPrestador(),
                            "monto", cotizacionDto.getTarifa()
                        );
                    }
                } catch (IllegalArgumentException e) {
                    cotizacionRecibidaWarnings.add(e.getMessage());
                    log.warn("Payload de cotizacion enviada inválido: {}", e.getMessage());
                } catch (Exception e) {
                    cotizacionRecibidaWarnings.add("Error inesperado al procesar cotización: " + e.getMessage());
                    log.error("Error procesando cotizacion enviada desde webhook", e);
                }
            }

            if (payloadSection != null
                    && topicMatches(topic, "habilidad", "catalogue.habilidad.alta", "matching.habilidad.alta")
                    && eventMatches(eventName, "habilidad", "alta")) {
                // TODO(suscripciones): revisar canal/evento definitivo para alta de habilidades
                try {
                    HabilidadAltaWebhookDTO habilidadAlta = objectMapper.convertValue(payloadSection, HabilidadAltaWebhookDTO.class);
                    if (habilidadAlta.getId() == null) {
                        habilidadWarnings.add("payload.id ausente en alta de habilidad");
                        log.warn("Alta de habilidad sin id: {}", payloadSection);
                    } else if (habilidadAlta.getIdRubro() == null) {
                        habilidadWarnings.add("payload.id_rubro ausente en alta de habilidad");
                        log.warn("Alta de habilidad {} sin id_rubro", habilidadAlta.getId());
                    } else {
                        habilidadSyncService.upsertDesdeDTO(habilidadAlta);
                        habilidadProcesada = true;
                        habilidadIdProcesada = habilidadAlta.getId();
                    }
                } catch (IllegalArgumentException e) {
                    habilidadWarnings.add(e.getMessage());
                    log.warn("Payload de alta de habilidad inválido: {}", e.getMessage());
                } catch (Exception e) {
                    habilidadWarnings.add("Error procesando alta de habilidad: " + e.getMessage());
                    log.error("Error procesando alta de habilidad desde webhook", e);
                }
            }

            if (payloadSection != null
                    && topicMatches(topic, "habilidad", "catalogue.habilidad.modificacion", "matching.habilidad.modificacion")
                    && eventMatches(eventName, "habilidad", "modificacion", "alta_modificacion")) {
                // TODO(suscripciones): revisar canal/evento definitivo para modificación de habilidades
                try {
                    HabilidadAltaWebhookDTO habilidadMod = objectMapper.convertValue(payloadSection, HabilidadAltaWebhookDTO.class);
                    if (habilidadMod.getId() == null) {
                        habilidadModWarnings.add("payload.id ausente en modificacion de habilidad");
                        log.warn("Modificación de habilidad sin id: {}", payloadSection);
                    } else if (habilidadMod.getIdRubro() == null) {
                        habilidadModWarnings.add("payload.id_rubro ausente en modificacion de habilidad");
                        log.warn("Modificación de habilidad {} sin id_rubro", habilidadMod.getId());
                    } else {
                        habilidadSyncService.actualizarDesdeDTO(habilidadMod);
                        habilidadModificada = true;
                        habilidadIdModificada = habilidadMod.getId();
                    }
                } catch (IllegalArgumentException e) {
                    habilidadModWarnings.add(e.getMessage());
                    log.warn("Payload de modificacion de habilidad inválido: {}", e.getMessage());
                } catch (Exception e) {
                    habilidadModWarnings.add("Error procesando modificacion de habilidad: " + e.getMessage());
                    log.error("Error procesando modificacion de habilidad desde webhook", e);
                }
            }

            if (payloadSection != null
                    && topicMatches(topic, "rubro", "catalogue.rubro.alta", "matching.rubro.alta")
                    && eventMatches(eventName, "rubro", "alta")) {
                try {
                    RubroAltaWebhookDTO rubroAlta = objectMapper.convertValue(payloadSection, RubroAltaWebhookDTO.class);
                    rubroSyncService.upsertDesdeDTO(rubroAlta);
                    rubroProcesado = true;
                    rubroIdProcesado = rubroAlta.getId();
                } catch (IllegalArgumentException e) {
                    rubroWarnings.add(e.getMessage());
                    log.warn("Payload de alta de rubro inválido: {}", e.getMessage());
                } catch (Exception e) {
                    rubroWarnings.add("Error procesando alta de rubro: " + e.getMessage());
                    log.error("Error procesando alta de rubro desde webhook", e);
                }
            }

            if (payloadSection != null
                    && topicMatches(topic, "rubro", "catalogue.rubro.modificacion", "matching.rubro.modificacion")
                    && eventMatches(eventName, "rubro", "modificacion")) {
                try {
                    RubroModificacionWebhookDTO rubroModificacion = objectMapper.convertValue(payloadSection, RubroModificacionWebhookDTO.class);
                    rubroSyncService.actualizarDesdeDTO(rubroModificacion);
                    rubroModificado = true;
                    rubroIdModificado = rubroModificacion.getId();
                } catch (IllegalArgumentException e) {
                    rubroModificacionWarnings.add(e.getMessage());
                    log.warn("Payload de modificación de rubro inválido: {}", e.getMessage());
                } catch (Exception e) {
                    rubroModificacionWarnings.add("Error procesando modificación de rubro: " + e.getMessage());
                    log.error("Error procesando modificación de rubro desde webhook", e);
                }
            }

            if (payloadSection != null
                    && topicMatches(topic, "zona", "catalogue.zona.alta", "matching.zona.alta")
                    && eventMatches(eventName, "zona", "alta")) {
                try {
                    ZonaAltaWebhookDTO zonaAlta = objectMapper.convertValue(payloadSection, ZonaAltaWebhookDTO.class);
                    zonaSyncService.upsertDesdeDTO(zonaAlta);
                    zonaProcesada = true;
                    zonaIdProcesada = zonaAlta.getId();
                } catch (IllegalArgumentException e) {
                    zonaWarnings.add(e.getMessage());
                    log.warn("Payload de alta de zona inválido: {}", e.getMessage());
                } catch (Exception e) {
                    zonaWarnings.add("Error procesando alta de zona: " + e.getMessage());
                    log.error("Error procesando alta de zona desde webhook", e);
                }
            }

            if (payloadSection != null
                    && topicMatches(topic, "zona", "catalogue.zona.modificacion", "matching.zona.modificacion")
                    && eventMatches(eventName, "zona", "modificacion")) {
                try {
                    ZonaModificacionWebhookDTO zonaModificacion = objectMapper.convertValue(payloadSection, ZonaModificacionWebhookDTO.class);
                    zonaSyncService.actualizarDesdeDTO(zonaModificacion);
                    zonaModificada = true;
                    zonaIdModificada = zonaModificacion.getId();
                } catch (IllegalArgumentException e) {
                    zonaModificacionWarnings.add(e.getMessage());
                    log.warn("Payload de modificación de zona inválido: {}", e.getMessage());
                } catch (Exception e) {
                    zonaModificacionWarnings.add("Error procesando modificación de zona: " + e.getMessage());
                    log.error("Error procesando modificación de zona desde webhook", e);
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
            storedPayload.put("solicitudCancelada", solicitudCancelada);
            if (solicitudIdCancelada != null) {
                storedPayload.put("solicitudIdCancelada", solicitudIdCancelada);
            }
            if (!solicitudCancelWarnings.isEmpty()) {
                storedPayload.put("solicitudCancelWarnings", solicitudCancelWarnings);
            }
            storedPayload.put("cotizacionAceptada", cotizacionAceptada);
            if (cotizacionAceptadaDetails != null) {
                storedPayload.put("cotizacionAceptadaDetails", cotizacionAceptadaDetails);
            }
            if (!cotizacionAceptadaWarnings.isEmpty()) {
                storedPayload.put("cotizacionAceptadaWarnings", cotizacionAceptadaWarnings);
            }
            storedPayload.put("cotizacionRecibida", cotizacionRecibida);
            if (cotizacionRecibidaDetails != null) {
                storedPayload.put("cotizacionRecibidaDetails", cotizacionRecibidaDetails);
            }
            if (!cotizacionRecibidaWarnings.isEmpty()) {
                storedPayload.put("cotizacionRecibidaWarnings", cotizacionRecibidaWarnings);
            }
            storedPayload.put("habilidadProcesada", habilidadProcesada);
            if (habilidadIdProcesada != null) {
                storedPayload.put("habilidadId", habilidadIdProcesada);
            }
            if (!habilidadWarnings.isEmpty()) {
                storedPayload.put("habilidadWarnings", habilidadWarnings);
            }
            storedPayload.put("habilidadModificada", habilidadModificada);
            if (habilidadIdModificada != null) {
                storedPayload.put("habilidadIdModificada", habilidadIdModificada);
            }
            if (!habilidadModWarnings.isEmpty()) {
                storedPayload.put("habilidadModWarnings", habilidadModWarnings);
            }
            storedPayload.put("rubroProcesado", rubroProcesado);
            if (rubroIdProcesado != null) {
                storedPayload.put("rubroId", rubroIdProcesado);
            }
            if (!rubroWarnings.isEmpty()) {
                storedPayload.put("rubroWarnings", rubroWarnings);
            }
            storedPayload.put("rubroModificado", rubroModificado);
            if (rubroIdModificado != null) {
                storedPayload.put("rubroIdModificado", rubroIdModificado);
            }
            if (!rubroModificacionWarnings.isEmpty()) {
                storedPayload.put("rubroModificacionWarnings", rubroModificacionWarnings);
            }
            storedPayload.put("zonaProcesada", zonaProcesada);
            if (zonaIdProcesada != null) {
                storedPayload.put("zonaId", zonaIdProcesada);
            }
            if (!zonaWarnings.isEmpty()) {
                storedPayload.put("zonaWarnings", zonaWarnings);
            }
            storedPayload.put("zonaModificada", zonaModificada);
            if (zonaIdModificada != null) {
                storedPayload.put("zonaIdModificada", zonaIdModificada);
            }
            if (!zonaModificacionWarnings.isEmpty()) {
                storedPayload.put("zonaModificacionWarnings", zonaModificacionWarnings);
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
            responsePayload.put("solicitudCancelada", solicitudCancelada);
            if (solicitudIdCancelada != null) {
                responsePayload.put("solicitudIdCancelada", solicitudIdCancelada);
            }
            if (!solicitudCancelWarnings.isEmpty()) {
                responsePayload.put("solicitudCancelWarnings", solicitudCancelWarnings);
            }
            responsePayload.put("cotizacionAceptada", cotizacionAceptada);
            if (cotizacionAceptadaDetails != null) {
                responsePayload.put("cotizacionAceptadaDetails", cotizacionAceptadaDetails);
            }
            if (!cotizacionAceptadaWarnings.isEmpty()) {
                responsePayload.put("cotizacionAceptadaWarnings", cotizacionAceptadaWarnings);
            }
            responsePayload.put("cotizacionRecibida", cotizacionRecibida);
            if (cotizacionRecibidaDetails != null) {
                responsePayload.put("cotizacionRecibidaDetails", cotizacionRecibidaDetails);
            }
            if (!cotizacionRecibidaWarnings.isEmpty()) {
                responsePayload.put("cotizacionRecibidaWarnings", cotizacionRecibidaWarnings);
            }
            responsePayload.put("habilidadProcesada", habilidadProcesada);
            if (habilidadIdProcesada != null) {
                responsePayload.put("habilidadId", habilidadIdProcesada);
            }
            if (!habilidadWarnings.isEmpty()) {
                responsePayload.put("habilidadWarnings", habilidadWarnings);
            }
            responsePayload.put("habilidadModificada", habilidadModificada);
            if (habilidadIdModificada != null) {
                responsePayload.put("habilidadIdModificada", habilidadIdModificada);
            }
            if (!habilidadModWarnings.isEmpty()) {
                responsePayload.put("habilidadModWarnings", habilidadModWarnings);
            }
            responsePayload.put("rubroProcesado", rubroProcesado);
            if (rubroIdProcesado != null) {
                responsePayload.put("rubroId", rubroIdProcesado);
            }
            if (!rubroWarnings.isEmpty()) {
                responsePayload.put("rubroWarnings", rubroWarnings);
            }
            responsePayload.put("rubroModificado", rubroModificado);
            if (rubroIdModificado != null) {
                responsePayload.put("rubroIdModificado", rubroIdModificado);
            }
            if (!rubroModificacionWarnings.isEmpty()) {
                responsePayload.put("rubroModificacionWarnings", rubroModificacionWarnings);
            }
            responsePayload.put("zonaProcesada", zonaProcesada);
            if (zonaIdProcesada != null) {
                responsePayload.put("zonaId", zonaIdProcesada);
            }
            if (!zonaWarnings.isEmpty()) {
                responsePayload.put("zonaWarnings", zonaWarnings);
            }
            responsePayload.put("zonaModificada", zonaModificada);
            if (zonaIdModificada != null) {
                responsePayload.put("zonaIdModificada", zonaIdModificada);
            }
            if (!zonaModificacionWarnings.isEmpty()) {
                responsePayload.put("zonaModificacionWarnings", zonaModificacionWarnings);
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

    private AckOutcome attemptAckIfPossible(Map<String, Object> payload, Map<String, String> headers) {
        String messageId = extractString(payload, "msgId");
        if (messageId == null) {
            messageId = extractString(payload, "messageId");
        }
        String subscriptionId = extractString(payload, "subscriptionId");

        if (messageId == null && headers != null) {
            messageId = extractHeader(headers, "x-message-id", "x_msg_id", "message-id");
        }
        if (subscriptionId == null && headers != null) {
            subscriptionId = extractHeader(headers, "x-subscription-id", "subscription-id");
        }

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

    private Long extractLong(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            String text = value.toString();
            if (text == null || text.isBlank()) {
                return null;
            }
            return Long.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal extractBigDecimal(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            String text = value.toString();
            if (text == null || text.isBlank()) {
                return null;
            }
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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

    private String extractHeader(Map<String, String> headers, String... candidateNames) {
        if (headers == null || headers.isEmpty()) return null;
        for (String candidate : candidateNames) {
            if (candidate == null) continue;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                if (headerName != null && headerName.equalsIgnoreCase(candidate)) {
                    String value = entry.getValue();
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                }
            }
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

    private boolean topicMatches(String topic, String... candidates) {
        if (topic == null || topic.isBlank()) {
            return false;
        }
        String normalized = normalizeTopicDomain(topic);
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (topic.equalsIgnoreCase(candidate)) {
                return true;
            }
            if (!candidate.contains(".") && normalized != null && normalized.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTopicDomain(String topic) {
        if (topic == null) {
            return null;
        }
        String trimmed = topic.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\\.");
        if (parts.length == 0) {
            return null;
        }
        if (parts.length == 1) {
            return parts[0];
        }
        if (parts.length >= 3) {
            return parts[parts.length - 2];
        }
        return parts[parts.length - 1];
    }

    private boolean eventMatches(String eventName, String domain, String... expected) {
        if (expected == null || expected.length == 0) {
            return true;
        }
        String normalized = normalizeEventName(eventName);
        for (String candidate : expected) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (eventName != null && eventName.equalsIgnoreCase(candidate)) {
                return true;
            }
            if (normalized != null && normalized.equalsIgnoreCase(candidate)) {
                return true;
            }
            if (eventName != null) {
                if (domain != null && eventName.equalsIgnoreCase(candidate + "_" + domain)) {
                    return true;
                }
                if (domain != null && eventName.equalsIgnoreCase(domain + "." + candidate)) {
                    return true;
                }
                if (domain != null && eventName.equalsIgnoreCase(domain + "_" + candidate)) {
                    return true;
                }
                if (eventName.contains("_")) {
                    for (String part : eventName.split("_")) {
                        if (part.equalsIgnoreCase(candidate)) {
                            return true;
                        }
                    }
                }
                if (eventName.contains(".")) {
                    for (String part : eventName.split("\\.")) {
                        if (part.equalsIgnoreCase(candidate)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private String normalizeEventName(String eventName) {
        if (eventName == null) {
            return null;
        }
        String trimmed = eventName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < trimmed.length() - 1) {
            trimmed = trimmed.substring(lastSlash + 1);
        }
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < trimmed.length() - 1) {
            trimmed = trimmed.substring(lastDot + 1);
        }
        int firstUnderscore = trimmed.indexOf('_');
        if (firstUnderscore >= 0) {
            trimmed = trimmed.substring(0, firstUnderscore);
        }
        return trimmed;
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
