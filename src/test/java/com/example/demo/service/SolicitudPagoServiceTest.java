package com.example.demo.service;

import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.enums.EstadoSolicitudPago;
import com.example.demo.model.Solicitud;
import com.example.demo.model.SolicitudPago;
import com.example.demo.repository.SolicitudPagoRepository;
import com.example.demo.repository.SolicitudRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SolicitudPagoService.
 * Cubre creación, actualización, búsquedas y validaciones.
 */
class SolicitudPagoServiceTest {

    @Mock
    private SolicitudPagoRepository solicitudPagoRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @InjectMocks
    private SolicitudPagoService solicitudPagoService;

    private Solicitud solicitud;
    private SolicitudPago pago;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        solicitud = new Solicitud();
        solicitud.setId(1L);
        solicitud.setDescripcion("Instalación eléctrica");

        pago = new SolicitudPago();
        pago.setId(10L);
        pago.setMonto(5000.0);
        pago.setEstado(EstadoSolicitudPago.PENDIENTE);
        pago.setSolicitud(solicitud);
    }

    // ======================================================
    // ✅ TESTS - Creación de pago
    // ======================================================

    @Test
    @DisplayName("Debe crear un pago correctamente")
    void testCrearPago_Success() {
        SolicitudPagoCreateDTO dto = new SolicitudPagoCreateDTO();
        dto.setSolicitudId(1L);
        dto.setMonto(5000.0);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(solicitudPagoRepository.save(any(SolicitudPago.class))).thenAnswer(inv -> {
            SolicitudPago sp = inv.getArgument(0);
            sp.setId(100L);
            return sp;
        });

        SolicitudPagoDTO result = solicitudPagoService.crearPago(dto);

        assertNotNull(result);
        assertEquals(5000.0, result.getMonto());
        assertEquals(EstadoSolicitudPago.PENDIENTE, result.getEstado());
        verify(solicitudPagoRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la solicitud no existe al crear pago")
    void testCrearPago_SolicitudNoExiste() {
        SolicitudPagoCreateDTO dto = new SolicitudPagoCreateDTO();
        dto.setSolicitudId(99L);
        dto.setMonto(2500.0);

        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudPagoService.crearPago(dto));

        assertTrue(ex.getMessage().contains("Solicitud no encontrada"));
        verify(solicitudPagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el monto es inválido")
    void testCrearPago_MontoInvalido() {
        SolicitudPagoCreateDTO dto = new SolicitudPagoCreateDTO();
        dto.setSolicitudId(1L);
        dto.setMonto(-500.0);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudPagoService.crearPago(dto));

        assertTrue(ex.getMessage().contains("Monto inválido"));
        verify(solicitudPagoRepository, never()).save(any());
    }

    // ======================================================
    // ✅ TESTS - Actualizar estado del pago
    // ======================================================

    @Test
    @DisplayName("Debe actualizar el estado de un pago correctamente")
    void testActualizarEstado_Success() {
        when(solicitudPagoRepository.findById(10L)).thenReturn(Optional.of(pago));
        when(solicitudPagoRepository.save(any(SolicitudPago.class))).thenReturn(pago);

        SolicitudPagoDTO result = solicitudPagoService.actualizarEstado(10L, EstadoSolicitudPago.COMPLETADO);

        assertEquals(EstadoSolicitudPago.COMPLETADO, result.getEstado());
        verify(solicitudPagoRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el pago no existe")
    void testActualizarEstado_PagoNoExiste() {
        when(solicitudPagoRepository.findById(50L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudPagoService.actualizarEstado(50L, EstadoSolicitudPago.COMPLETADO));

        assertTrue(ex.getMessage().contains("Pago no encontrado"));
        verify(solicitudPagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nuevo estado es nulo")
    void testActualizarEstado_EstadoNulo() {
        when(solicitudPagoRepository.findById(10L)).thenReturn(Optional.of(pago));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudPagoService.actualizarEstado(10L, null));

        assertTrue(ex.getMessage().contains("Estado inválido"));
        verify(solicitudPagoRepository, never()).save(any());
    }

    // ======================================================
    // ✅ TESTS - Obtener pagos
    // ======================================================

    @Test
    @DisplayName("Debe obtener pago por ID correctamente")
    void testObtenerPagoPorId_Success() {
        when(solicitudPagoRepository.findById(10L)).thenReturn(Optional.of(pago));

        SolicitudPagoDTO result = solicitudPagoService.obtenerPagoPorId(10L);

        assertNotNull(result);
        assertEquals(5000.0, result.getMonto());
        assertEquals(EstadoSolicitudPago.PENDIENTE, result.getEstado());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el pago no existe al buscar por ID")
    void testObtenerPagoPorId_NoExiste() {
        when(solicitudPagoRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudPagoService.obtenerPagoPorId(10L));

        assertTrue(ex.getMessage().contains("no encontrado"));
    }

    @Test
    @DisplayName("Debe listar pagos por solicitud")
    void testListarPorSolicitud() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(solicitudPagoRepository.findBySolicitud(solicitud)).thenReturn(List.of(pago));

        List<SolicitudPagoDTO> result = solicitudPagoService.listarPagosPorSolicitud(1L);

        assertEquals(1, result.size());
        assertEquals(5000.0, result.get(0).getMonto());
        verify(solicitudPagoRepository, times(1)).findBySolicitud(solicitud);
    }

    @Test
    @DisplayName("Debe lanzar excepción si la solicitud no existe al listar pagos")
    void testListarPorSolicitud_NoExiste() {
        when(solicitudRepository.findById(2L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudPagoService.listarPagosPorSolicitud(2L));

        assertTrue(ex.getMessage().contains("Solicitud no encontrada"));
        verify(solicitudPagoRepository, never()).findBySolicitud(any());
    }

    // ======================================================
    // ✅ TESTS - Eliminación de pagos
    // ======================================================

    @Test
    @DisplayName("Debe eliminar un pago correctamente")
    void testEliminarPago_Success() {
        when(solicitudPagoRepository.existsById(10L)).thenReturn(true);

        solicitudPagoService.eliminarPago(10L);

        verify(solicitudPagoRepository, times(1)).deleteById(10L);
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar si el pago no existe")
    void testEliminarPago_NoExiste() {
        when(solicitudPagoRepository.existsById(99L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> solicitudPagoService.eliminarPago(99L));

        assertTrue(ex.getMessage().contains("no existe"));
        verify(solicitudPagoRepository, never()).deleteById(anyLong());
    }
}
