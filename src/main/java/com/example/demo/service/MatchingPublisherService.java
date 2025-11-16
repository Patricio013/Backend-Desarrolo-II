package com.example.demo.service;

import com.example.demo.config.MatchingIntegrationProperties;
import com.example.demo.controller.SolicitudController;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.entity.Cotizacion;
import com.example.demo.entity.MatchingPublishMessage;
import com.example.demo.entity.MatchingPublishMessage.MessageType;
import com.example.demo.entity.MatchingPublishMessage.PublishStatus;
import com.example.demo.entity.Solicitud;
import com.example.demo.repository.MatchingPublishMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingPublisherService {

    private static final List<PublishStatus> RETRIABLE_STATUSES = List.of(PublishStatus.PENDING, PublishStatus.FAILED);
    private static final int DEFAULT_RETRY_LIMIT = 50;
    private static final int MAX_RETRY_LIMIT = 200;

    private final RestClient matchingRestClient;
    private final MatchingIntegrationProperties properties;
    private final MatchingPublishMessageRepository publishMessageRepository;
    private final ObjectMapper objectMapper;

    public PublishResult publishSolicitudesTop3(List<SolicitudTop3Resultado> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            log.info("No hay solicitudes procesadas para publicar top3");
            return PublishResult.skipped("No solicitudes to publish");
        }

        PublishMessage message = buildMessage(resultados);
        MatchingPublishMessage stored = persistMessage(MessageType.TOP3, message);

        if (!properties.publishEnabled()) {
            log.info("Matching publish disabled; mensaje {} almacenado como pendiente", stored.getId());
            return PublishResult.skipped("Publishing disabled by configuration");
        }

        return sendAndUpdate(stored, message);
    }

    private PublishMessage buildMessage(List<SolicitudTop3Resultado> resultados) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("generatedAt", Instant.now().toString());
        payload.put("solicitudes", resultados.stream()
                .filter(Objects::nonNull)
                .map(this::mapSolicitud)
                .toList());

        return new PublishMessage(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                new Destination(properties.publishTop3Channel(), properties.publishTop3EventName()),
                payload
        );
    }

    public PublishResult publishCotizaciones(Solicitud solicitud,
                                             List<Cotizacion> cotizaciones,
                                             int objetivoCotizaciones) {
        if (solicitud == null || cotizaciones == null || cotizaciones.isEmpty()) {
            log.debug("Sin cotizaciones para publicar");
            return PublishResult.skipped("No cotizaciones to publish");
        }

        PublishMessage message = buildCotizacionesMessage(solicitud, cotizaciones, objetivoCotizaciones);
        MatchingPublishMessage stored = persistMessage(MessageType.COTIZACIONES, message);

        if (!properties.publishEnabled()) {
            log.debug("Matching publish disabled; mensaje {} almacenado como pendiente", stored.getId());
            return PublishResult.skipped("Publishing disabled by configuration");
        }

        return sendAndUpdate(stored, message);
    }

    private Map<String, Object> mapSolicitud(SolicitudTop3Resultado resultado) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("solicitudId", resultado.getSolicitudId());
        out.put("descripcion", resultado.getDescripcion());
        out.put("usuarioId", resultado.getUsuarioId());
        out.put("fecha", resultado.getFecha());
        out.put("horario", resultado.getHorario());
        out.put("estado", resultado.getEstado());
        out.put("fueCotizada", resultado.getFueCotizada());
        out.put("esCritica", resultado.getEsCritica());
        Map<String, Object> direccion = buildDireccion(resultado);
        if (direccion != null) {
            out.put("direccion", direccion);
        }
        List<InvitacionCotizacionDTO> top3 = resultado.getTop3();
        out.put("top3", top3 == null ? List.of() : top3.stream()
                .filter(Objects::nonNull)
                .map(this::mapInvitacion)
                .toList());
        return out;
    }

    private Map<String, Object> buildDireccion(SolicitudController.SolicitudTop3Resultado resultado) {
        boolean hasData =
                resultado.getDireccionProvincia() != null ||
                resultado.getDireccionCiudad() != null ||
                resultado.getDireccionCalle() != null ||
                resultado.getDireccionNumero() != null ||
                resultado.getDireccionPiso() != null ||
                resultado.getDireccionDepto() != null ||
                resultado.getDireccionCodigoPostal() != null;
        if (!hasData) {
            return null;
        }
        Map<String, Object> direccion = new LinkedHashMap<>();
        direccion.put("provincia", resultado.getDireccionProvincia());
        direccion.put("ciudad", resultado.getDireccionCiudad());
        direccion.put("calle", resultado.getDireccionCalle());
        direccion.put("numero", resultado.getDireccionNumero());
        direccion.put("piso", resultado.getDireccionPiso());
        direccion.put("depto", resultado.getDireccionDepto());
        direccion.put("codigoPostal", resultado.getDireccionCodigoPostal());
        return direccion;
    }

    private Map<String, Object> mapInvitacion(InvitacionCotizacionDTO invitacion) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("prestadorId", invitacion.getPrestadorId());
        out.put("prestadorNombre", invitacion.getPrestadorNombre());
        out.put("mensaje", invitacion.getMensaje());
        out.put("enviado", invitacion.isEnviado());
        out.put("timestamp", invitacion.getTimestamp() != null ? invitacion.getTimestamp().toString() : null);
        out.put("habilidadId", invitacion.getHabilidadId());
        out.put("rubroId", invitacion.getRubroId());
        out.put("cotizacionId", invitacion.getCotizacionId());
        out.put("solicitudId", invitacion.getSolicitudId());
        return out;
    }

    private PublishMessage buildCotizacionesMessage(Solicitud solicitud,
                                                    List<Cotizacion> cotizaciones,
                                                    int objetivoCotizaciones) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("generatedAt", Instant.now().toString());
        payload.put("solicitud", mapCotizacionesSolicitud(solicitud, cotizaciones, objetivoCotizaciones));

        return new PublishMessage(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                new Destination(properties.publishCotizacionesChannel(), properties.publishCotizacionesEventName()),
                payload
        );
    }

    private Map<String, Object> mapCotizacionesSolicitud(Solicitud solicitud,
                                                         List<Cotizacion> cotizaciones,
                                                         int objetivoCotizaciones) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("solicitudId", solicitud.getId());
        out.put("estado", solicitud.getEstado());
        out.put("esCritica", solicitud.isEsCritica());
        out.put("prestadorAsignadoId", solicitud.getPrestadorAsignadoId());
        out.put("objetivoCotizaciones", objetivoCotizaciones);
        out.put("totalCotizaciones", cotizaciones.size());
        out.put("round", solicitud.getCotizacionRound());
        out.put("cotizaciones", cotizaciones.stream()
                .filter(Objects::nonNull)
                .map(this::mapCotizacion)
                .toList());
        return out;
    }

    private Map<String, Object> mapCotizacion(Cotizacion cotizacion) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cotizacionId", cotizacion.getId());
        out.put("prestadorId", cotizacion.getPrestador().getId());
        out.put("monto", BigDecimal.valueOf(cotizacion.getValor()));
        out.put("round", cotizacion.getRound());
        return out;
    }

    private record PublishMessage(
            String messageId,
            String timestamp,
            Destination destination,
            Map<String, Object> payload
    ) {
    }

    private record Destination(String topic, String eventName) {
    }

    public record PublishResult(String messageId, boolean success, HttpStatus status, String errorMessage) {
        public static PublishResult success(String messageId, HttpStatus status) {
            return new PublishResult(messageId, true, status, null);
        }

        public static PublishResult failure(String messageId, HttpStatus status, String errorMessage) {
            return new PublishResult(messageId, false, status, errorMessage);
        }

        public static PublishResult skipped(String reason) {
            return new PublishResult(null, false, HttpStatus.ACCEPTED, reason);
        }
    }

    public record RetryResult(int attempted, int sent, int failed, long remainingPending,
                              List<Long> processedMessageIds) {}

    // ===== Pagos: Solicitud de Pago Emitida =====
    public PublishResult publishSolicitudPagoEmitida(
            String idCorrelacion,
            Long idUsuario,
            Long idPrestador,
            Long idSolicitud,
            BigDecimal montoSubtotal,
            BigDecimal impuestos,
            BigDecimal comisiones,
            String moneda,
            String metodoPreferido,
            String descripcion,
            String descripcionSolicitud
    ) {
        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("idCorrelacion", idCorrelacion);
        pago.put("idUsuario", idUsuario);
        pago.put("idPrestador", idPrestador);
        pago.put("idSolicitud", idSolicitud);
        pago.put("montoSubtotal", montoSubtotal);
        pago.put("impuestos", impuestos);
        pago.put("comisiones", comisiones);
        pago.put("moneda", moneda);
        pago.put("metodoPreferido", metodoPreferido);
        if (descripcion != null && !descripcion.isBlank()) {
            pago.put("descripcion", descripcion);
        }
        if (descripcionSolicitud != null && !descripcionSolicitud.isBlank()) {
            pago.put("descripcionSolicitud", descripcionSolicitud);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("generatedAt", Instant.now().toString());
        payload.put("pago", pago);

        PublishMessage message = new PublishMessage(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                new Destination(properties.publishPagoChannel(), properties.publishPagoEventName()),
                payload
        );
        MatchingPublishMessage stored = persistMessage(MessageType.SOLICITUD_PAGO, message);

        if (!properties.publishEnabled()) {
            log.debug("Matching publish disabled; mensaje {} almacenado como pendiente", stored.getId());
            return PublishResult.skipped("Publishing disabled by configuration");
        }

        return sendAndUpdate(stored, message);
    }

    public RetryResult retryPendingMessages(List<Long> messageIds, int max) {
        List<MatchingPublishMessage> targets = findMessagesForRetry(messageIds, max);
        if (targets.isEmpty()) {
            long remaining = publishMessageRepository.countByStatusIn(RETRIABLE_STATUSES);
            return new RetryResult(0, 0, 0, remaining, List.of());
        }

        if (!properties.publishEnabled()) {
            log.warn("Matching publish disabled por configuración, reintentando manualmente {} mensajes", targets.size());
        }

        List<Long> processed = new ArrayList<>();
        int sent = 0;
        int failed = 0;
        for (MatchingPublishMessage entry : targets) {
            PublishResult result = sendStoredMessage(entry);
            processed.add(entry.getId());
            if (result.success()) {
                sent++;
            } else {
                failed++;
            }
        }
        long remaining = publishMessageRepository.countByStatusIn(RETRIABLE_STATUSES);
        return new RetryResult(processed.size(), sent, failed, remaining, processed);
    }

    private List<MatchingPublishMessage> findMessagesForRetry(List<Long> messageIds, int max) {
        if (messageIds != null && !messageIds.isEmpty()) {
            return publishMessageRepository.findByIdIn(messageIds).stream()
                    .filter(msg -> msg.getStatus() != PublishStatus.SENT)
                    .toList();
        }
        int limit = normalizeRetryLimit(max);
        return publishMessageRepository.findByStatusIn(RETRIABLE_STATUSES, PageRequest.of(0, limit));
    }

    private int normalizeRetryLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_RETRY_LIMIT;
        }
        return Math.min(requested, MAX_RETRY_LIMIT);
    }

    public List<MatchingPublishMessage> listMessages(MatchingPublishMessage.PublishStatus status, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 200));
        if (status == null) {
            return publishMessageRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, pageSize))
                    .getContent();
        }
        return publishMessageRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, pageSize))
                .getContent();
    }

    private PublishResult sendStoredMessage(MatchingPublishMessage stored) {
        PublishMessage message = rebuildMessage(stored);
        return sendAndUpdate(stored, message);
    }

    private PublishResult sendAndUpdate(MatchingPublishMessage stored, PublishMessage message) {
        try {
            ResponseEntity<Void> response = matchingRestClient.post()
                    .uri(properties.publishPath())
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            stored.markAttempt(status.value(), null, PublishStatus.SENT);
            publishMessageRepository.save(stored);
            log.info("Publicado messageId={} type={} status={} topic={} event={}",
                    message.messageId(), stored.getType(), status,
                    message.destination().topic(), message.destination().eventName());
            return PublishResult.success(message.messageId(), status);
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            stored.markAttempt(status.value(), e.getResponseBodyAsString(), PublishStatus.FAILED);
            publishMessageRepository.save(stored);
            log.error("Error publicando messageId={} type={} status={} body={}",
                    message.messageId(), stored.getType(), status, e.getResponseBodyAsString());
            return PublishResult.failure(message.messageId(), status, e.getResponseBodyAsString());
        } catch (RestClientException e) {
            stored.markAttempt(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), PublishStatus.FAILED);
            publishMessageRepository.save(stored);
            log.error("Error publicando messageId={} type={}", message.messageId(), stored.getType(), e);
            return PublishResult.failure(message.messageId(), HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private PublishMessage rebuildMessage(MatchingPublishMessage stored) {
        Map<String, Object> payload = deserializePayload(stored.getPayloadJson());
        return new PublishMessage(
                stored.getMessageId(),
                stored.getMessageTimestamp(),
                new Destination(stored.getDestinationTopic(), stored.getDestinationEvent()),
                payload
        );
    }

    private MatchingPublishMessage persistMessage(MessageType type, PublishMessage message) {
        MatchingPublishMessage entity = MatchingPublishMessage.builder()
                .messageId(message.messageId())
                .type(type)
                .destinationTopic(message.destination().topic())
                .destinationEvent(message.destination().eventName())
                .messageTimestamp(message.timestamp())
                .payloadJson(serializePayload(message.payload()))
                .status(PublishStatus.PENDING)
                .build();
        return publishMessageRepository.save(entity);
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload para Matching", e);
        }
    }

    private Map<String, Object> deserializePayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo deserializar el payload almacenado de Matching", e);
        }
    }
}
