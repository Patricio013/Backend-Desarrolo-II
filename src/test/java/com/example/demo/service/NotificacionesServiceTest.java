package com.example.demo.service;

import com.example.demo.entity.Notificaciones;
import com.example.demo.repository.NotificacionesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionesServiceTest {

    @Mock
    private NotificacionesRepository notificacionesRepository;

    @InjectMocks
    private NotificacionesService notificacionesService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Debe crear una notificación correctamente")
    void testCrearNotificacion_Success() {
        Notificaciones notificacion = Notificaciones.builder().id(1L).titulo("Test").build();
        when(notificacionesRepository.save(any(Notificaciones.class))).thenReturn(notificacion);

        Notificaciones result = notificacionesService.crearNotificacion(notificacion);

        assertNotNull(result);
        assertEquals("Test", result.getTitulo());
        verify(notificacionesRepository, times(1)).save(notificacion);
    }

    @Test
    @DisplayName("Debe notificar una invitación a cotización correctamente")
    void testNotificarInvitacionCotizacion_Success() {
        Long cotizacionId = 123L;
        String titulo = "Nueva Invitación";
        String mensaje = "Has sido invitado a cotizar.";

        ArgumentCaptor<Notificaciones> captor = ArgumentCaptor.forClass(Notificaciones.class);
        when(notificacionesRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Notificaciones result = notificacionesService.notificarInvitacionCotizacion(cotizacionId, titulo, mensaje);

        assertNotNull(result);
        Notificaciones captured = captor.getValue();
        assertEquals(cotizacionId, captured.getCotizacionId());
        assertEquals(titulo, captured.getTitulo());
        assertEquals(mensaje, captured.getMensaje());
        assertFalse(captured.isLeida());
    }

    @Test
    @DisplayName("Debe marcar una notificación como leída si existe")
    void testMarcarComoLeida_Found() {
        Long notificacionId = 1L;
        Notificaciones notificacion = Notificaciones.builder().id(notificacionId).leida(false).build();

        when(notificacionesRepository.findById(notificacionId)).thenReturn(Optional.of(notificacion));

        Optional<Notificaciones> result = notificacionesService.marcarComoLeida(notificacionId);

        assertTrue(result.isPresent());
        assertTrue(result.get().isLeida());
        verify(notificacionesRepository, times(1)).findById(notificacionId);
        verify(notificacionesRepository, times(1)).save(notificacion);
    }

    @Test
    @DisplayName("No debe hacer nada al marcar como leída si no existe")
    void testMarcarComoLeida_NotFound() {
        Long notificacionId = 99L;
        when(notificacionesRepository.findById(notificacionId)).thenReturn(Optional.empty());

        Optional<Notificaciones> result = notificacionesService.marcarComoLeida(notificacionId);

        assertFalse(result.isPresent());
        verify(notificacionesRepository, times(1)).findById(notificacionId);
        verify(notificacionesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe eliminar una notificación por su ID")
    void testEliminarNotificacion() {
        Long notificacionId = 1L;
        doNothing().when(notificacionesRepository).deleteById(notificacionId);

        notificacionesService.eliminarNotificacion(notificacionId);

        verify(notificacionesRepository, times(1)).deleteById(notificacionId);
    }

    @Test
    @DisplayName("Debe devolver una lista de notificaciones pendientes")
    void testPendientes() {
        List<Notificaciones> pendientes = List.of(
                Notificaciones.builder().id(1L).leida(false).build(),
                Notificaciones.builder().id(2L).leida(false).build()
        );
        when(notificacionesRepository.findTop100ByLeidaFalseOrderByFechaAsc()).thenReturn(pendientes);

        List<Notificaciones> result = notificacionesService.pendientes();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(notificacionesRepository, times(1)).findTop100ByLeidaFalseOrderByFechaAsc();
    }
}