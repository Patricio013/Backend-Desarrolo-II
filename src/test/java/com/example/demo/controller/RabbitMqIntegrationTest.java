package com.example.demo.rabbit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.AmqpTemplate;

import static org.mockito.Mockito.*;

class RabbitMqIntegrationTest {

    @Mock
    private AmqpTemplate amqpTemplate;

    @InjectMocks
    private DummyPublisher dummyPublisher;

    public RabbitMqIntegrationTest() {
        MockitoAnnotations.openMocks(this);
    }

    static class DummyPublisher {
        private final AmqpTemplate amqpTemplate;

        public DummyPublisher(AmqpTemplate amqpTemplate) {
            this.amqpTemplate = amqpTemplate;
        }

        public void enviar(String exchange, String routingKey, Object payload) {
            amqpTemplate.convertAndSend(exchange, routingKey, payload);
        }
    }

    @Test
    @DisplayName("Debe enviar mensaje a Rabbit usando AmqpTemplate mockeado")
    void testEnviarMensajeRabbit() {
        // Arrange
        String exchange = "test.exchange";
        String routingKey = "test.key";
        String payload = "{\"test\":\"ok\"}";

        // Act
        dummyPublisher.enviar(exchange, routingKey, payload);

        // Assert
        verify(amqpTemplate, times(1)).convertAndSend(exchange, routingKey, payload);
        verifyNoMoreInteractions(amqpTemplate);
    }
}
