package com.example.demo.service;

import com.example.demo.entity.Prestador;
import com.example.demo.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PrestadorSyncService
 * Cubre sincronización de datos desde servicio externo (mockeado)
 */
class PrestadorSyncServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PrestadorSyncService prestadorSyncService;

    private Prestador prestador;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        prestador = new Prestador();
        prestador.setId(1L);
        prestador.setNombre("Carlos Perez");
        prestador.setEmail("carlos@example.com");
    }

    // =========================================================
    // ✅ SINCRONIZACIÓN EXITOSA
    // =========================================================
    @Test
    @DisplayName("Debe sincronizar correctamente un prestador existente")
    void testSyncPrestador_Success() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));

        // Mock respuesta externa (simulada)
        Prestador apiResponse = new Prestador();
        apiResponse.setNombre("Carlos P.");
        apiResponse.setEmail("carlos.new@example.com");

        when(restTemplate.getForObject(anyString(), eq(Prestador.class))).thenReturn(apiResponse);
        when(prestadorRepository.save(any(Prestador.class))).thenAnswer(inv -> inv.getArgument(0));

        Prestador result = prestadorSyncService.syncPrestador(1L);

        assertNotNull(result);
        assertEquals("carlos.new@example.com", result.getEmail());
        verify(prestadorRepository).save(any());
    }

    // =========================================================
    // ⚠️ PRESTADOR NO ENCONTRADO
    // =========================================================
    @Test
    @DisplayName("Debe lanzar excepción si el prestador no existe en la BD")
    void testSyncPrestador_NoExiste() {
        when(prestadorRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> prestadorSyncService.syncPrestador(99L));

        assertTrue(ex.getMessage().contains("no encontrado"));
        verify(prestadorRepository, never()).save(any());
    }

    // =========================================================
    // ❌ ERROR DE SERVICIO EXTERNO
    // =========================================================
    @Test
    @DisplayName("Debe manejar error del servicio externo")
    void testSyncPrestador_ErrorExterno() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));
        when(restTemplate.getForObject(anyString(), eq(Prestador.class)))
                .thenThrow(new RuntimeException("API no disponible"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestadorSyncService.syncPrestador(1L));

        assertTrue(ex.getMessage().contains("API no disponible"));
        verify(prestadorRepository, never()).save(any());
    }

    // =========================================================
    // 💾 VERIFICAR QUE GUARDA CAMBIOS
    // =========================================================
    @Test
    @DisplayName("Debe guardar los cambios del prestador sincronizado")
    void testSyncPrestador_GuardaCambios() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));

        Prestador actualizado = new Prestador();
        actualizado.setNombre("Carlos Actualizado");
        actualizado.setEmail("nuevo@mail.com");

        when(restTemplate.getForObject(anyString(), eq(Prestador.class))).thenReturn(actualizado);

        prestadorSyncService.syncPrestador(1L);

        verify(prestadorRepository, times(1)).save(any(Prestador.class));
    }
}
