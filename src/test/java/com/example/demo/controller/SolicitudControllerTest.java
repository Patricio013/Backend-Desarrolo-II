package com.example.demo.controller;

import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.dto.SolicitudAsignarDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.entity.Solicitud;
import com.example.demo.service.CotizacionService;
import com.example.demo.service.SolicitudService;
import com.example.demo.websocket.SolicitudEventsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitudControllerTest {

    @Mock
    private SolicitudService solicitudService;

    @Mock
    private CotizacionService cotizacionService;

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

        when(solicitudService.procesarTodasLasCreadas()).thenReturn(List.of(dto));

        ResponseEntity<List<SolicitudController.SolicitudTop3Resultado>> res =
                controller.invitarTop3ParaTodasLasCreadas();

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(1, res.getBody().size());
        verify(solicitudService).procesarTodasLasCreadas();
    }

    @Test
    void crearSolicitudes_ok() {
        Solicitud s = new Solicitud();
        s.setId(10L);
        when(solicitudService.crearDesdeEventos(any())).thenReturn(List.of(s));

        ResponseEntity<List<Solicitud>> res =
                controller.crearSolicitudes(List.of(new SolicitudesCreadasDTO()));

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(10L, res.getBody().get(0).getId());
        verify(solicitudService).crearDesdeEventos(any());
    }

    @Test
    void cancelar_ok() {
        doNothing().when(solicitudService).cancelarPorId(5L);

        controller.cancelar(5L);

        verify(solicitudService).cancelarPorId(5L);
    }

    @Test
    void recotizar_ok() {
        controller.recotizarSolicitud(7L);

        verify(solicitudService).recotizar(7L);
    }


    @Test
    void recibirCotizacion_ok() {
        CotizacionesSubmit body = new CotizacionesSubmit();
        body.setSolicitudId(1L);
        body.setPrestadorId(2L);
        body.setMonto(BigDecimal.valueOf(5000.0));

        doNothing().when(cotizacionService).recibirCotizacion(body);

        ResponseEntity<Map<String,Object>> res = controller.recibir(body);

        assertEquals(201, res.getStatusCodeValue());
        assertEquals(1L, res.getBody().get("solicitudID"));
        assertEquals(2L, res.getBody().get("prestadorID"));
        assertEquals(BigDecimal.valueOf(5000.0), res.getBody().get("monto"));
        verify(cotizacionService).recibirCotizacion(body);
    }

    @Test
    void asignar_ok() {
        SolicitudAsignarDTO input = new SolicitudAsignarDTO();
        SolicitudPagoDTO output = new SolicitudPagoDTO();
        output.setSolicitudId(1L);

        when(cotizacionService.aceptarYAsignar(input)).thenReturn(output);

        ResponseEntity<SolicitudPagoDTO> res = controller.asignar(input);

        assertEquals(201, res.getStatusCodeValue());
        assertEquals(1L, res.getBody().getSolicitudId());
        verify(cotizacionService).aceptarYAsignar(input);
    }

    @Test
    void listarTodasComoWs_ok() {
        when(solicitudService.listarTodasComoWs()).thenReturn(List.of());

        var res = controller.listarTodasComoWs();

        assertEquals(200, res.getStatusCodeValue());
        assertNotNull(res.getBody());
        verify(solicitudService).listarTodasComoWs();
    }
}
