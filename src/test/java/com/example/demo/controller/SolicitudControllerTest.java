package com.example.demo.controller;

import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.SolicitudAsignarDTO;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.Solicitud;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
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
    void testCrearDesdeEventos() {
        List<SolicitudesCreadasDTO> dtos = List.of(new SolicitudesCreadasDTO(), new SolicitudesCreadasDTO());
        when(solicitudService.crearDesdeEventos(anyList())).thenReturn(dtos);

        ResponseEntity<List<SolicitudesCreadasDTO>> response = solicitudController.crearDesdeEventos(dtos);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(solicitudService).crearDesdeEventos(dtos);
    }

    @Test
    void testCancelarPorId() {
        Long solicitudId = 5L;
        doNothing().when(solicitudService).cancelarSolicitud(solicitudId);

        ResponseEntity<Void> response = solicitudController.cancelarSolicitud(solicitudId);

        assertEquals(200, response.getStatusCodeValue());
        verify(solicitudService).cancelarSolicitud(solicitudId);
    }

    @Test
    void testRecotizar() {
        Long solicitudId = 3L;
        SolicitudTop3Resultado result = new SolicitudTop3Resultado();
        // Assuming invitarTop3 is the correct method now
        when(solicitudService.invitarTop3(solicitudId)).thenReturn(result);

        ResponseEntity<SolicitudTop3Resultado> response = solicitudController.invitarTop3(solicitudId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(result, response.getBody());
        verify(solicitudService).invitarTop3(solicitudId);
    }

    @Test
    void testRecibirCotizacion() {
        CotizacionesSubmit input = new CotizacionesSubmit();
        input.setSolicitudId(1L);
        Solicitud expected = new Solicitud();
        when(cotizacionService.createCotizacion(any(CotizacionesSubmit.class))).thenReturn(expected);

        ResponseEntity<Solicitud> response = solicitudController.registrarCotizacion(input);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expected, response.getBody());
        verify(cotizacionService).createCotizacion(input);
    }

    @Test
    void testAsignarSolicitud() {
        SolicitudAsignarDTO input = new SolicitudAsignarDTO();
        input.setCotizacionId(9L);
        SolicitudesCreadasDTO dto = new SolicitudesCreadasDTO();
        when(solicitudService.asignarSolicitud(any(SolicitudAsignarDTO.class))).thenReturn(dto);

        ResponseEntity<SolicitudesCreadasDTO> response = solicitudController.asignarSolicitud(input);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
        verify(solicitudService).asignarSolicitud(input);
    }

    @Test
    void testListarTodasComoWs() {
        List<SolicitudesCreadasDTO> mockSolicitudes = List.of(new SolicitudesCreadasDTO(), new SolicitudesCreadasDTO());
        when(solicitudService.listarTodas()).thenReturn(mockSolicitudes);

        ResponseEntity<List<SolicitudesCreadasDTO>> response = solicitudController.listarTodas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(solicitudService).listarTodas();
    }
}
