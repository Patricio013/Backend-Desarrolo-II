package com.example.demo.service;

import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.entity.Solicitud;
import com.example.demo.repository.SolicitudRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.CotizacionRepository;
import com.example.demo.repository.NotificacionesRepository;
import com.example.demo.entity.Usuario;
import com.example.demo.dto.ModuleResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests para CalificacionService.
 * Cubre casos exitosos, fallidos y excepciones simuladas.
 */
class CalificacionServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private NotificacionesRepository notificacionesRepository;

    @InjectMocks
    private CalificacionService calificacionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ======================================================
    // ✅ TESTS - Registro de calificación
    // ======================================================

    @Test
    @DisplayName("Debería registrar una calificación correctamente")
    void testRegistrarCalificacion_Success() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO(1L, 1L, 10L, 5, "Excelente servicio");

        Solicitud solicitud = new Solicitud();
        solicitud.setId(1L);

        Usuario usuario = new Usuario();
        usuario.setId(10L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(solicitudRepository.save(any(Solicitud.class))).thenReturn(solicitud);

        ModuleResponse<String> result = calificacionService.appendBatchItem(dto);

        assertNotNull(result);
        assertEquals("calificaciones", result.module());
        assertTrue(result.message().contains("procesada"));
        verify(solicitudRepository, times(1)).save(any(Solicitud.class));
    }

    @Test
    @DisplayName("Debería fallar si la solicitud no existe")
    void testRegistrarCalificacion_SolicitudNoEncontrada() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO(1L, 99L, 10L, 5, "test");

        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                calificacionService.appendBatchItem(dto)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("solicitud"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería fallar si el usuario no existe")
    void testRegistrarCalificacion_UsuarioNoEncontrado() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO(1L, 1L, 999L, 5, "test");
        Solicitud solicitud = new Solicitud();
        solicitud.setId(1L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                calificacionService.appendBatchItem(dto)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("usuario"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería manejar error de base de datos al guardar")
    void testRegistrarCalificacion_ErrorAlGuardar() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO(1L, 1L, 10L, 5, "test");

        Solicitud solicitud = new Solicitud();
        solicitud.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(10L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(solicitudRepository.save(any(Solicitud.class))).thenThrow(new RuntimeException("DB error"));

        Exception ex = assertThrows(RuntimeException.class, () ->
                calificacionService.appendBatchItem(dto)
        );

        assertTrue(ex.getMessage().contains("DB error"));
    }

    // ======================================================
    // ✅ TESTS - Validaciones del DTO
    // ======================================================

    @Test
    @DisplayName("Debería lanzar error si el DTO es nulo")
    void testRegistrarCalificacion_NullDto() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                calificacionService.appendBatchItem(null)
        );
        assertTrue(ex.getMessage().contains("DTO"));
        verifyNoInteractions(solicitudRepository, usuarioRepository);
    }

    @Test
    @DisplayName("Debería lanzar error si el puntaje es inválido")
    void testRegistrarCalificacion_PuntajeInvalido() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO(1L, 1L, 10L, 7, "test"); // puntaje fuera de rango 1-5

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                calificacionService.appendBatchItem(dto)
        );

        assertTrue(ex.getMessage().contains("puntaje"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería aceptar comentarios vacíos pero válidos")
    void testRegistrarCalificacion_ComentarioVacio() {
        RecibirCalificacionesDTO dto = new RecibirCalificacionesDTO(1L, 1L, 10L, 3, "");

        Solicitud solicitud = new Solicitud();
        solicitud.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(10L);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        ModuleResponse<String> result = calificacionService.appendBatchItem(dto);

        assertNotNull(result);
        assertEquals("calificaciones", result.module());
        verify(solicitudRepository, times(1)).save(any(Solicitud.class));
    }

    // ======================================================
    // ✅ TESTS - Consultas auxiliares
    // ======================================================

    @Test
    @DisplayName("Debería retornar lista vacía si no hay calificaciones")
    void testObtenerCalificaciones_Vacia() {
        Solicitud solicitud = new Solicitud();
        solicitud.setId(2L);

        when(solicitudRepository.findById(2L)).thenReturn(Optional.of(solicitud));

        var result = calificacionService.obtenerCalificacionesPorSolicitud(2L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Debería lanzar excepción si la solicitud no existe al buscar calificaciones")
    void testObtenerCalificaciones_SolicitudNoExiste() {
        when(solicitudRepository.findById(5L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () ->
                calificacionService.obtenerCalificacionesPorSolicitud(5L)
        );

        assertTrue(ex.getMessage().contains("solicitud"));
    }
}
