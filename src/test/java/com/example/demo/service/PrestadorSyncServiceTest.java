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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ✅ Unit test de PrestadorSyncService
 * Simula el flujo RabbitMQ con MatchingPublisherService mockeado.
 */
class PrestadorSyncServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private MatchingPublisherService matchingPublisherService;

    @Mock
    private RabbitTemplate rabbitTemplate; // simulado por si MatchingPublisher usa internamente Rabbit

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
    }

    // =========================================================
    // ✅ TEST: sincronización de un prestador
    // =========================================================
    @Test
    @DisplayName("Debe publicar datos de prestador en RabbitMQ correctamente")
    void testSyncPrestador_Success() {
        when(prestadorRepository.findAll()).thenReturn(List.of(prestador));

        prestadorSyncService.syncPrestadores();

        verify(prestadorRepository, times(1)).findAll();
        verify(matchingPublisherService, times(1))
                .publicarPrestador(any(Prestador.class));
    }

    // =========================================================
    // ✅ TEST: cuando no hay prestadores en base
    // =========================================================
    @Test
    @DisplayName("No debe publicar si no hay prestadores disponibles")
    void testSyncPrestador_EmptyList() {
        when(prestadorRepository.findAll()).thenReturn(List.of());

        prestadorSyncService.syncPrestadores();

        verify(prestadorRepository, times(1)).findAll();
        verify(matchingPublisherService, never())
                .publicarPrestador(any());
    }

    // =========================================================
    // ✅ TEST: error controlado al publicar
    // =========================================================
    @Test
    @DisplayName("Debe manejar errores al intentar publicar un prestador")
    void testSyncPrestador_ErrorPublicacion() {
        when(prestadorRepository.findAll()).thenReturn(List.of(prestador));
        doThrow(new RuntimeException("Fallo RabbitMQ"))
                .when(matchingPublisherService)
                .publicarPrestador(any(Prestador.class));

        prestadorSyncService.syncPrestadores();

        verify(prestadorRepository, times(1)).findAll();
        verify(matchingPublisherService, times(1))
                .publicarPrestador(prestador);
    }
}
