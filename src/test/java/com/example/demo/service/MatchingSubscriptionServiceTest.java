package com.example.demo.service;

import com.example.demo.config.MatchingIntegrationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchingSubscriptionServiceTest {

    @Mock
    private RestClient matchingRestClient;
    @Mock
    private MatchingIntegrationProperties properties;

    // Mocks for the fluent RestClient API
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;

    private MatchingSubscriptionService subscriptionService;

    private final String webhookUrl = "http://localhost/webhook";
    private final String teamName = "my-team";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup deep mocks for the RestClient fluent API
        when(matchingRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(matchingRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(properties.subscribePath()).thenReturn("/subscriptions");
        when(properties.ackPath()).thenReturn("/ack/{msgId}");
        when(properties.apiKey()).thenReturn("test-api-key");

        subscriptionService = new MatchingSubscriptionService(matchingRestClient, properties, webhookUrl, teamName);
    }

    // --- Tests for subscribe ---

    @Test
    @DisplayName("subscribe - Debe suscribirse correctamente y devolver éxito")
    void testSubscribe_Success() {
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        MatchingSubscriptionService.SubscriptionResult result = subscriptionService.subscribe("test.topic", "test.event");

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK, result.status());
        assertEquals("test.topic", result.topic());
        assertEquals("test.event", result.eventName());
        verify(requestBodySpec, times(1)).body(any());
    }

    @Test
    @DisplayName("subscribe - Debe manejar error de API y devolver fallo")
    void testSubscribe_ApiError() {
        RestClientResponseException ex = new RestClientResponseException(
                "Bad Request", HttpStatus.BAD_REQUEST, "Error", null, "{\"error\":\"invalid_topic\"}".getBytes(StandardCharsets.UTF_8), null);
        when(responseSpec.toBodilessEntity()).thenThrow(ex);

        MatchingSubscriptionService.SubscriptionResult result = subscriptionService.subscribe("test.topic", "test.event");

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST, result.status());
        assertEquals("{\"error\":\"invalid_topic\"}", result.errorBody());
    }

    @Test
    @DisplayName("subscribe - Debe manejar error de red y devolver fallo")
    void testSubscribe_NetworkError() {
        when(responseSpec.toBodilessEntity()).thenThrow(new RestClientException("Connection timed out"));

        MatchingSubscriptionService.SubscriptionResult result = subscriptionService.subscribe("test.topic", "test.event");

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.status());
        assertEquals("Connection timed out", result.errorBody());
    }

    @Test
    @DisplayName("subscribe - Debe lanzar excepción con topic nulo")
    void testSubscribe_NullTopic() {
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.subscribe(null, "test.event"));
    }

    @Test
    @DisplayName("subscribe - Debe construir topic correctamente desde partes")
    void testSubscribe_FromParts() {
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        MatchingSubscriptionService.SubscriptionResult result = subscriptionService.subscribe("team", "domain", "action", "event");

        assertTrue(result.isSuccess(), "Subscription should be successful");
        assertEquals("domain", result.topic(), "Topic should be 'domain'");
        assertEquals("event", result.eventName(), "Event name should be 'event'");
    }

    // --- Tests for acknowledgeMessage ---

    @Test
    @DisplayName("acknowledgeMessage - Debe enviar ACK correctamente y devolver éxito")
    void testAcknowledgeMessage_Success() {
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.accepted().build());

        MatchingSubscriptionService.AckResult result = subscriptionService.acknowledgeMessage("msg-123", "sub-456");

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.ACCEPTED, result.status());
        verify(requestBodyUriSpec, times(1)).uri("/ack/msg-123");
    }

    @Test
    @DisplayName("acknowledgeMessage - Debe lanzar excepción con messageId nulo o vacío")
    void testAcknowledgeMessage_InvalidMessageId() {
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage(null, "sub-456"));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage("  ", "sub-456"));
    }

    @Test
    @DisplayName("acknowledgeMessage - Debe lanzar excepción con subscriptionId nulo o vacío")
    void testAcknowledgeMessage_InvalidSubscriptionId() {
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage("msg-123", null));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.acknowledgeMessage("msg-123", "  "));
    }

    @Test
    @DisplayName("acknowledgeMessage - Debe manejar error de API y devolver fallo")
    void testAcknowledgeMessage_ApiError() {
        RestClientResponseException ex = new RestClientResponseException(
                "Not Found", HttpStatus.NOT_FOUND, "Error", null, "{\"error\":\"not_found\"}".getBytes(StandardCharsets.UTF_8), null);
        when(responseSpec.toBodilessEntity()).thenThrow(ex);

        MatchingSubscriptionService.AckResult result = subscriptionService.acknowledgeMessage("msg-123", "sub-456");

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.NOT_FOUND, result.status());
        assertEquals("{\"error\":\"not_found\"}", result.errorBody());
    }

    // --- Tests for listSubscriptions ---

    @Test
    @DisplayName("listSubscriptions - Debe listar suscripciones correctamente")
    void testListSubscriptions_Success() {
        List<MatchingSubscriptionService.SubscriptionDetails> mockList = List.of(
                new MatchingSubscriptionService.SubscriptionDetails("sub-1", webhookUrl, teamName, "t1", "e1", "ACTIVE", null)
        );
        when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn(mockList);

        MatchingSubscriptionService.SubscriptionListResult result = subscriptionService.listSubscriptions();

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK, result.status());
        assertEquals(1, result.subscriptions().size());
        assertEquals("sub-1", result.subscriptions().get(0).subscriptionId());
    }

    @Test
    @DisplayName("listSubscriptions - Debe manejar error de API")
    void testListSubscriptions_ApiError() {
        RestClientResponseException ex = new RestClientResponseException(
                "Forbidden", HttpStatus.FORBIDDEN, "Error", null, null, null);
        when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class))).thenThrow(ex);

        MatchingSubscriptionService.SubscriptionListResult result = subscriptionService.listSubscriptions();

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.FORBIDDEN, result.status());
        assertTrue(result.subscriptions().isEmpty());
    }

    // --- Tests for unsubscribe ---

    @Test
    @DisplayName("unsubscribe - Debe desuscribirse correctamente")
    void testUnsubscribe_Success() {
        RestClient.RequestHeadersSpec deleteSpec = mock(RestClient.RequestHeadersSpec.class);
        when(matchingRestClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(deleteSpec);
        when(deleteSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        MatchingSubscriptionService.SubscriptionResult result = subscriptionService.unsubscribe("sub-123");

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.NO_CONTENT, result.status());
        verify(matchingRestClient, times(1)).delete();
    }

    @Test
    @DisplayName("unsubscribe - Debe lanzar excepción con subscriptionId nulo o vacío")
    void testUnsubscribe_InvalidId() {
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.unsubscribe(null));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.unsubscribe(" "));
    }
}