package com.example.demo.response;

import com.example.demo.dto.ModuleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModuleResponseFactory {

    private final String webhookUrl;
    private final String teamName;

    public ModuleResponseFactory(
            @Value("${module.response.webhook-url:https://matching-squad.com/webhook}") String webhookUrl,
            @Value("${module.response.team-name:matching}") String teamName) {
        this.webhookUrl = webhookUrl;
        this.teamName = teamName;
    }

    public <T> ModuleResponse<T> build(String topic, String eventName, T message) {
        return ModuleResponse.<T>builder()
                .webhookUrl(webhookUrl)
                .teamName(teamName)
                .topic(topic)
                .eventName(eventName)
                .message(message)
                .build();
    }
}
