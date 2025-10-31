package com.example.demo.service;

import com.example.demo.config.MatchingIntegrationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchingSubscriptionServiceTest {

    private MatchingSubscriptionService subscriptionService;

    @Mock private RestClient matchingRestClient;
    @Mock private MatchingIntegrationProperties properties;

    // Mocks for RestClient fluent API
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    @Captor private ArgumentCaptor<Object> bodyCaptor;

    private static final String TEST_WEBHOOK_URL = "http://example.com/webhook";
    private static final String TEST_TEAM_NAME = "my-team";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        subscriptionService = new MatchingSubscriptionService(matchingRestClient, properties, TEST_WEBHOOK_URL, TEST_TEAM_NAME);

        // Common mock chain for POST
        when(matchingRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        // Common mock chain for GET
        when(matchingRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Common mock chain for DELETE
        when(matchingRestClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenAnswer(invocation -> {
            Function<UriBuilder, URI> uriFunction = invocation.getArgument(0);
            // We don't need a real UriBuilder, just to return the spec
            return requestHeadersSpec;
        });
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("subscribe - Lanza excepción con argumentos nulos")
    void testSubscribe_NullArgs() {
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.subscribe(null, "event"));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.subscribe("topic", null));
    }

    @Test
    @DisplayName("acknowledgeMessage - Lanza excepción con argumentos inválidos")
    void testAcknowledgeMessage_InvalidArgs() {
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage(null, "sub-123"));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage("  ", "sub-123"));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage("msg-123", null));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage("msg-123", ""));
    }

    @Test
    @DisplayName("listSubscriptions - Éxito al listar suscripciones")
    void testListSubscriptions_Success() {
        // Arrange
        when(properties.subscribePath()).thenReturn("/subscribe");
        List<MatchingSubscriptionService.SubscriptionDetails> mockList = List.of(
                new MatchingSubscriptionService.SubscriptionDetails("sub-1", TEST_WEBHOOK_URL, TEST_TEAM_NAME, "topic1", "event1", "ACTIVE", null)
        );
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(mockList);

        // Act
        MatchingSubscriptionService.SubscriptionListResult result = subscriptionService.listSubscriptions();

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK, result.status());
        assertEquals(1, result.subscriptions().size());
        assertEquals("sub-1", result.subscriptions().get(0).subscriptionId());
    }

    @Test
    @DisplayName("listSubscriptions - Falla por error de API")
    void testListSubscriptions_ApiError() {
        // Arrange
        when(properties.subscribePath()).thenReturn("/subscribe");
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientResponseException("Server Error", HttpStatus.INTERNAL_SERVER_ERROR, "Error", null, null, null));

        // Act
        MatchingSubscriptionService.SubscriptionListResult result = subscriptionService.listSubscriptions();

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.status());
        assertTrue(result.subscriptions().isEmpty());
    }

    @Test
    @DisplayName("unsubscribe - Éxito al desuscribir")
    void testUnsubscribe_Success() {
        // Arrange
        when(properties.subscribePath()).thenReturn("/subscribe");
        when(responseSpec.toBodilessEntity()).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        // Act
        MatchingSubscriptionService.SubscriptionResult result = subscriptionService.unsubscribe("sub-123");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.NO_CONTENT, result.status());
        verify(requestHeadersUriSpec).uri(any(Function.class));
    }

    @Test
    @DisplayName("unsubscribe - Falla si la suscripción no existe")
    void testUnsubscribe_NotFound() {
        // Arrange
        when(properties.subscribePath()).thenReturn("/subscribe");
        when(responseSpec.toBodilessEntity()).thenThrow(new RestClientResponseException("Not Found", HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // Act
        MatchingSubscriptionService.SubscriptionResult result = subscriptionService.unsubscribe("sub-123");

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.NOT_FOUND, result.status());
    }

    @Test
    @DisplayName("unsubscribe - Lanza excepción con ID inválido")
    void testUnsubscribe_InvalidId() {
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.unsubscribe(null));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.unsubscribe(" "));
    }
}