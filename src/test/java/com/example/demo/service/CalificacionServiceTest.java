package com.example.demo.service;

import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.entity.Prestador;
import com.example.demo.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalificacionServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @InjectMocks
    private CalificacionService calificacionService;

    private Prestador prestador;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        prestador = new Prestador();
        prestador.setId(1L);
        prestador.setCalificacion(new ArrayList<>());
        prestador.setTrabajosFinalizados(3);
    }

    @Test
    @DisplayName("Debe agregar calificaciones válidas correctamente")
    void testAppendBatchItem_Success() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO();
        dto.setId(1L);
        dto.setPuntuaciones(List.of((short) 4, (short) 5));

        when(prestadorRepository.findByExternalId(1L)).thenReturn(Optional.of(prestador));

        calificacionService.appendBatchItem(dto);

        assertEquals(2, prestador.getCalificacion().size());
        verify(prestadorRepository, times(1)).save(prestador);
    }

    @Test
    @DisplayName("Debe lanzar error si el DTO es nulo o sin ID")
    void testAppendBatchItem_NullItemOrId() {
        assertThrows(ResponseStatusException.class, () -> calificacionService.appendBatchItem(null));

        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO();
        dto.setId(null);
        dto.setPuntuaciones(List.of((short) 5));

        assertThrows(ResponseStatusException.class, () -> calificacionService.appendBatchItem(dto));
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar error si las puntuaciones son vacías")
    void testAppendBatchItem_EmptyScores() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO();
        dto.setId(1L);
        dto.setPuntuaciones(new ArrayList<>());

        assertThrows(ResponseStatusException.class, () -> calificacionService.appendBatchItem(dto));
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar error si el prestador no existe")
    void testAppendBatchItem_PrestadorNoExiste() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO();
        dto.setId(999L);
        dto.setPuntuaciones(List.of((short) 5));

        when(prestadorRepository.findByExternalId(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> calificacionService.appendBatchItem(dto));
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar error si se exceden los trabajos finalizados")
    void testAppendBatchItem_ExcedeTrabajosFinalizados() {
        prestador.setTrabajosFinalizados(2);

        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO();
        dto.setId(1L);
        dto.setPuntuaciones(List.of((short) 4, (short) 5, (short) 3));

        when(prestadorRepository.findByExternalId(1L)).thenReturn(Optional.of(prestador));

        assertThrows(ResponseStatusException.class, () -> calificacionService.appendBatchItem(dto));
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar error si hay puntuaciones fuera de rango")
    void testAppendBatchItem_PuntuacionFueraDeRango() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO();
        dto.setId(1L);
        dto.setPuntuaciones(List.of((short) 6));

        when(prestadorRepository.findByExternalId(1L)).thenReturn(Optional.of(prestador));

        assertThrows(ResponseStatusException.class, () -> calificacionService.appendBatchItem(dto));
        verify(prestadorRepository, never()).save(any());
    }
}
