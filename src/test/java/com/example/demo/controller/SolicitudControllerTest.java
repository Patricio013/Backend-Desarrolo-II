package com.example.demo.controller;

import com.example.demo.dto.InvitacionCotizacionDTO;
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
    void invitarTop3ParaTodasLasCreadas_ok() {
        SolicitudController.SolicitudTop3Resultado mockResult =
                new SolicitudController.SolicitudTop3Resultado();
        mockResult.setSolicitudId(1L);
        mockResult.setEstado("COTIZANDO");
        mockResult.setTop3(List.of(new InvitacionCotizacionDTO()));

        when(solicitudService.procesarTodasLasCreadas()).thenReturn(List.of(mockResult));

        ResponseEntity<List<SolicitudController.SolicitudTop3Resultado>> response =
                controller.invitarTop3ParaTodasLasCreadas();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("COTIZANDO", response.getBody().get(0).getEstado());
    }

    @Test
    void cancelarSolicitud_ok() {
        doNothing().when(solicitudService).cancelarPorId(5L);

        controller.cancelar(5L);

        verify(solicitudService).cancelarPorId(5L);
    }

    @Test
    void recotizarSolicitud_placeholder() {
        // Método vacío -> solo verificamos que no falle
        controller.recotizarSolicitud("abc");
    }
}
