package com.example.demo.service;

import com.example.demo.config.MatchingIntegrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class MatchingSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(MatchingSubscriptionService.class);

    private final RestClient matchingRestClient;
    private final MatchingIntegrationProperties properties;
    private final String webhookUrl;
    private final String teamName;

    public MatchingSubscriptionService(
            RestClient matchingRestClient,
            MatchingIntegrationProperties properties,
            @Value("${module.response.webhook-url:https://matching-squad.com/webhook}") String webhookUrl,
            @Value("${module.response.team-name:matching}") String teamName
    ) {
        this.matchingRestClient = matchingRestClient;
        this.properties = properties;
        this.webhookUrl = webhookUrl;
        this.teamName = teamName;
    }

    public SubscriptionResult subscribe(String topic, String eventName) {
        if (topic == null || eventName == null) {
            throw new IllegalArgumentException("topic and eventName must not be null");
        }
        String safeTopic = topic.trim();
        String safeEvent = eventName.trim();
        SubscriptionRequest body = new SubscriptionRequest(webhookUrl, teamName, safeTopic, safeEvent);
        try {
            log.info("Enviando suscripción Matching topic={} eventName={} webhookUrl={} squadName={} apiKey={}",
                    safeTopic,
                    safeEvent,
                    webhookUrl,
                    teamName,
                    maskApiKey(properties.apiKey()));
            ResponseEntity<Void> response = matchingRestClient.post()
                    .uri(properties.subscribePath())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Matching subscription request sent topic={} eventName={} status={}"
                    + " webhookUrl={} squadName={}",
                    safeTopic, safeEvent, response.getStatusCode(), webhookUrl, teamName);
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            return SubscriptionResult.success(safeTopic, safeEvent, status, null);
        } catch (RestClientResponseException e) {
            log.error("Failed to subscribe to matching topic {} with event {}. Status={} body={}",
                    safeTopic, safeEvent, e.getStatusText(), e.getResponseBodyAsString());
            return SubscriptionResult.failure(safeTopic, safeEvent, HttpStatus.valueOf(e.getStatusCode().value()),
                    e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Failed to subscribe to matching topic {} with event {}", topic, eventName, e);
            return SubscriptionResult.failure(safeTopic, safeEvent, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public SubscriptionResult subscribe(String targetTeamName, String domain, String action) {
        return subscribe(targetTeamName, domain, action, null);
    }

    public SubscriptionResult subscribe(String targetTeamName, String domain, String action, String eventName) {
        if (targetTeamName == null || domain == null || action == null) {
            throw new IllegalArgumentException("targetTeamName, domain and action must be provided");
        }
        String normalizedTeam = targetTeamName.trim();
        String normalizedDomain = domain.trim();
        String normalizedAction = action.trim();
        String topic = String.join(".", normalizedTeam, normalizedDomain, normalizedAction);
        String normalizedEventName = (eventName != null && !eventName.isBlank())
                ? eventName.trim()
                : normalizedAction;
        return subscribe(topic, normalizedEventName);
    }

    public AckResult acknowledgeMessage(String messageId, String subscriptionId) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId must not be empty");
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("subscriptionId must not be empty");
        }

        String trimmedMessageId = messageId.trim();
        String trimmedSubscriptionId = subscriptionId.trim();
        String ackPath = properties.ackPath().replace("{msgId}", trimmedMessageId);

        AckRequest body = new AckRequest(trimmedMessageId, trimmedSubscriptionId);
        try {
            ResponseEntity<Void> response = matchingRestClient.post()
                    .uri(ackPath)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            log.info("ACK sent for message {} subscription {} status {}", trimmedMessageId, trimmedSubscriptionId, status);
            return AckResult.success(status, null);
        } catch (RestClientResponseException e) {
            HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
            log.error("Failed to ACK message {} subscription {} status {} body {}",
                    trimmedMessageId, trimmedSubscriptionId, status, e.getResponseBodyAsString());
            return AckResult.failure(status, e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Failed to ACK message {} subscription {}", trimmedMessageId, trimmedSubscriptionId, e);
            return AckResult.failure(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public SubscriptionListResult listSubscriptions() {
        try {
            List<SubscriptionDetails> subscriptions = matchingRestClient.get()
                    .uri(properties.subscribePath())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            List<SubscriptionDetails> safeList = subscriptions != null ? subscriptions : List.of();
            return new SubscriptionListResult(HttpStatus.OK, safeList, null);
        } catch (RestClientResponseException e) {
            log.error("Failed to list matching subscriptions. Status={} body={}",
                    e.getStatusText(), e.getResponseBodyAsString());
            return new SubscriptionListResult(HttpStatus.valueOf(e.getStatusCode().value()), Collections.emptyList(),
                    e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Failed to list matching subscriptions", e);
            return new SubscriptionListResult(HttpStatus.INTERNAL_SERVER_ERROR, Collections.emptyList(), e.getMessage());
        }
    }

    public SubscriptionResult unsubscribe(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("subscriptionId must not be empty");
        }
        String trimmed = subscriptionId.trim();
        try {
            ResponseEntity<Void> response = matchingRestClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.subscribePath())
                            .pathSegment(trimmed)
                            .build())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Matching unsubscribe request sent subscriptionId={} status={}",
                    trimmed, response.getStatusCode());
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            return SubscriptionResult.success(null, null, status, null);
        } catch (RestClientResponseException e) {
            log.error("Failed to unsubscribe subscriptionId={} status={} body={}",
                    trimmed, e.getStatusText(), e.getResponseBodyAsString());
            return SubscriptionResult.failure(null, null,
                    HttpStatus.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Failed to unsubscribe subscriptionId={}", trimmed, e);
            return SubscriptionResult.failure(null, null, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private record SubscriptionRequest(
            String webhookUrl,
            String squadName,
            String topic,
            String eventName
    ) {
    }

    private record AckRequest(String msgId, String subscriptionId) {
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "<empty>";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 6) {
            return trimmed.charAt(0) + "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 2);
    }

    public record SubscriptionResult(
            String topic,
            String eventName,
            HttpStatus status,
            String errorBody
    ) {
        public boolean isSuccess() {
            return status != null && status.is2xxSuccessful();
        }

        public static SubscriptionResult success(String topic, String eventName, HttpStatus status, String errorBody) {
            return new SubscriptionResult(topic, eventName, status, errorBody);
        }

        public static SubscriptionResult failure(String topic, String eventName, HttpStatus status, String errorBody) {
            return new SubscriptionResult(topic, eventName, status, errorBody);
        }
    }

    public record SubscriptionDetails(
            String subscriptionId,
            String webhookUrl,
            String squadName,
            String topic,
            String eventName,
            String status,
            Instant createdAt
    ) {
    }

    public record SubscriptionListResult(
            HttpStatus status,
            List<SubscriptionDetails> subscriptions,
            String errorBody
    ) {
        public boolean isSuccess() {
            return status != null && status.is2xxSuccessful();
        }
    }

    public record AckResult(HttpStatus status, String errorBody) {
        public boolean isSuccess() {
            return status != null && status.is2xxSuccessful();
        }

        public static AckResult success(HttpStatus status, String errorBody) {
            return new AckResult(status, errorBody);
        }

        public static AckResult failure(HttpStatus status, String errorBody) {
            return new AckResult(status, errorBody);
        }
    }
}
