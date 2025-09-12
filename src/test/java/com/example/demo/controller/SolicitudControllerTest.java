package com.example.demo.controller;

import com.example.demo.dto.InvitacionCotizacionDTO;
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
    private SolicitudService solicitudService;

    @InjectMocks
    private SolicitudController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void invitarTop3_ok() {
        SolicitudController.SolicitudTop3Resultado r = new SolicitudController.SolicitudTop3Resultado();
        r.setSolicitudId(1L);
        r.setDescripcion("test");
        r.setEstado("COTIZANDO");
        r.setTop3(List.of(new InvitacionCotizacionDTO()));

        when(solicitudService.procesarTodasLasCreadas()).thenReturn(List.of(r));

        ResponseEntity<List<SolicitudController.SolicitudTop3Resultado>> resp = controller.invitarTop3ParaTodasLasCreadas();

        assertEquals(1, resp.getBody().size());
        assertEquals("COTIZANDO", resp.getBody().get(0).getEstado());
    }

    @Test
    void crearSolicitudes_ok() {
        SolicitudesCreadasDTO dto = new SolicitudesCreadasDTO();
        Solicitud s = new Solicitud();
        s.setId(1L);

        when(solicitudService.crearDesdeEventos(anyList())).thenReturn(List.of(s));

        ResponseEntity<List<Solicitud>> resp = controller.crearSolicitudes(List.of(dto));

        assertEquals(1, resp.getBody().size());
        assertEquals(1L, resp.getBody().get(0).getId());
    }

    @Test
    void cancelar_ok() {
        doNothing().when(solicitudService).cancelarPorId(5L);

        controller.cancelar(5L);

        verify(solicitudService).cancelarPorId(5L);
    }
}
