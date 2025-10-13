package com.example.demo.service;

import com.example.demo.entity.Notificaciones;
import com.example.demo.repository.NotificacionesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionesServiceTest {

    @Mock
    private NotificacionesRepository notificacionesRepository;

    @InjectMocks
    private NotificacionesService notificacionesService;

    private Notificaciones notificacion;

    @BeforeEach
    void setUp() {
        notificacion = Notificaciones.builder()
                .id(1L)
                .cotizacionId(10L)
                .titulo("Título prueba")
                .mensaje("Mensaje prueba")
                .leida(false)
                .build();
    }

    @Test
    void crearNotificacion_deberiaGuardarYDevolverNotificacion() {
        when(notificacionesRepository.save(notificacion)).thenReturn(notificacion);

        Notificaciones resultado = notificacionesService.crearNotificacion(notificacion);

        assertNotNull(resultado);
        assertEquals("Título prueba", resultado.getTitulo());
        verify(notificacionesRepository, times(1)).save(notificacion);
    }

    @Test
    void notificarInvitacionCotizacion_deberiaCrearYGuardarNotificacion() {
        when(notificacionesRepository.save(any(Notificaciones.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notificaciones resultado = notificacionesService.notificarInvitacionCotizacion(99L, "Invitación", "Mensaje de prueba");

        assertNotNull(resultado);
        assertEquals(99L, resultado.getCotizacionId());
        assertEquals("Invitación", resultado.getTitulo());
        assertFalse(resultado.isLeida());
        verify(notificacionesRepository).save(any(Notificaciones.class));
    }

    @Test
    void marcarComoLeida_deberiaCambiarEstadoALeida() {
        Notificaciones noLeida = Notificaciones.builder()
                .id(1L)
                .titulo("T")
                .mensaje("M")
                .leida(false)
                .build();

        when(notificacionesRepository.findById(1L)).thenReturn(Optional.of(noLeida));
        when(notificacionesRepository.save(any(Notificaciones.class))).thenReturn(noLeida);

        Optional<Notificaciones> resultado = notificacionesService.marcarComoLeida(1L);

        assertTrue(resultado.isPresent());
        assertTrue(resultado.get().isLeida());
        verify(notificacionesRepository).findById(1L);
        verify(notificacionesRepository).save(noLeida);
    }

    @Test
    void marcarComoLeida_noEncuentraNotificacion_noHaceNada() {
        when(notificacionesRepository.findById(2L)).thenReturn(Optional.empty());

        Optional<Notificaciones> resultado = notificacionesService.marcarComoLeida(2L);

        assertFalse(resultado.isPresent());
        verify(notificacionesRepository, never()).save(any());
    }

    @Test
    void eliminarNotificacion_deberiaLlamarADeletePorId() {
        doNothing().when(notificacionesRepository).deleteById(1L);

        notificacionesService.eliminarNotificacion(1L);

        verify(notificacionesRepository, times(1)).deleteById(1L);
    }

    @Test
    void pendientes_deberiaDevolverListaDeNotificacionesPendientes() {
        when(notificacionesRepository.findTop100ByLeidaFalseOrderByFechaAsc()).thenReturn(List.of(notificacion));

        List<Notificaciones> pendientes = notificacionesService.pendientes();

        assertEquals(1, pendientes.size());
        assertEquals("Mensaje prueba", pendientes.get(0).getMensaje());
        verify(notificacionesRepository).findTop100ByLeidaFalseOrderByFechaAsc();
    }
}
