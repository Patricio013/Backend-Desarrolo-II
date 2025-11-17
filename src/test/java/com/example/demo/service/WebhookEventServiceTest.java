package com.example.demo.service;

import com.example.demo.entity.WebhookEvent;
import com.example.demo.repository.WebhookEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookEventServiceTest {

    @Mock
    private WebhookEventRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookEventService webhookEventService;

    @Captor
    private ArgumentCaptor<WebhookEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- Tests for storeEvent ---

    @Test
    @DisplayName("storeEvent - Debe guardar un evento con datos válidos y serializar el payload")
    void testStoreEvent_Success() throws JsonProcessingException {
        // Arrange
        String topic = "test.topic";
        String eventName = "test.event";
        String messageId = "msg-123";
        String subscriptionId = "sub-456";
        Map<String, Object> payload = Map.of("key", "value");
        String serializedPayload = "{\"key\":\"value\"}";

        when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);
        when(repository.save(any(WebhookEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        webhookEventService.storeEvent(topic, eventName, messageId, subscriptionId, payload);

        // Assert
        verify(repository).save(eventCaptor.capture());
        WebhookEvent capturedEvent = eventCaptor.getValue();

        assertEquals(topic, capturedEvent.getTopic());
        assertEquals(eventName, capturedEvent.getEventName());
        assertEquals(messageId, capturedEvent.getMessageId());
        assertEquals(subscriptionId, capturedEvent.getSubscriptionId());
        assertEquals(serializedPayload, capturedEvent.getRawPayload());
    }

    @Test
    @DisplayName("storeEvent - Debe guardar campos en blanco como nulos")
    void testStoreEvent_BlankFieldsAreStoredAsNull() throws JsonProcessingException {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(repository.save(any(WebhookEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        webhookEventService.storeEvent("  ", "", null, "sub-123", Map.of());

        // Assert
        verify(repository).save(eventCaptor.capture());
        WebhookEvent capturedEvent = eventCaptor.getValue();

        assertNull(capturedEvent.getTopic());
        assertNull(capturedEvent.getEventName());
        assertNull(capturedEvent.getMessageId());
        assertEquals("sub-123", capturedEvent.getSubscriptionId());
    }

    @Test
    @DisplayName("storeEvent - Debe usar payload.toString() si la serialización JSON falla")
    void testStoreEvent_SerializationFailure() throws JsonProcessingException {
        // Arrange
        Map<String, Object> payload = Map.of("key", "value");
        when(objectMapper.writeValueAsString(payload)).thenThrow(new JsonProcessingException("Test Error") {});
        when(repository.save(any(WebhookEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        webhookEventService.storeEvent("topic", "event", "msg-123", "sub-456", payload);

        // Assert
        verify(repository).save(eventCaptor.capture());
        WebhookEvent capturedEvent = eventCaptor.getValue();

        assertEquals(payload.toString(), capturedEvent.getRawPayload());
    }

    @Test
    @DisplayName("storeEvent - Debe guardar un payload vacío como '{}'")
    void testStoreEvent_EmptyPayload() throws JsonProcessingException {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(repository.save(any(WebhookEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        webhookEventService.storeEvent("topic", "event", "msg-123", "sub-456", Map.of());

        // Assert
        verify(repository).save(eventCaptor.capture());
        assertEquals("{}", eventCaptor.getValue().getRawPayload());
    }

    // --- Tests for listEvents ---

    @Test
    @DisplayName("listEvents - Debe devolver una lista de eventos ordenados por fecha descendente")
    void testListEvents() {
        // Arrange
        List<WebhookEvent> mockEvents = List.of(new WebhookEvent(), new WebhookEvent());
        when(repository.findAll(Sort.by(Sort.Direction.DESC, "receivedAt"))).thenReturn(mockEvents);

        // Act
        List<WebhookEvent> result = webhookEventService.listEvents();

        // Assert
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll(Sort.by(Sort.Direction.DESC, "receivedAt"));
    }

    // --- Tests for findById ---

    @Test
    @DisplayName("findById - Debe devolver un evento si se encuentra")
    void testFindById_Found() {
        // Arrange
        WebhookEvent event = WebhookEvent.builder().id(1L).topic("found").build();
        when(repository.findById(1L)).thenReturn(Optional.of(event));

        // Act
        Optional<WebhookEvent> result = webhookEventService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("found", result.get().getTopic());
    }

    @Test
    @DisplayName("findById - Debe devolver un Optional vacío si no se encuentra")
    void testFindById_NotFound() {
        // Arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<WebhookEvent> result = webhookEventService.findById(99L);

        // Assert
        assertFalse(result.isPresent());
    }
}