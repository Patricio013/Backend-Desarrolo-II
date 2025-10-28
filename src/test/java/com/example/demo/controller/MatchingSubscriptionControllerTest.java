package com.example.demo.controller;

import com.example.demo.service.MatchingSubscriptionService;
import com.example.demo.service.MatchingSubscriptionService.SubscriptionResult;
import com.example.demo.service.MatchingSubscriptionService.AckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchingSubscriptionController.class)
class MatchingSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchingSubscriptionService service;

    @Autowired
    private ObjectMapper objectMapper;

    private SubscriptionResult successResult;
    private SubscriptionResult failResult;

    @BeforeEach
    void setup() {
        successResult = SubscriptionResult.success(
                "topic.test", "event", HttpStatus.OK, null
        );

        failResult = SubscriptionResult.failure(
                "topic.test", "event", HttpStatus.INTERNAL_SERVER_ERROR, "error"
        );
    }

    // --- POST /api/subscriptions/subscribe ---
//    @Test
    void testSubscribeSuccess() throws Exception {
        when(service.subscribe(anyString(), anyString())).thenReturn(successResult);

        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"topic.test\",\"event\":\"event\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.topic").value("topic.test"));
    }

//    @Test
    void testSubscribeFailure() throws Exception {
        when(service.subscribe(anyString(), anyString())).thenReturn(failResult);

        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"topic.test\",\"event\":\"event\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errorBody").value("error"));
    }

//    @Test
    void testSubscribeInvalidInput() throws Exception {
        mockMvc.perform(post("/api/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"\",\"event\":\"event\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/subscriptions ---
//    @Test
    void testListSubscriptionsSuccess() throws Exception {
        when(service.listSubscriptions()).thenReturn(
                SubscriptionResult.success(null, null, HttpStatus.OK,
                        List.of(new MatchingSubscriptionService.SubscriptionDetails(
                                "id", "url", "team", "topic", "event", "ACTIVE", null
                        ))
                )

        );

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.subscriptions").isArray())
                .andExpect(jsonPath("$.subscriptions[0].id").value("id"));
    }

//    @Test
    void testListSubscriptionsFailure() throws Exception {
        when(service.listSubscriptions()).thenReturn(
                SubscriptionResult.failure(null, null, HttpStatus.INTERNAL_SERVER_ERROR, "error")
        );

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorBody").value("error"));
    }

    // --- POST /api/subscriptions/ack/{msgId} ---
//    @Test
    void testAcknowledgeSuccess() throws Exception {
        when(service.acknowledgeMessage(eq("123"), anyString()))
                .thenReturn(AckResult.success("sub-1", "123", HttpStatus.OK));

        mockMvc.perform(post("/api/subscriptions/ack/123")
                        .param("subscriptionId", "sub-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

//    @Test
    void testAcknowledgeFailure() throws Exception {
        when(service.acknowledgeMessage(eq("123"), anyString()))
                .thenReturn(AckResult.failure("sub-1", "123", HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        mockMvc.perform(post("/api/subscriptions/ack/123")
                        .param("subscriptionId", "sub-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorBody").value("error"));
    }

    // --- DELETE /api/subscriptions/{id} ---
//    @Test
    void testUnsubscribeSuccess() throws Exception {
        when(service.unsubscribe(eq("abc"))).thenReturn(successResult);

        mockMvc.perform(delete("/api/subscriptions/abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

//    @Test
    void testUnsubscribeFailure() throws Exception {
        when(service.unsubscribe(eq("abc"))).thenReturn(failResult);

        mockMvc.perform(delete("/api/subscriptions/abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorBody").value("error"));
    }

//    @Test
    void testUnsubscribeInvalidInput() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/"))
                .andExpect(status().is4xxClientError());
    }
}
