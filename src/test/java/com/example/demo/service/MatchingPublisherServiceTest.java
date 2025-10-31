package com.example.demo.service;

import com.example.demo.config.MatchingIntegrationProperties;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.entity.Cotizacion;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchingPublisherServiceTest {

    @InjectMocks
    private MatchingPublisherService matchingPublisherService;

    @Mock
    private RestClient matchingRestClient;

    @Mock
    private MatchingIntegrationProperties properties;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock the fluent API of RestClient
        when(matchingRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    // --- Tests for publishSolicitudesTop3 ---

    @Test
    @DisplayName("publishSolicitudesTop3 - Omite si la publicación está deshabilitada")
    void testPublishSolicitudesTop3_Disabled() {
        // Arrange
        when(properties.publishEnabled()).thenReturn(false);

        // Act
        MatchingPublisherService.PublishResult result = matchingPublisherService.publishSolicitudesTop3(List.of(createSolicitudTop3Resultado()));

        // Assert
        assertFalse(result.success());
        assertEquals("Publishing disabled by configuration", result.errorMessage());
        verify(matchingRestClient, never()).post();
    }

    @Test
    @DisplayName("publishSolicitudesTop3 - Omite si la lista de resultados es nula o vacía")
    void testPublishSolicitudesTop3_EmptyInput() {
        // Arrange
        when(properties.publishEnabled()).thenReturn(true);

        // Act
        MatchingPublisherService.PublishResult resultNull = matchingPublisherService.publishSolicitudesTop3(null);
        MatchingPublisherService.PublishResult resultEmpty = matchingPublisherService.publishSolicitudesTop3(Collections.emptyList());

        // Assert
        assertFalse(resultNull.success());
        assertFalse(resultEmpty.success());
        verify(matchingRestClient, never()).post();
    }

    // --- Tests for publishCotizaciones ---

    @Test
    @DisplayName("publishCotizaciones - Omite si la lista de cotizaciones es nula o vacía")
    void testPublishCotizaciones_EmptyInput() {
        // Arrange
        when(properties.publishEnabled()).thenReturn(true);
        Solicitud solicitud = Solicitud.builder().id(1L).build();

        // Act
        MatchingPublisherService.PublishResult resultNull = matchingPublisherService.publishCotizaciones(solicitud, null, 3);
        MatchingPublisherService.PublishResult resultEmpty = matchingPublisherService.publishCotizaciones(solicitud, Collections.emptyList(), 3);

        // Assert
        assertFalse(resultNull.success());
        assertFalse(resultEmpty.success());
        verify(matchingRestClient, never()).post();
    }

    // --- Tests for publishSolicitudPagoEmitida ---

    @Test
    @DisplayName("publishSolicitudPagoEmitida - Omite si la publicación está deshabilitada")
    void testPublishSolicitudPagoEmitida_Disabled() {
        // Arrange
        when(properties.publishEnabled()).thenReturn(false);

        // Act
        MatchingPublisherService.PublishResult result = matchingPublisherService.publishSolicitudPagoEmitida(
                "corr-123", 1L, 100L, 200L, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO,
                "ARS", "MP", "Concepto", "Descripcion"
        );

        // Assert
        assertFalse(result.success());
        assertEquals("Publishing disabled by configuration", result.errorMessage());
        verify(matchingRestClient, never()).post();
    }

    // --- Helper Methods ---

    private SolicitudTop3Resultado createSolicitudTop3Resultado() {
        SolicitudTop3Resultado resultado = new SolicitudTop3Resultado();
        resultado.setSolicitudId(1L);
        resultado.setDescripcion("Test Desc");
        resultado.setEstado("COTIZANDO");
        resultado.setTop3(List.of(
                InvitacionCotizacionDTO.builder()
                        .prestadorId(100L)
                        .prestadorNombre("Juan Perez")
                        .build()
        ));
        return resultado;
    }

    private Map<String, Object> getPayloadFromCapturedMessage() {
        Object captured = messageCaptor.getValue();
        assertInstanceOf(Map.class, captured, "El cuerpo del mensaje debe ser un Map");
        Map<String, Object> message = (Map<String, Object>) captured;
        Object payload = message.get("payload");
        assertInstanceOf(Map.class, payload, "El payload debe ser un Map");
        return (Map<String, Object>) payload;
    }
}