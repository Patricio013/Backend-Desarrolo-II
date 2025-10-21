package com.example.demo.config;

import com.example.demo.service.MatchingSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MatchingSubscriptionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MatchingSubscriptionInitializer.class);

    private final MatchingIntegrationProperties properties;
    private final MatchingSubscriptionService subscriptionService;

    @Value("${module.response.webhook-url:https://matching-squad.com/webhook}")
    private String webhookUrl;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.autoSubscribeEnabled()) {
            log.info("Matching auto-subscribe disabled; skipping startup subscription");
            return;
        }
        if (!properties.hasApiKey()) {
            log.warn("Matching auto-subscribe enabled but API key is missing; skipping subscription");
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Matching auto-subscribe requires module.response.webhook-url; skipping subscription");
            return;
        }

        var autoSubscriptions = properties.autoSubscriptions();
        if (autoSubscriptions.isEmpty()) {
            log.warn("Matching auto-subscribe enabled but no targets configured; skipping subscription");
            return;
        }

        var listResult = subscriptionService.listSubscriptions();
        Set<String> existingTopics = new HashSet<>();
        String normalizedWebhook = webhookUrl.trim();
        if (listResult.isSuccess()) {
            listResult.subscriptions().stream()
                    .filter(sub -> sub.topic() != null && sub.webhookUrl() != null)
                    .filter(sub -> normalizedWebhook.equalsIgnoreCase(sub.webhookUrl()))
                    .map(sub -> sub.topic().toLowerCase(Locale.ROOT))
                    .forEach(existingTopics::add);
        } else {
            log.warn("Could not verify existing matching subscriptions (status {}). Proceeding to subscribe anyway.",
                    listResult.status());
        }

        for (var target : autoSubscriptions) {
            String topic = String.join(".", target.team(), target.domain(), target.action());
            String topicKey = topic.toLowerCase(Locale.ROOT);
            if (existingTopics.contains(topicKey)) {
                log.info("Matching subscription already present for topic {} and webhook {}", topic, normalizedWebhook);
                continue;
            }

            var result = subscriptionService.subscribe(target.team(), target.domain(), target.action());
            if (result.isSuccess()) {
                log.info("Matching subscription ensured for topic {} (event {})", topic, result.eventName());
                existingTopics.add(topicKey);
                continue;
            }

            if (result.status() != null && result.status().is4xxClientError()
                    && result.errorBody() != null
                    && result.errorBody().toLowerCase(Locale.ROOT).contains("ya existe")) {
                log.info("Matching subscription already exists according to API response for topic {}", topic);
                existingTopics.add(topicKey);
            } else {
                log.error("Failed to ensure matching subscription for topic {}. Status={} error={}",
                        topic, result.status(), result.errorBody());
            }
        }
    }
}
