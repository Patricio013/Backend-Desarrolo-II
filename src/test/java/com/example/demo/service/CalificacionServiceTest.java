package com.example.demo.service;

import com.example.demo.entity.Calificacion;
import com.example.demo.entity.Solicitud;
import com.example.demo.repository.CalificacionRepository;
import com.example.demo.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalificacionServiceTest {

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @InjectMocks
    private CalificacionService calificacionService;

    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        solicitud = new Solicitud();
        solicitud.setId(1L);
    }

    @Test
    @DisplayName("✅ Debe obtener calificaciones por ID de solicitud")
    void testObtenerCalificacionesPorSolicitud() {
        Calificacion c1 = new Calificacion();
        c1.setId(1L);
        c1.setPuntaje(4);
        c1.setComentario("Muy bueno");

        solicitud.setCalificaciones(List.of(c1));
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        List<Calificacion> result = calificacionService.obtenerCalificaciones(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).getPuntaje());
        verify(solicitudRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("❌ Debe lanzar excepción si la solicitud no existe")
    void testObtenerCalificaciones_SolicitudNoExiste() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> calificacionService.obtenerCalificaciones(99L));
    }
}
