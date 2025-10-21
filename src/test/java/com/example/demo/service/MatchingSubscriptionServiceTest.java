package com.example.demo.service;

import com.example.demo.dto.MatchingSubscriptionRequest;
import com.example.demo.dto.ModuleResponse;
import com.example.demo.entity.Prestador;
import com.example.demo.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchingSubscriptionService
 * Simula todos los escenarios posibles del servicio.
 */
class MatchingSubscriptionServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @InjectMocks
    private MatchingSubscriptionService matchingSubscriptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Debería crear o actualizar una suscripción correctamente")
    void testCreateOrUpdateSubscription_Success() {
        MatchingSubscriptionRequest request = new MatchingSubscriptionRequest();
        request.setPrestadorId(1L);
        request.setEmail("test@example.com");
        request.setCategoriasSuscritas(new String[]{"Electricidad", "Plomería"});

        Prestador prestador = new Prestador();
        prestador.setId(1L);
        prestador.setEmail("test@example.com");

        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));
        when(prestadorRepository.save(any(Prestador.class))).thenReturn(prestador);

        ModuleResponse result = matchingSubscriptionService.createOrUpdateSubscription(request);

        assertNotNull(result);
        assertEquals("OK", result.getStatus());
        verify(prestadorRepository, times(1)).save(prestador);
    }

    @Test
    @DisplayName("Debería lanzar excepción si el prestador no existe")
    void testCreateOrUpdateSubscription_PrestadorNotFound() {
        MatchingSubscriptionRequest request = new MatchingSubscriptionRequest();
        request.setPrestadorId(999L);

        when(prestadorRepository.findById(999L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                matchingSubscriptionService.createOrUpdateSubscription(request)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("no existe"));
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería manejar correctamente un request nulo")
    void testCreateOrUpdateSubscription_NullRequest() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                matchingSubscriptionService.createOrUpdateSubscription(null)
        );

        assertTrue(ex.getMessage().contains("request"));
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería manejar lista de categorías vacía")
    void testCreateOrUpdateSubscription_EmptyCategories() {
        MatchingSubscriptionRequest request = new MatchingSubscriptionRequest();
        request.setPrestadorId(1L);
        request.setCategoriasSuscritas(new String[]{});

        Prestador prestador = new Prestador();
        prestador.setId(1L);

        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));

        ModuleResponse result = matchingSubscriptionService.createOrUpdateSubscription(request);

        assertEquals("OK", result.getStatus());
        assertNotNull(result.getMessage());
        verify(prestadorRepository, times(1)).save(prestador);
    }

    @Test
    @DisplayName("Debería manejar correctamente errores inesperados al guardar")
    void testCreateOrUpdateSubscription_SaveError() {
        MatchingSubscriptionRequest request = new MatchingSubscriptionRequest();
        request.setPrestadorId(1L);

        Prestador prestador = new Prestador();
        prestador.setId(1L);

        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));
        when(prestadorRepository.save(any(Prestador.class)))
                .thenThrow(new RuntimeException("Error al guardar"));

        Exception ex = assertThrows(RuntimeException.class, () ->
                matchingSubscriptionService.createOrUpdateSubscription(request)
        );

        assertTrue(ex.getMessage().contains("guardar"));
        verify(prestadorRepository, times(1)).save(prestador);
    }
}
