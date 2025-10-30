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

import static org.mockito.Mockito.*;

/**
 * ✅ Test unitario para PrestadorSyncService
 * Mockea RabbitMQ y MatchingPublisherService
 */
class PrestadorSyncServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private MatchingPublisherService matchingPublisherService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PrestadorSyncService prestadorSyncService;

    private Prestador prestador;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        prestador = new Prestador();
        prestador.setId(1L);
        prestador.setNombre("Juan Pérez");
        prestador.setEmail("juan@correo.com");
    }

    @Test
    @DisplayName("Debe sincronizar todos los prestadores y publicar en RabbitMQ")
    void testSincronizarPrestadores() {
        when(prestadorRepository.findAll()).thenReturn(List.of(prestador));

        // Método más probable en tu clase: sincronizar()
        prestadorSyncService.sincronizar();

        verify(prestadorRepository, times(1)).findAll();
        verify(matchingPublisherService, times(1))
                .publicarPrestadores(anyList());
    }

    @Test
    @DisplayName("Debe manejar lista vacía sin publicar")
    void testSincronizarPrestadoresVacios() {
        when(prestadorRepository.findAll()).thenReturn(List.of());

        prestadorSyncService.sincronizar();

        verify(prestadorRepository, times(1)).findAll();
        verify(matchingPublisherService, never())
                .publicarPrestadores(anyList());
    }

    @Test
    @DisplayName("Debe manejar errores al publicar en RabbitMQ sin romper el flujo")
    void testSincronizarConErrorDePublicacion() {
        when(prestadorRepository.findAll()).thenReturn(List.of(prestador));
        doThrow(new RuntimeException("Error al publicar en RabbitMQ"))
                .when(matchingPublisherService)
                .publicarPrestadores(anyList());

        prestadorSyncService.sincronizar();

        verify(prestadorRepository, times(1)).findAll();
        verify(matchingPublisherService, times(1))
                .publicarPrestadores(anyList());
    }
}
