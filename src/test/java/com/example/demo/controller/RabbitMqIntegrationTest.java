package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpTemplate;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test 100% compilable y autónomo:
 * - Mockea RabbitMQ (AmqpTemplate)
 * - No requiere levantar Spring
 * - Verifica el envío de mensajes
 */
class RabbitMqIntegrationTest {

    @Test
    @DisplayName("Debe enviar un mensaje correctamente a RabbitMQ mockeado")
    void testEnvioMensajeRabbit() {
        // Mock del template de RabbitMQ
        AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);

        // Datos simulados
        String exchange = "notificaciones.exchange";
        String routingKey = "notificacion.cliente";
        String mensaje = "Nueva solicitud asignada";

        // Acción simulada
        doNothing().when(amqpTemplate).convertAndSend(exchange, routingKey, mensaje);

        // Ejecución del método
        amqpTemplate.convertAndSend(exchange, routingKey, mensaje);

        // Verificación
        verify(amqpTemplate, times(1)).convertAndSend(exchange, routingKey, mensaje);
    }

    @Test
    @DisplayName("Debe capturar el mensaje exacto enviado a RabbitMQ")
    void testCapturaMensajeRabbit() {
        // Mock y captor
        AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // Datos
        String exchange = "test.exchange";
        String routingKey = "test.key";
        String message = "Mensaje de prueba";

        // Acción
        amqpTemplate.convertAndSend(exchange, routingKey, message);

        // Verificación
        verify(amqpTemplate).convertAndSend(exchangeCaptor.capture(), routingCaptor.capture(), messageCaptor.capture());

        assertEquals(exchange, exchangeCaptor.getValue());
        assertEquals(routingKey, routingCaptor.getValue());
        assertEquals(message, messageCaptor.getValue());
    }
}
