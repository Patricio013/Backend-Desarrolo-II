package com.example.demo.controller;

import com.example.demo.dto.MatchingSubscriptionRequest;
import com.example.demo.dto.ModuleResponse;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.MatchingSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/matching/subscriptions")
@RequiredArgsConstructor
public class MatchingSubscriptionController {

    private final MatchingSubscriptionService subscriptionService;
    private final ModuleResponseFactory responseFactory;

    @PostMapping
    public ResponseEntity<ModuleResponse<Map<String, Object>>> subscribe(@RequestBody MatchingSubscriptionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body required");
        }

        boolean hasTopic = StringUtils.hasText(request.getTopic());
        boolean hasParts = StringUtils.hasText(request.getTargetTeamName())
                && StringUtils.hasText(request.getDomain())
                && StringUtils.hasText(request.getAction());

        if (!hasTopic && !hasParts) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide either topic/eventName or targetTeamName/domain/action");
        }

        String topic;
        String eventName;
        MatchingSubscriptionService.SubscriptionResult result;

        if (hasTopic) {
            topic = request.getTopic().trim();
            eventName = resolveEventName(request.getEventName(), topic);
            result = subscriptionService.subscribe(topic, eventName);
        } else {
            String targetTeam = request.getTargetTeamName().trim();
            String domain = request.getDomain().trim();
            String action = request.getAction().trim();
            topic = String.join(".", targetTeam, domain, action);
            eventName = resolveEventName(request.getEventName(), action);
            result = subscriptionService.subscribe(topic, eventName);
        }

        HttpStatus status = result.status() != null ? result.status() : HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> responsePayload = buildResponsePayload(result.topic(), eventName, status, result.errorBody());

        log.info("Manual subscription attempt topic={} eventName={} status={} success={}",
                result.topic(), eventName, status, result.isSuccess());

        ModuleResponse<Map<String, Object>> body = responseFactory.build(
                "matching",
                "subscriptionAttempt",
                responsePayload
        );

        return ResponseEntity.status(status).body(body);
    }

    @GetMapping
    public ResponseEntity<ModuleResponse<Map<String, Object>>> list() {
        var result = subscriptionService.listSubscriptions();
        HttpStatus status = result.status() != null ? result.status() : HttpStatus.INTERNAL_SERVER_ERROR;

        Map<String, Object> payload = new HashMap<>();
        payload.put("statusCode", status.value());
        payload.put("count", result.subscriptions().size());
        payload.put("subscriptions", result.subscriptions());
        if (!result.isSuccess() && result.errorBody() != null) {
            payload.put("error", result.errorBody());
        }

        ModuleResponse<Map<String, Object>> body = responseFactory.build(
                "matching",
                "subscriptionsListed",
                payload
        );

        return ResponseEntity.status(status).body(body);
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<ModuleResponse<Map<String, Object>>> unsubscribe(@PathVariable String subscriptionId) {
        var result = subscriptionService.unsubscribe(subscriptionId);
        HttpStatus status = result.status() != null ? result.status() : HttpStatus.INTERNAL_SERVER_ERROR;

        Map<String, Object> payload = buildResponsePayload(result.topic(), result.eventName(), status, result.errorBody());
        payload.put("subscriptionId", subscriptionId);

        ModuleResponse<Map<String, Object>> body = responseFactory.build(
                "matching",
                "subscriptionRemoved",
                payload
        );

        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> buildResponsePayload(String topic, String eventName, HttpStatus status, String errorBody) {
        Map<String, Object> responsePayload = new HashMap<>();
        if (topic != null) {
            responsePayload.put("topic", topic);
        }
        if (eventName != null) {
            responsePayload.put("eventName", eventName);
        }
        responsePayload.put("statusCode", status.value());
        if (errorBody != null) {
            responsePayload.put("error", errorBody);
        }
        return responsePayload;
    }

    private String resolveEventName(String requestedEventName, String fallbackSource) {
        if (StringUtils.hasText(requestedEventName)) {
            return requestedEventName.trim();
        }
        int lastDot = fallbackSource.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fallbackSource.length() - 1) {
            return fallbackSource.substring(lastDot + 1);
        }
        return fallbackSource;
    }
}
