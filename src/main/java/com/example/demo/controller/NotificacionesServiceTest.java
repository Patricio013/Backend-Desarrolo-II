package com.example.demo.service;

import com.example.demo.entity.Notificaciones;
import com.example.demo.repository.NotificacionesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
                .cotizacionId(100L)
                .titulo("Test")
                .mensaje("Mensaje de test")
                .leida(false)
                .fecha(LocalDateTime.now())
                .build();
    }

    @Test
    void crearNotificacion_debeGuardarYRetornar() {
        when(notificacionesRepository.save(any(Notificaciones.class))).thenReturn(notificacion);

        Notificaciones creada = notificacionesService.crearNotificacion(new Notificaciones());

        assertNotNull(creada);
        assertEquals(notificacion.getId(), creada.getId());
        verify(notificacionesRepository).save(any(Notificaciones.class));
    }

    @Test
    void notificarInvitacionCotizacion_debeGuardarYRetornar() {
        when(notificacionesRepository.save(any(Notificaciones.class))).thenReturn(notificacion);

        Notificaciones notif = notificacionesService.notificarInvitacionCotizacion(100L, "Test", "Mensaje");

        assertNotNull(notif);
        verify(notificacionesRepository).save(any(Notificaciones.class));
    }

    @Test
    void marcarComoLeida_cuandoExiste_debeActualizarYRetornar() {
        when(notificacionesRepository.findById(1L)).thenReturn(Optional.of(notificacion));
        when(notificacionesRepository.save(any(Notificaciones.class))).thenReturn(notificacion);

        Optional<Notificaciones> actualizada = notificacionesService.marcarComoLeida(1L);

        assertTrue(actualizada.isPresent());
        assertTrue(actualizada.get().isLeida());
        verify(notificacionesRepository).findById(1L);
        verify(notificacionesRepository).save(notificacion);
    }

    @Test
    void marcarComoLeida_cuandoNoExiste_debeRetornarVacio() {
        when(notificacionesRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Notificaciones> resultado = notificacionesService.marcarComoLeida(99L);

        assertFalse(resultado.isPresent());
        verify(notificacionesRepository).findById(99L);
        verify(notificacionesRepository, never()).save(any());
    }

    @Test
    void eliminarNotificacion_debeLlamarDeleteById() {
        doNothing().when(notificacionesRepository).deleteById(1L);

        notificacionesService.eliminarNotificacion(1L);

        verify(notificacionesRepository).deleteById(1L);
    }

    @Test
    void pendientes_debeRetornarListaDeNoLeidas() {
        when(notificacionesRepository.findTop100ByLeidaFalseOrderByFechaAsc()).thenReturn(List.of(notificacion));

        List<Notificaciones> pendientes = notificacionesService.pendientes();

        assertFalse(pendientes.isEmpty());
        assertEquals(1, pendientes.size());
        assertFalse(pendientes.get(0).isLeida());
        verify(notificacionesRepository).findTop100ByLeidaFalseOrderByFechaAsc();
    }
}