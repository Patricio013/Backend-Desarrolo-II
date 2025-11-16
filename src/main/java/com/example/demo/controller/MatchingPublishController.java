package com.example.demo.controller;

import com.example.demo.dto.MatchingPublishRetryRequest;
import com.example.demo.dto.ModuleResponse;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.MatchingPublisherService;
import lombok.RequiredArgsConstructor;
import com.example.demo.entity.MatchingPublishMessage;
import com.example.demo.entity.MatchingPublishMessage.PublishStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matching/publish")
@RequiredArgsConstructor
public class MatchingPublishController {

    private final MatchingPublisherService matchingPublisherService;
    private final ModuleResponseFactory responseFactory;

    @PostMapping("/retry")
    public ResponseEntity<ModuleResponse<Map<String, Object>>> retry(@RequestBody(required = false) MatchingPublishRetryRequest request) {
        var result = matchingPublisherService.retryPendingMessages(
                request != null ? request.getMessageIds() : null,
                request != null && request.getMax() != null ? request.getMax() : 0
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("attempted", result.attempted());
        payload.put("sent", result.sent());
        payload.put("failed", result.failed());
        payload.put("remainingPending", result.remainingPending());
        payload.put("processedIds", result.processedMessageIds());

        return ResponseEntity.ok(responseFactory.build("matching", "publishRetry", payload));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<MatchingPublishMessage>> listMessages(
            @RequestParam(name = "status", required = false) PublishStatus status,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        List<MatchingPublishMessage> messages = matchingPublisherService.listMessages(status, limit);
        return ResponseEntity.ok(messages);
    }
}
