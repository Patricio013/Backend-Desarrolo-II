package com.example.demo.response;

import com.example.demo.dto.ModuleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModuleResponseFactory {

    private final String webhookUrl;
    private final String squadName;

    public ModuleResponseFactory(
            @Value("${module.response.webhook-url:https://matching-squad.com/webhook}") String webhookUrl,
            @Value("${module.response.squad-name:matching-squad}") String squadName) {
        this.webhookUrl = webhookUrl;
        this.squadName = squadName;
    }

    public <T> ModuleResponse<T> build(String topic, String eventName, T message) {
        return ModuleResponse.<T>builder()
                .webhookUrl(webhookUrl)
                .squadName(squadName)
                .topic(topic)
                .eventName(eventName)
                .message(message)
                .build();
    }
}
