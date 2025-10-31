package com.example.demo.dto;

import lombok.Data;

@Data
public class MatchingSubscriptionRequest {
    private String topic;
    private String eventName;
    private String targetTeamName;
    private String domain;
    private String action;
}
