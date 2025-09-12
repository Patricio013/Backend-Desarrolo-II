package com.example.demo.controller;

import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.service.CalificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalificacionControllerTest {

    @Mock
    private CalificacionService calificacionService;

    @InjectMocks
    private CalificacionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void agregarBatch_ok() {
        RecibirCalificacionesDTO dto1 = new RecibirCalificacionesDTO();
        dto1.setId(1L);
        RecibirCalificacionesDTO dto2 = new RecibirCalificacionesDTO();
        dto2.setId(2L);

        doNothing().when(calificacionService).appendBatchItem(any());

        ResponseEntity<String> resp = controller.agregarBatch(List.of(dto1, dto2));

        assertEquals("ok", resp.getBody());
        verify(calificacionService, times(2)).appendBatchItem(any());
    }

    @Test
    void agregarBatch_unoFalla() {
        RecibirCalificacionesDTO dto1 = new RecibirCalificacionesDTO();
        dto1.setId(1L);
        RecibirCalificacionesDTO dto2 = new RecibirCalificacionesDTO();
        dto2.setId(2L);

        doNothing().when(calificacionService).appendBatchItem(dto1);
        doThrow(new RuntimeException("fail")).when(calificacionService).appendBatchItem(dto2);

        ResponseEntity<String> resp = controller.agregarBatch(List.of(dto1, dto2));

        assertEquals("ok", resp.getBody());
        verify(calificacionService, times(2)).appendBatchItem(any());
    }
}
