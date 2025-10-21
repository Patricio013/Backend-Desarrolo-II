package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.CalificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CalificacionControllerTest {

    @Mock
    private CalificacionService calificacionService;

    @Mock
    private ModuleResponseFactory responseFactory;

    @InjectMocks
    private CalificacionController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void agregarBatch_todosExitosos_devuelveOk() {
        // Arrange
        RecibirCalificacionesDTO dto1 = new RecibirCalificacionesDTO();
        dto1.setId(1L);
        RecibirCalificacionesDTO dto2 = new RecibirCalificacionesDTO();
        dto2.setId(2L);

        List<RecibirCalificacionesDTO> lista = List.of(dto1, dto2);

        ModuleResponse<String> mockResponse =
                new ModuleResponse<>("calificaciones", "calificacionesBatchProcesadas", "ok");
        when(responseFactory.build(any(), any(), any())).thenReturn(mockResponse);

        // Act
        ResponseEntity<ModuleResponse<String>> response = controller.agregarBatch(lista);

        // Assert
        verify(calificacionService, times(2)).appendBatchItem(any());
        assertThat(response.getBody()).isEqualTo(mockResponse);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void agregarBatch_conAlgunosFallidos_noLanzaExcepcion() {
        // Arrange
        RecibirCalificacionesDTO ok = new RecibirCalificacionesDTO();
        ok.setId(1L);
        RecibirCalificacionesDTO fail = new RecibirCalificacionesDTO();
        fail.setId(2L);

        List<RecibirCalificacionesDTO> lista = List.of(ok, fail);

        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Error"))
                .when(calificacionService).appendBatchItem(fail);

        when(responseFactory.build(any(), any(), any()))
                .thenReturn(new ModuleResponse<>("calificaciones", "calificacionesBatchProcesadas", "ok"));

        // Act
        ResponseEntity<ModuleResponse<String>> response = controller.agregarBatch(lista);

        // Assert
        verify(calificacionService, times(2)).appendBatchItem(any());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void agregarBatch_conExcepcionGeneral_noInterrumpeElLoop() {
        // Arrange
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO();
        dto.setId(99L);

        doThrow(new RuntimeException("Error inesperado"))
                .when(calificacionService).appendBatchItem(any());

        when(responseFactory.build(any(), any(), any()))
                .thenReturn(new ModuleResponse<>("calificaciones", "calificacionesBatchProcesadas", "ok"));

        // Act
        ResponseEntity<ModuleResponse<String>> response = controller.agregarBatch(List.of(dto));

        // Assert
        verify(calificacionService, times(1)).appendBatchItem(dto);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
