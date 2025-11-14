package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class MatchingPublishRetryRequest {
    private List<Long> messageIds;
    private Integer max;
}
