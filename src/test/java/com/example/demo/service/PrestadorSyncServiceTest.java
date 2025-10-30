package com.example.demo.service;

import com.example.demo.entity.Prestador;
import com.example.demo.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
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
    @DisplayName("Debe guardar y publicar un prestador correctamente")
    void testSaveAndSync_Success() {
        when(prestadorRepository.save(any(Prestador.class))).thenReturn(prestador);

        prestadorSyncService.saveAndSync(prestador);

        verify(prestadorRepository, times(1)).save(prestador);
        verify(matchingPublisherService, times(1)).publicarPrestador(prestador);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el prestador es nulo")
    void testSaveAndSync_NullInput() {
        assertThrows(ResponseStatusException.class, () -> prestadorSyncService.saveAndSync(null));

        verify(prestadorRepository, never()).save(any());
        verify(matchingPublisherService, never()).publicarPrestador(any());
    }

    @Test
    @DisplayName("No debe publicar si falla el guardado en repositorio")
    void testSaveAndSync_RepositoryError() {
        when(prestadorRepository.save(any(Prestador.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> prestadorSyncService.saveAndSync(prestador));

        verify(prestadorRepository, times(1)).save(prestador);
        verify(matchingPublisherService, never()).publicarPrestador(any());
    }

    @Test
    @DisplayName("Debe guardar aunque falle la publicación en RabbitMQ")
    void testSaveAndSync_PublisherError() {
        when(prestadorRepository.save(any(Prestador.class))).thenReturn(prestador);
        doThrow(new RuntimeException("RabbitMQ error")).when(matchingPublisherService).publicarPrestador(any(Prestador.class));

        prestadorSyncService.saveAndSync(prestador);

        verify(prestadorRepository, times(1)).save(prestador);
        verify(matchingPublisherService, times(1)).publicarPrestador(prestador);
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

        try {
            prestadorSyncService.sincronizar();
        } catch (Exception e) {
            fail("El método no debería propagar la excepción de publicación: " + e.getMessage());
        }

        verify(prestadorRepository, times(1)).findAll();
        verify(matchingPublisherService, times(1)).publicarPrestadores(anyList());
    }
}
