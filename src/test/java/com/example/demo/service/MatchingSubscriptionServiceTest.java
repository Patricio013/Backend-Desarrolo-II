package com.example.demo.service;

import com.example.demo.dto.MatchingSubscriptionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchingSubscriptionServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private MatchingSubscriptionService matchingSubscriptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Debería suscribirse correctamente cuando la API responde 200 OK")
    void testSubscribeSuccess() {
        // Arrange
        MatchingSubscriptionRequest request = new MatchingSubscriptionRequest(
                "test.topic", "test.event", "http://localhost/webhook", "my-team", "apikey"
        );

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntityStub.ok());

        // Act
        var result = matchingSubscriptionService.subscribe(request);

        // Assert
        assertEquals(HttpStatus.OK, result.status());
        verify(restClient, times(1)).post();
    }

    @Test
    @DisplayName("Debería manejar error HTTP 500 del servidor")
    void testSubscribeApiError() {
        MatchingSubscriptionRequest request = new MatchingSubscriptionRequest(
                "domain", "event", "http://localhost/webhook", "team", "key"
        );

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("Internal Server Error"));

        var result = matchingSubscriptionService.subscribe(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.status());
        verify(restClient, times(1)).post();
    }

    @Test
    @DisplayName("Debería manejar error de red al intentar suscribirse")
    void testSubscribeNetworkError() {
        MatchingSubscriptionRequest request = new MatchingSubscriptionRequest(
                "network", "fail", "http://webhook", "squad", "key"
        );

        when(restClient.post()).thenThrow(new RuntimeException("Network timeout"));

        var result = matchingSubscriptionService.subscribe(request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.status());
    }

    /** Mock de ResponseEntity simplificada para no importar clases HTTP completas **/
    private static class ResponseEntityStub {
        static ResponseEntityStub ok() { return new ResponseEntityStub(HttpStatus.OK); }
        final HttpStatus status;
        ResponseEntityStub(HttpStatus status) { this.status = status; }
        HttpStatus getStatusCode() { return status; }
    }
}
