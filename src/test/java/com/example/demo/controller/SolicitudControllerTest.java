package com.example.demo.controller;

import com.example.demo.model.Solicitud;
import com.example.demo.model.dto.SolicitudPagoDTO;
import com.example.demo.model.dto.SolicitudTop3Resultado;
import com.example.demo.service.CotizacionService;
import com.example.demo.service.SolicitudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SolicitudControllerTest {

    @Mock
    private SolicitudService solicitudService;

    @Mock
    private CotizacionService cotizacionService;

    @InjectMocks
    private SolicitudController solicitudController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInvitarTop3() {
        List<SolicitudTop3Resultado> mockResult = List.of(new SolicitudTop3Resultado());
        when(solicitudService.procesarTodasLasCreadas()).thenReturn(mockResult);

        ResponseEntity<List<SolicitudTop3Resultado>> response = solicitudController.invitarTop3();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockResult, response.getBody());
        verify(solicitudService, times(1)).procesarTodasLasCreadas();
    }

    @Test
    void testCrearDesdeEventos() {
        List<Solicitud> mockSolicitudes = List.of(new Solicitud(), new Solicitud());
        when(solicitudService.crearDesdeEventos()).thenReturn(mockSolicitudes);

        ResponseEntity<List<Solicitud>> response = solicitudController.crearDesdeEventos();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(solicitudService).crearDesdeEventos();
    }

    @Test
    void testCancelarPorId() {
        Long solicitudId = 5L;
        when(solicitudService.cancelarPorId(solicitudId)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = solicitudController.cancelarPorId(solicitudId);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().containsKey("status"));
        assertEquals("cancelled", response.getBody().get("status"));
        verify(solicitudService).cancelarPorId(solicitudId);
    }

    @Test
    void testRecotizar() {
        Long solicitudId = 3L;
        SolicitudTop3Resultado result = new SolicitudTop3Resultado();
        when(solicitudService.recotizar(solicitudId)).thenReturn(result);

        ResponseEntity<SolicitudTop3Resultado> response = solicitudController.recotizar(solicitudId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(result, response.getBody());
        verify(solicitudService).recotizar(solicitudId);
    }

    @Test
    void testRecibirCotizacion() {
        Map<String, Object> input = Map.of("solicitudId", 1);
        Map<String, Object> expected = Map.of("status", "ok");
        when(cotizacionService.recibirCotizacion(input)).thenReturn(expected);

        ResponseEntity<Map<String, Object>> response = solicitudController.recibirCotizacion(input);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expected, response.getBody());
        verify(cotizacionService).recibirCotizacion(input);
    }

    @Test
    void testAsignarSolicitud() {
        Map<String, Object> input = Map.of("cotizacionId", 9);
        SolicitudPagoDTO dto = new SolicitudPagoDTO();
        when(cotizacionService.aceptarYAsignar(input)).thenReturn(dto);

        ResponseEntity<SolicitudPagoDTO> response = solicitudController.asignarSolicitud(input);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
        verify(cotizacionService).aceptarYAsignar(input);
    }

    @Test
    void testListarTodasComoWs() {
        List<Map<String, Object>> mockWs = List.of(Map.of("id", 1), Map.of("id", 2));
        when(solicitudService.listarTodasComoWs()).thenReturn(mockWs);

        ResponseEntity<List<Map<String, Object>>> response = solicitudController.listarTodasComoWs();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(solicitudService).listarTodasComoWs();
    }
}
