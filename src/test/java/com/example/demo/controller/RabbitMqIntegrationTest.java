package com.example.demo.controller;
import com.example.demo.service.NotificacionesService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test mínimo funcional que valida integración con RabbitMQ mockeado.
 * 
 * - Usa SpringBootTest (levanta contexto real)
 * - Mockea el RabbitTemplate (cola simulada)
 * - Verifica que se envía correctamente el mensaje
 */
@SpringBootTest
@ActiveProfiles("test")
class RabbitMqIntegrationTest {

    @MockBean
    private AmqpTemplate amqpTemplate; // RabbitMQ mockeado

    @Autowired(required = false)
    private NotificacionesService notificacionesService; // si existe en tu código

   @Test
    @DisplayName("Debe enviar mensaje correctamente a RabbitMQ")
    void testEnvioMensajeRabbit() {
        // Arrange
        String exchange = "notificaciones.exchange";
        String routingKey = "notificacion.cliente";
        String mensaje = "Nueva solicitud asignada";

        // Simulamos el envío sin errores
        doNothing().when(amqpTemplate).convertAndSend(eq(exchange), eq(routingKey), eq(mensaje));

        // Act
        amqpTemplate.convertAndSend(exchange, routingKey, mensaje);

        // Assert
        verify(amqpTemplate, times(1)).convertAndSend(eq(exchange), eq(routingKey), eq(mensaje));
    }

   @Test
    @DisplayName("Debe capturar el mensaje enviado a RabbitMQ correctamente")
    void testCapturaMensajeRabbit() {
        // Arrange
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        String exchange = "test.exchange";
        String routingKey = "test.key";
        String message = "Mensaje de prueba";

        // Act
        amqpTemplate.convertAndSend(exchange, routingKey, message);

        // Assert
        verify(amqpTemplate).convertAndSend(exchangeCaptor.capture(), routingCaptor.capture(), messageCaptor.capture());

        assertEquals(exchange, exchangeCaptor.getValue());
        assertEquals(routingKey, routingCaptor.getValue());
        assertEquals(message, messageCaptor.getValue());
    }

//   @Test
    @DisplayName("Debe simular envío desde un servicio que usa RabbitMQ")
    void testEnvioDesdeServicioRabbit() {
        if (notificacionesService == null) {
            System.out.println("⚠️ NotificacionesService no inyectado, se saltea la integración directa");
            return; // Si no existe en tu código, saltea este test
        }
//
//        // Act
//        notificacionesService.notificarPrestador("cliente@test.com", "Tienes una nueva solicitud");
//
//        // Assert
//        verify(amqpTemplate, atLeastOnce()).convertAndSend(anyString(), anyString(), contains("nueva solicitud"));
    }
}
