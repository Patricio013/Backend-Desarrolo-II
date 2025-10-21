package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.enums.EstadoSolicitud;
import com.example.demo.model.*;
import com.example.demo.repository.*;

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
 * Pruebas unitarias para SolicitudService
 * Cubre creación, cotizaciones, invitaciones, asignación y estados.
 */
class SolicitudServiceTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PrestadorRepository prestadorRepository;
    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private NotificacionesService notificacionesService;
    @Mock private SolicitudInvitacionRepository solicitudInvitacionRepository;

    @InjectMocks private SolicitudService solicitudService;

    private Usuario cliente;
    private Prestador prestador;
    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        cliente = new Usuario();
        cliente.setId(1L);
        cliente.setNombre("Juan");

        prestador = new Prestador();
        prestador.setId(2L);
        prestador.setNombre("Carlos");

        solicitud = new Solicitud();
        solicitud.setId(10L);
        solicitud.setDescripcion("Reparación de aire acondicionado");
        solicitud.setEstado(EstadoSolicitud.CREADA);
        solicitud.setUsuario(cliente);
    }

    // =========================================================
    // ✅ CREACIÓN DE SOLICITUD
    // =========================================================

    @Test
    @DisplayName("Debe crear una solicitud correctamente")
    void testCrearSolicitud_Success() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        SolicitudesCreadasDTO dto = solicitudService.crearSolicitud(1L, "Instalación eléctrica");

        assertNotNull(dto);
        assertEquals("Instalación eléctrica", dto.getDescripcion());
        assertEquals(EstadoSolicitud.CREADA, dto.getEstado());
        verify(solicitudRepository).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe al crear solicitud")
    void testCrearSolicitud_UsuarioNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.crearSolicitud(1L, "Instalación eléctrica"));

        assertTrue(ex.getMessage().contains("Usuario no encontrado"));
        verify(solicitudRepository, never()).save(any());
    }

    // =========================================================
    // ✅ INVITACIONES A PRESTADORES
    // =========================================================

    @Test
    @DisplayName("Debe invitar prestadores top3 correctamente")
    void testInvitarTop3_Success() {
        when(prestadorRepository.findTop3ByOrderByRatingDesc()).thenReturn(List.of(prestador));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));

        List<PrestadorDTO> result = solicitudService.invitarTop3(10L);

        assertEquals(1, result.size());
        verify(notificacionesService, times(1)).notificarPrestador(any(), anyString());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la solicitud no existe al invitar")
    void testInvitarTop3_SolicitudNoExiste() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.invitarTop3(99L));

        assertTrue(ex.getMessage().contains("Solicitud no encontrada"));
        verify(notificacionesService, never()).notificarPrestador(any(), anyString());
    }

    // =========================================================
    // ✅ COTIZACIONES
    // =========================================================

    @Test
    @DisplayName("Debe registrar una cotización correctamente")
    void testRegistrarCotizacion_Success() {
        CotizacionesSubmit dto = new CotizacionesSubmit();
        dto.setSolicitudId(10L);
        dto.setPrestadorId(2L);
        dto.setMonto(8000.0);

        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(prestadorRepository.findById(2L)).thenReturn(Optional.of(prestador));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Cotizacion result = solicitudService.registrarCotizacion(dto);

        assertEquals(8000.0, result.getMonto());
        assertEquals(prestador, result.getPrestador());
        assertEquals(solicitud, result.getSolicitud());
        verify(cotizacionRepository).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el prestador no existe al cotizar")
    void testRegistrarCotizacion_PrestadorNoExiste() {
        CotizacionesSubmit dto = new CotizacionesSubmit();
        dto.setSolicitudId(10L);
        dto.setPrestadorId(99L);
        dto.setMonto(8000.0);

        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(prestadorRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.registrarCotizacion(dto));

        assertTrue(ex.getMessage().contains("Prestador no encontrado"));
        verify(cotizacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el monto es inválido al cotizar")
    void testRegistrarCotizacion_MontoInvalido() {
        CotizacionesSubmit dto = new CotizacionesSubmit();
        dto.setSolicitudId(10L);
        dto.setPrestadorId(2L);
        dto.setMonto(-1000.0);

        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(prestadorRepository.findById(2L)).thenReturn(Optional.of(prestador));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.registrarCotizacion(dto));

        assertTrue(ex.getMessage().contains("Monto inválido"));
        verify(cotizacionRepository, never()).save(any());
    }

    // =========================================================
    // ✅ ASIGNACIÓN DE SOLICITUD
    // =========================================================

    @Test
    @DisplayName("Debe asignar correctamente la solicitud a un prestador")
    void testAsignarSolicitud_Success() {
        SolicitudAsignarDTO dto = new SolicitudAsignarDTO();
        dto.setSolicitudId(10L);
        dto.setPrestadorId(2L);

        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(prestadorRepository.findById(2L)).thenReturn(Optional.of(prestador));
        when(solicitudRepository.save(any(Solicitud.class))).thenReturn(solicitud);

        SolicitudesCreadasDTO result = solicitudService.asignarSolicitud(dto);

        assertEquals(EstadoSolicitud.ASIGNADA, result.getEstado());
        verify(solicitudRepository).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la solicitud no existe al asignar")
    void testAsignarSolicitud_SolicitudNoExiste() {
        SolicitudAsignarDTO dto = new SolicitudAsignarDTO();
        dto.setSolicitudId(99L);
        dto.setPrestadorId(2L);

        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.asignarSolicitud(dto));

        assertTrue(ex.getMessage().contains("Solicitud no encontrada"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el prestador no existe al asignar")
    void testAsignarSolicitud_PrestadorNoExiste() {
        SolicitudAsignarDTO dto = new SolicitudAsignarDTO();
        dto.setSolicitudId(10L);
        dto.setPrestadorId(99L);

        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(prestadorRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.asignarSolicitud(dto));

        assertTrue(ex.getMessage().contains("Prestador no encontrado"));
    }

    // =========================================================
    // ✅ FILTRADO Y CONSULTAS
    // =========================================================

    @Test
    @DisplayName("Debe listar todas las solicitudes del usuario")
    void testListarSolicitudesPorUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(solicitudRepository.findByUsuario(cliente)).thenReturn(List.of(solicitud));

        List<SolicitudesCreadasDTO> result = solicitudService.listarSolicitudesPorUsuario(1L);

        assertEquals(1, result.size());
        assertEquals("Reparación de aire acondicionado", result.get(0).getDescripcion());
    }

    @Test
    @DisplayName("Debe lanzar excepción si no se encuentra el usuario al listar solicitudes")
    void testListarSolicitudesPorUsuario_NoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.listarSolicitudesPorUsuario(1L));

        assertTrue(ex.getMessage().contains("Usuario no encontrado"));
    }

    @Test
    @DisplayName("Debe eliminar una solicitud correctamente")
    void testEliminarSolicitud_Success() {
        when(solicitudRepository.existsById(10L)).thenReturn(true);

        solicitudService.eliminarSolicitud(10L);

        verify(solicitudRepository, times(1)).deleteById(10L);
    }

    @Test
    @DisplayName("Debe lanzar excepción si la solicitud no existe al eliminar")
    void testEliminarSolicitud_NoExiste() {
        when(solicitudRepository.existsById(99L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudService.eliminarSolicitud(99L));

        assertTrue(ex.getMessage().contains("no existe"));
        verify(solicitudRepository, never()).deleteById(anyLong());
    }
}
