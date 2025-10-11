package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ConfigurationProperties(prefix = "integrations.matching")
public record MatchingIntegrationProperties(
        String baseUrl,
        String apiKey,
        String subscribePath,
        String ackPath,
        boolean autoSubscribeEnabled,
        List<AutoSubscription> autoSubscriptions,
        boolean publishEnabled,
        String publishPath,
        String publishSource,
        String publishTop3Channel,
        String publishTop3EventName
) {

    public MatchingIntegrationProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("integrations.matching.base-url must not be empty");
        }
        if (subscribePath == null || subscribePath.isBlank()) {
            subscribePath = "/subscribe";
        } else if (!subscribePath.startsWith("/")) {
            subscribePath = "/" + subscribePath;
        }

        if (ackPath == null || ackPath.isBlank()) {
            ackPath = "/messages/{msgId}/ack";
        } else if (!ackPath.startsWith("/")) {
            ackPath = "/" + ackPath;
        }
        if (!ackPath.contains("{msgId}")) {
            throw new IllegalStateException("integrations.matching.ack-path must contain {msgId} placeholder");
        }

        if (autoSubscribeEnabled) {
            if (autoSubscriptions == null || autoSubscriptions.isEmpty()) {
                throw new IllegalStateException("At least one auto subscription must be configured when integrations.matching.auto-subscribe-enabled=true");
            }
        }

        autoSubscriptions = normalize(autoSubscriptions);

        if (publishPath == null || publishPath.isBlank()) {
            publishPath = "/publish";
        } else if (!publishPath.startsWith("/")) {
            publishPath = "/" + publishPath;
        }

        if (publishSource == null || publishSource.isBlank()) {
            publishSource = "matching";
        }

        if (publishTop3Channel == null || publishTop3Channel.isBlank()) {
            throw new IllegalStateException("integrations.matching.publish-top3-channel must not be empty");
        }
        if (publishTop3EventName == null || publishTop3EventName.isBlank()) {
            throw new IllegalStateException("integrations.matching.publish-top3-event must not be empty");
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<AutoSubscription> autoSubscriptions() {
        return autoSubscriptions == null ? List.of() : autoSubscriptions;
    }

    private static List<AutoSubscription> normalize(List<AutoSubscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return Collections.emptyList();
        }
        return subscriptions.stream()
                .filter(Objects::nonNull)
                .map(AutoSubscription::normalize)
                .toList();
    }

    public record AutoSubscription(String team, String domain, String action, String eventName) {
        private static AutoSubscription normalize(AutoSubscription input) {
            String normalizedTeam = requireNonBlank(input.team, "team").toLowerCase(Locale.ROOT);
            String normalizedDomain = requireNonBlank(input.domain, "domain").toLowerCase(Locale.ROOT);
            String normalizedAction = requireNonBlank(input.action, "action").toLowerCase(Locale.ROOT);
            String normalizedEventName = (input.eventName == null || input.eventName.isBlank())
                    ? normalizedAction
                    : input.eventName.trim();
            return new AutoSubscription(
                    normalizedTeam,
                    normalizedDomain,
                    normalizedAction,
                    normalizedEventName
            );
        }

        private static String requireNonBlank(String value, String propertyName) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Auto subscription " + propertyName + " must not be empty");
            }
            return value.trim();
        }

        public String eventNameOrAction() {
            return (eventName == null || eventName.isBlank()) ? action : eventName;
        }
    }
}
