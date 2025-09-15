package com.example.demo.controller;

import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.Solicitud;
import com.example.demo.service.SolicitudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitudControllerTest {

    @Mock
    private SolicitudService service;

    @InjectMocks
    private SolicitudController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void invitarTop3_ok() {
        var dto = new SolicitudController.SolicitudTop3Resultado();
        dto.setSolicitudId(1L);
        dto.setEstado("COTIZANDO");

        when(service.procesarTodasLasCreadas()).thenReturn(List.of(dto));

        ResponseEntity<List<SolicitudController.SolicitudTop3Resultado>> res = controller.invitarTop3ParaTodasLasCreadas();

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(1, res.getBody().size());
        verify(service).procesarTodasLasCreadas();
    }

    @Test
    void crearSolicitudes_ok() {
        Solicitud s = new Solicitud();
        s.setId(10L);
        when(service.crearDesdeEventos(any())).thenReturn(List.of(s));

        ResponseEntity<List<Solicitud>> res = controller.crearSolicitudes(List.of(new SolicitudesCreadasDTO()));

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(10L, res.getBody().get(0).getId());
        verify(service).crearDesdeEventos(any());
    }

    @Test
    void cancelar_ok() {
        doNothing().when(service).cancelarPorId(5L);

        controller.cancelar(5L);

        verify(service).cancelarPorId(5L);
    }

    @Test
    void recotizar_ok() {
        controller.recotizarSolicitud(7L);
        verify(service).recotizar(7L);
    }

    @Test
    void listarTodasComoWs_ok() {
        when(service.listarTodasComoWs()).thenReturn(List.of());

        var res = controller.listarTodasComoWs();

        assertEquals(200, res.getStatusCodeValue());
        assertNotNull(res.getBody());
        verify(service).listarTodasComoWs();
    }
}
