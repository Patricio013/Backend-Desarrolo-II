package com.example.demo.service;

import com.example.demo.entity.Prestador;
import com.example.demo.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ✅ Test unitario para PrestadorSyncService
 * - Mockea RabbitTemplate y MatchingPublisherService
 * - Verifica sincronización, obtención y manejo de errores
 */
class PrestadorSyncServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MatchingPublisherService matchingPublisherService;

    @InjectMocks
    private PrestadorSyncService prestadorSyncService;

    private Prestador prestador;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        prestador = new Prestador();
        prestador.setId(1L);
        prestador.setNombre("Carlos Pérez");
        prestador.setEmail("carlos@example.com");
        prestador.setActivo(true);
    }

    // =========================================================
    // ✅ TEST: Sincronización de un solo prestador
    // =========================================================
    @Test
    @DisplayName("Debe sincronizar un prestador correctamente con RabbitMQ")
    void testSincronizarPrestador_Success() {
        when(prestadorRepository.findAll()).thenReturn(List.of(prestador));
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any());

        prestadorSyncService.sincronizarPrestadores();

        verify(prestadorRepository, times(1)).findAll();
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any());
    }

    // =========================================================
    // ✅ TEST: Sincronización vacía
    // =========================================================
    @Test
    @DisplayName("No debe enviar mensajes si no hay prestadores")
    void testSincronizarPrestadores_Vacio() {
        when(prestadorRepository.findAll()).thenReturn(List.of());

        prestadorSyncService.sincronizarPrestadores();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any());
    }

    // =========================================================
    // ✅ TEST: Sincronizar por ID
    // =========================================================
    @Test
    @DisplayName("Debe sincronizar un prestador específico por ID")
    void testSincronizarPorId_Success() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any());

        prestadorSyncService.sincronizarPrestador(1L);

        verify(prestadorRepository).findById(1L);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el prestador no existe al sincronizar por ID")
    void testSincronizarPorId_NotFound() {
        when(prestadorRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> prestadorSyncService.sincronizarPrestador(99L));

        assertTrue(ex.getMessage().contains("Prestador no encontrado"));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any());
    }

    // =========================================================
    // ✅ TEST: Publicación hacia MatchingPublisherService
    // =========================================================
    @Test
    @DisplayName("Debe publicar correctamente al servicio de matching")
    void testPublicarAlMatchingService_Success() {
        when(prestadorRepository.findAll()).thenReturn(List.of(prestador));
        doNothing().when(matchingPublisherService).enviarPrestador(any(Prestador.class));

        prestadorSyncService.publicarAlMatchingService();

        verify(matchingPublisherService, times(1)).enviarPrestador(prestador);
    }

    @Test
    @DisplayName("Debe ignorar la publicación si no hay prestadores")
    void testPublicarAlMatchingService_SinPrestadores() {
        when(prestadorRepository.findAll()).thenReturn(List.of());

        prestadorSyncService.publicarAlMatchingService();

        verify(matchingPublisherService, never()).enviarPrestador(any());
    }

    // =========================================================
    // ✅ TEST: Manejo de excepciones en RabbitMQ
    // =========================================================
    @Test
    @DisplayName("Debe manejar excepciones al enviar mensajes RabbitMQ")
    void testManejoExcepcionRabbitMQ() {
        when(prestadorRepository.findAll()).thenReturn(List.of(prestador));
        doThrow(new RuntimeException("Error RabbitMQ")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any());

        assertDoesNotThrow(() -> prestadorSyncService.sincronizarPrestadores());
    }
}
