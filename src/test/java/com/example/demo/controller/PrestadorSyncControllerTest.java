package com.example.demo.controller;

import com.example.demo.dto.PrestadorDTO;
import com.example.demo.dto.ModuleResponse;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.PrestadorSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PrestadorSyncControllerTest {

    @Mock
    private PrestadorSyncService prestadorSyncService;

    @Mock
    private ModuleResponseFactory responseFactory;

    @InjectMocks
    private PrestadorSyncController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void obtenerPrestadores_deberiaRetornarListaOk() {
        // Arrange
        PrestadorDTO prestador1 = new PrestadorDTO();
        prestador1.setId(1L);
        prestador1.setNombre("Juan Pérez");

        PrestadorDTO prestador2 = new PrestadorDTO();
        prestador2.setId(2L);
        prestador2.setNombre("María López");

        when(prestadorSyncService.obtenerPrestadores()).thenReturn(List.of(prestador1, prestador2));

        // Act
        ResponseEntity<List<PrestadorDTO>> response = controller.obtenerPrestadores();

        // Assert
        verify(prestadorSyncService).obtenerPrestadores();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().get(0).getNombre()).isEqualTo("Juan Pérez");
    }

    @Test
    void sincronizarPrestadores_deberiaInvocarServicioYRetornarOk() {
        // Arrange
        ModuleResponse okResponse = new ModuleResponse("OK", true);
        when(prestadorSyncService.sincronizarPrestadores()).thenReturn(okResponse);

        // Act
        ResponseEntity<ModuleResponse> response = controller.sincronizarPrestadores();

        // Assert
        verify(prestadorSyncService).sincronizarPrestadores();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("OK");
    }

    @Test
    void sincronizarPrestadores_deberiaManejarExcepcionYRetornarError() {
        // Arrange
        when(prestadorSyncService.sincronizarPrestadores()).thenThrow(new RuntimeException("Error de conexión"));

        // Act
        ResponseEntity<ModuleResponse> response = controller.sincronizarPrestadores();

        // Assert
        verify(prestadorSyncService).sincronizarPrestadores();
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody().getMessage()).contains("Error");
    }

    @Test
    void obtenerPrestadorPorId_devuelvePrestadorSiExiste() {
        // Arrange
        Long idPrestador = 5L;
        PrestadorDTO dto = new PrestadorDTO();
        dto.setId(idPrestador);
        dto.setNombre("Pedro Sync");

        when(prestadorSyncService.obtenerPrestadorPorId(idPrestador)).thenReturn(dto);

        // Act
        ResponseEntity<PrestadorDTO> response = controller.obtenerPrestadorPorId(idPrestador);

        // Assert
        verify(prestadorSyncService).obtenerPrestadorPorId(idPrestador);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getNombre()).isEqualTo("Pedro Sync");
    }

    @Test
    void obtenerPrestadorPorId_devuelveNotFoundSiNoExiste() {
        // Arrange
        Long idPrestador = 9L;
        when(prestadorSyncService.obtenerPrestadorPorId(idPrestador)).thenReturn(null);

        // Act
        ResponseEntity<PrestadorDTO> response = controller.obtenerPrestadorPorId(idPrestador);

        // Assert
        verify(prestadorSyncService).obtenerPrestadorPorId(idPrestador);
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
    }
}
