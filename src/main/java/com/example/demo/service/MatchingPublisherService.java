package com.example.demo.service;

import com.example.demo.config.MatchingIntegrationProperties;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.entity.Cotizacion;
import com.example.demo.entity.Solicitud;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingPublisherService {

    private final RestClient matchingRestClient;
    private final MatchingIntegrationProperties properties;

    public PublishResult publishSolicitudesTop3(List<SolicitudTop3Resultado> resultados) {
        if (!properties.publishEnabled()) {
            log.info("Matching publish disabled; skipping top3 publication");
            return PublishResult.skipped("Publishing disabled by configuration");
        }
        if (resultados == null || resultados.isEmpty()) {
            log.info("No hay solicitudes procesadas para publicar top3");
            return PublishResult.skipped("No solicitudes to publish");
        }

        PublishMessage message = buildMessage(resultados);
        try {
            ResponseEntity<Void> response = matchingRestClient.post()
                    .uri(properties.publishPath())
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            log.info("Publicado top3 en Matching messageId={} status={} channel={} event={}",
                    message.messageId(), status, message.destination().channel(), message.destination().eventName());
            return PublishResult.success(message.messageId(), status);
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            log.error("Error publicando top3 messageId={} status={} body={}",
                    message.messageId(), status, e.getResponseBodyAsString());
            return PublishResult.failure(message.messageId(), status, e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error publicando top3 messageId={}", message.messageId(), e);
            return PublishResult.failure(message.messageId(), HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
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
                properties.publishSource(),
                new Destination(properties.publishTop3Channel(), properties.publishTop3EventName()),
                payload
        );
    }

    public PublishResult publishCotizaciones(Solicitud solicitud,
                                             List<Cotizacion> cotizaciones,
                                             int objetivoCotizaciones) {
        if (!properties.publishEnabled()) {
            log.debug("Matching publish disabled; skipping cotizaciones publication");
            return PublishResult.skipped("Publishing disabled by configuration");
        }
        if (solicitud == null || cotizaciones == null || cotizaciones.isEmpty()) {
            log.debug("Sin cotizaciones para publicar");
            return PublishResult.skipped("No cotizaciones to publish");
        }

        PublishMessage message = buildCotizacionesMessage(solicitud, cotizaciones, objetivoCotizaciones);
        try {
            ResponseEntity<Void> response = matchingRestClient.post()
                    .uri(properties.publishPath())
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            log.info("Publicado resumen de cotizaciones messageId={} status={} channel={} event={}",
                    message.messageId(), status, message.destination().channel(), message.destination().eventName());
            return PublishResult.success(message.messageId(), status);
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            log.error("Error publicando cotizaciones messageId={} status={} body={}",
                    message.messageId(), status, e.getResponseBodyAsString());
            return PublishResult.failure(message.messageId(), status, e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error publicando cotizaciones messageId={}", message.messageId(), e);
            return PublishResult.failure(message.messageId(), HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Map<String, Object> mapSolicitud(SolicitudTop3Resultado resultado) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("solicitudId", resultado.getSolicitudId());
        out.put("descripcion", resultado.getDescripcion());
        out.put("estado", resultado.getEstado());
        out.put("fueCotizada", resultado.getFueCotizada());
        out.put("esCritica", resultado.getEsCritica());
        List<InvitacionCotizacionDTO> top3 = resultado.getTop3();
        out.put("top3", top3 == null ? List.of() : top3.stream()
                .filter(Objects::nonNull)
                .map(this::mapInvitacion)
                .toList());
        return out;
    }

    private Map<String, Object> mapInvitacion(InvitacionCotizacionDTO invitacion) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("prestadorId", invitacion.getPrestadorId());
        out.put("prestadorNombre", invitacion.getPrestadorNombre());
        out.put("mensaje", invitacion.getMensaje());
        out.put("enviado", invitacion.isEnviado());
        out.put("timestamp", invitacion.getTimestamp());
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
                properties.publishSource(),
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
            String source,
            Destination destination,
            Map<String, Object> payload
    ) {
    }

    private record Destination(String channel, String eventName) {
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
            String metodoPreferido
    ) {
        if (!properties.publishEnabled()) {
            log.debug("Matching publish disabled; skipping pago emitida publication");
            return PublishResult.skipped("Publishing disabled by configuration");
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idCorrelacion", idCorrelacion);
        cuerpo.put("idUsuario", idUsuario);
        cuerpo.put("idPrestador", idPrestador);
        cuerpo.put("idSolicitud", idSolicitud);
        cuerpo.put("montoSubtotal", montoSubtotal);
        cuerpo.put("impuestos", impuestos);
        cuerpo.put("comisiones", comisiones);
        cuerpo.put("moneda", moneda);
        cuerpo.put("metodoPreferido", metodoPreferido);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("squad", "Matching y Agenda");
        payload.put("topico", "Pago");
        payload.put("evento", "Solicitud Pago Emitida");
        payload.put("cuerpo", cuerpo);

        PublishMessage message = new PublishMessage(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                properties.publishSource(),
                new Destination(properties.publishPagoChannel(), properties.publishPagoEventName()),
                payload
        );

        try {
            ResponseEntity<Void> response = matchingRestClient.post()
                    .uri(properties.publishPath())
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            log.info("Publicado evento Pago Emitida messageId={} status={}", message.messageId(), status);
            return PublishResult.success(message.messageId(), status);
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            log.error("Error publicando Pago Emitida messageId={} status={} body={}",
                    message.messageId(), status, e.getResponseBodyAsString());
            return PublishResult.failure(message.messageId(), status, e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Error publicando Pago Emitida messageId={}", message.messageId(), e);
            return PublishResult.failure(message.messageId(), HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
