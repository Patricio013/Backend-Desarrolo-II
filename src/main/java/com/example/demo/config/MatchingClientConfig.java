package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MatchingIntegrationProperties.class)
public class MatchingClientConfig {

    private static final Logger log = LoggerFactory.getLogger(MatchingClientConfig.class);

    @Bean
    public RestClient matchingRestClient(MatchingIntegrationProperties properties) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.hasApiKey()) {
            builder = builder.defaultHeader("X-API-KEY", properties.apiKey());
        } else {
            log.warn("Matching API key is empty; requests will be sent without authentication header");
        }

        return builder.build();
    }
}
