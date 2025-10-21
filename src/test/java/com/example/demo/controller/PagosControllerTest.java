package com.example.demo.controller;

import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.SolicitudPagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PagosControllerTest {

    @Mock
    private SolicitudPagoService solicitudPagoService;

    @Mock
    private ModuleResponseFactory responseFactory;

    @InjectMocks
    private PagosController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void crearSolicitudPago_deberiaRetornarOk() {
        // Arrange
        SolicitudPagoCreateDTO dto = new SolicitudPagoCreateDTO();
        dto.setIdSolicitud(1L);
        dto.setMonto(1500.0);

        SolicitudPagoDTO mockResponse = new SolicitudPagoDTO();
        mockResponse.setId(10L);

        when(solicitudPagoService.crearSolicitudPago(dto)).thenReturn(mockResponse);

        // Act
        ResponseEntity<SolicitudPagoDTO> response = controller.crearSolicitudPago(dto);

        // Assert
        verify(solicitudPagoService).crearSolicitudPago(dto);
        assertThat(response.getBody()).isEqualTo(mockResponse);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void listarPagosPorPrestador_devuelveListaPagos() {
        // Arrange
        Long idPrestador = 7L;

        SolicitudPagoDTO pago1 = new SolicitudPagoDTO();
        pago1.setId(1L);
        SolicitudPagoDTO pago2 = new SolicitudPagoDTO();
        pago2.setId(2L);

        when(solicitudPagoService.listarPagosPorPrestador(idPrestador))
                .thenReturn(List.of(pago1, pago2));

        // Act
        ResponseEntity<List<SolicitudPagoDTO>> response = controller.listarPagosPorPrestador(idPrestador);

        // Assert
        verify(solicitudPagoService).listarPagosPorPrestador(idPrestador);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void confirmarPago_deberiaRetornarOk() {
        // Arrange
        Long idPago = 5L;
        when(solicitudPagoService.confirmarPago(idPago)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = controller.confirmarPago(idPago);

        // Assert
        verify(solicitudPagoService).confirmarPago(idPago);
        assertThat(response.getBody()).isTrue();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void confirmarPago_devuelveFalseCuandoFalla() {
        // Arrange
        Long idPago = 9L;
        when(solicitudPagoService.confirmarPago(idPago)).thenReturn(false);

        // Act
        ResponseEntity<Boolean> response = controller.confirmarPago(idPago);

        // Assert
        verify(solicitudPagoService).confirmarPago(idPago);
        assertThat(response.getBody()).isFalse();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void eliminarPago_deberiaInvocarServiceYRetornarOk() {
        // Arrange
        Long idPago = 15L;
        doNothing().when(solicitudPagoService).eliminarPago(idPago);

        // Act
        ResponseEntity<Void> response = controller.eliminarPago(idPago);

        // Assert
        verify(solicitudPagoService).eliminarPago(idPago);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
