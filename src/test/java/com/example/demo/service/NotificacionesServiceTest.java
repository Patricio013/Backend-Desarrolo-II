package com.example.demo.service;

import com.example.demo.model.Notificaciones;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacionesRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.dto.ModuleResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para NotificacionesService.
 * Cubre envío, lectura, eliminación y validaciones.
 */
class NotificacionesServiceTest {

    @Mock
    private NotificacionesRepository notificacionesRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private NotificacionesService notificacionesService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ======================================================
    // ✅ TESTS - Envío de notificaciones
    // ======================================================

    @Test
    @DisplayName("Debe enviar una notificación correctamente a un usuario existente")
    void testEnviarNotificacion_Success() {
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);

        Notificaciones noti = new Notificaciones();
        noti.setId(100L);
        noti.setTitulo("Prueba");
        noti.setDescripcion("Mensaje de prueba");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(notificacionesRepository.save(any(Notificaciones.class))).thenReturn(noti);

        ModuleResponse result = notificacionesService.enviarNotificacion(1L, "Prueba", "Mensaje de prueba");

        assertNotNull(result);
        assertEquals("OK", result.getStatus());
        assertTrue(result.getMessage().contains("enviada"));
        verify(notificacionesRepository, times(1)).save(any(Notificaciones.class));
    }

    @Test
    @DisplayName("Debe fallar al enviar notificación a usuario inexistente")
    void testEnviarNotificacion_UsuarioNoExiste() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                notificacionesService.enviarNotificacion(999L, "Título", "Mensaje")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("usuario"));
        verify(notificacionesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el título o descripción son nulos o vacíos")
    void testEnviarNotificacion_DatosInvalidos() {
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class, () ->
                notificacionesService.enviarNotificacion(1L, "", "mensaje"));

        assertThrows(IllegalArgumentException.class, () ->
                notificacionesService.enviarNotificacion(1L, "titulo", null));

        verify(notificacionesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe manejar error del repositorio al guardar la notificación")
    void testEnviarNotificacion_ErrorRepositorio() {
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(notificacionesRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                notificacionesService.enviarNotificacion(1L, "Título", "Mensaje"));

        assertTrue(ex.getMessage().contains("DB error"));
    }

    // ======================================================
    // ✅ TESTS - Obtener notificaciones
    // ======================================================

    @Test
    @DisplayName("Debe obtener todas las notificaciones de un usuario")
    void testObtenerNotificacionesPorUsuario() {
        Notificaciones n1 = new Notificaciones();
        n1.setId(1L);
        n1.setTitulo("T1");
        n1.setDescripcion("Mensaje 1");

        Notificaciones n2 = new Notificaciones();
        n2.setId(2L);
        n2.setTitulo("T2");
        n2.setDescripcion("Mensaje 2");

        List<Notificaciones> lista = Arrays.asList(n1, n2);
        when(notificacionesRepository.findByUsuarioId(1L)).thenReturn(lista);

        List<Notificaciones> result = notificacionesService.obtenerNotificacionesPorUsuario(1L);

        assertEquals(2, result.size());
        assertEquals("T1", result.get(0).getTitulo());
        verify(notificacionesRepository, times(1)).findByUsuarioId(1L);
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay notificaciones")
    void testObtenerNotificaciones_Vacia() {
        when(notificacionesRepository.findByUsuarioId(5L)).thenReturn(Collections.emptyList());

        List<Notificaciones> result = notificacionesService.obtenerNotificacionesPorUsuario(5L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(notificacionesRepository, times(1)).findByUsuarioId(5L);
    }

    // ======================================================
    // ✅ TESTS - Marcar como leída
    // ======================================================

    @Test
    @DisplayName("Debe marcar notificación como leída correctamente")
    void testMarcarNotificacionLeida_Success() {
        Notificaciones noti = new Notificaciones();
        noti.setId(1L);
        noti.setLeido(false);

        when(notificacionesRepository.findById(1L)).thenReturn(Optional.of(noti));
        when(notificacionesRepository.save(any())).thenReturn(noti);

        ModuleResponse result = notificacionesService.marcarComoLeida(1L);

        assertEquals("OK", result.getStatus());
        assertTrue(noti.isLeido());
        verify(notificacionesRepository, times(1)).save(noti);
    }

    @Test
    @DisplayName("Debe lanzar excepción si la notificación no existe")
    void testMarcarNotificacionLeida_NoExiste() {
        when(notificacionesRepository.findById(10L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                notificacionesService.marcarComoLeida(10L)
        );

        assertTrue(ex.getMessage().contains("notificación"));
        verify(notificacionesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe manejar error del repositorio al actualizar notificación")
    void testMarcarNotificacionLeida_ErrorDB() {
        Notificaciones noti = new Notificaciones();
        noti.setId(1L);
        noti.setLeido(false);

        when(notificacionesRepository.findById(1L)).thenReturn(Optional.of(noti));
        when(notificacionesRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        Exception ex = assertThrows(RuntimeException.class, () ->
                notificacionesService.marcarComoLeida(1L)
        );

        assertTrue(ex.getMessage().contains("DB error"));
    }

    // ======================================================
    // ✅ TESTS - Eliminación
    // ======================================================

    @Test
    @DisplayName("Debe eliminar una notificación correctamente")
    void testEliminarNotificacion_Success() {
        Notificaciones noti = new Notificaciones();
        noti.setId(1L);

        when(notificacionesRepository.existsById(1L)).thenReturn(true);
        doNothing().when(notificacionesRepository).deleteById(1L);

        ModuleResponse result = notificacionesService.eliminarNotificacion(1L);

        assertEquals("OK", result.getStatus());
        verify(notificacionesRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Debe fallar al eliminar si no existe la notificación")
    void testEliminarNotificacion_NoExiste() {
        when(notificacionesRepository.existsById(10L)).thenReturn(false);

        Exception ex = assertThrows(RuntimeException.class, () ->
                notificacionesService.eliminarNotificacion(10L)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("no encontrada"));
        verify(notificacionesRepository, never()).deleteById(anyLong());
    }
}
