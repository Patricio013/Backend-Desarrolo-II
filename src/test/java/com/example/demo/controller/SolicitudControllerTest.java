package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.SolicitudAsignarDTO;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.Solicitud;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.service.CotizacionService;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.SolicitudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import com.example.demo.websocket.SolicitudEventsPublisher;

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

    @Mock
    private ModuleResponseFactory responseFactory;

    @InjectMocks
    private SolicitudController solicitudController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


//    @Test
    void testCrearDesdeEventos() {
        List<Solicitud> mockSolicitudes = List.of(new Solicitud());
        List<SolicitudesCreadasDTO> dtos = List.of(new SolicitudesCreadasDTO(), new SolicitudesCreadasDTO());
        when(solicitudService.crearDesdeEventos(anyList())).thenReturn(mockSolicitudes);
        when(responseFactory.build(anyString(), anyString(), any())).thenReturn(new ModuleResponse<>("solicitudes", "solicitudesCreadas", mockSolicitudes, null, null));

        ResponseEntity<ModuleResponse<List<Solicitud>>> response = solicitudController.crearSolicitudes(dtos);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getPayload().size());
        verify(solicitudService).crearDesdeEventos(dtos);
    }

//    @Test
    void testCancelarPorId() {
        Long solicitudId = 5L;
        doNothing().when(solicitudService).cancelarPorId(solicitudId);
        when(responseFactory.build(anyString(), anyString(), any())).thenReturn(new ModuleResponse<>("solicitudes", "solicitudCancelada", Map.of("solicitudId", solicitudId, "status", "cancelled"), null, null));

        ResponseEntity<ModuleResponse<Map<String, Object>>> response = solicitudController.cancelar(solicitudId);

        assertEquals(200, response.getStatusCodeValue());
        verify(solicitudService).cancelarPorId(solicitudId);
    }

//    @Test
    void testRecotizar() {
        Long solicitudId = 3L;
        SolicitudTop3Resultado result = new SolicitudTop3Resultado();
        when(solicitudService.recotizar(solicitudId)).thenReturn(result);
        when(responseFactory.build(anyString(), anyString(), any())).thenReturn(new ModuleResponse<>("solicitudes", "solicitudRecotizada", result, null, null));

        ResponseEntity<ModuleResponse<SolicitudTop3Resultado>> response = solicitudController.recotizarSolicitud(solicitudId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(result, response.getBody().getPayload());
        verify(solicitudService).recotizar(solicitudId);
    }

//    @Test
    void testRecibirCotizacion() {
        CotizacionesSubmit input = new CotizacionesSubmit();
        input.setSolicitudId(1L);
        doNothing().when(cotizacionService).recibirCotizacion(any(CotizacionesSubmit.class));
        when(responseFactory.build(anyString(), anyString(), any())).thenReturn(new ModuleResponse<>("cotizaciones", "cotizacionRecibida", Map.of(), null, null));

        ResponseEntity<ModuleResponse<Map<String, Object>>> response = solicitudController.recibir(input);

        assertEquals(201, response.getStatusCodeValue());
        verify(cotizacionService).recibirCotizacion(input);
    }

//    @Test
    void testAsignarSolicitud() {
        SolicitudAsignarDTO input = new SolicitudAsignarDTO();
        SolicitudPagoDTO dto = new SolicitudPagoDTO();
        when(cotizacionService.aceptarYAsignar(any(SolicitudAsignarDTO.class))).thenReturn(dto);
        when(responseFactory.build(anyString(), anyString(), any())).thenReturn(new ModuleResponse<>("solicitudes", "solicitudAsignada", dto, null, null));

        ResponseEntity<ModuleResponse<SolicitudPagoDTO>> response = solicitudController.asignar(input);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(dto, response.getBody().getPayload());
        verify(cotizacionService).aceptarYAsignar(input);
    }

//    @Test
    void testListarTodasComoWs() {
        List<SolicitudEventsPublisher.WsEvent> mockWs = List.of();
        when(solicitudService.listarTodasComoWs()).thenReturn(mockWs);
        when(responseFactory.build(anyString(), anyString(), any())).thenReturn(new ModuleResponse<>("solicitudes", "solicitudesWsListado", mockWs, null, null));

        ResponseEntity<ModuleResponse<List<SolicitudEventsPublisher.WsEvent>>> response = solicitudController.listarTodasComoWs();

        assertEquals(200, response.getStatusCodeValue());
        verify(solicitudService).listarTodasComoWs();
    }
}
