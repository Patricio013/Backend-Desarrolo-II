package com.example.demo.controller;

import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.entity.SolicitudPago;
import com.example.demo.repository.SolicitudPagoRepository;
import com.example.demo.service.SolicitudPagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagosControllerTest {

    @Mock
    private SolicitudPagoService service;

    @Mock
    private SolicitudPagoRepository repo;

    @InjectMocks
    private PagosController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void crearPago_ok() {
        SolicitudPagoCreateDTO input = new SolicitudPagoCreateDTO();
        SolicitudPagoDTO output = new SolicitudPagoDTO();
        output.setId(1L);

        when(service.crearYEnviar(input)).thenReturn(output);

        SolicitudPagoDTO result = controller.crear(input);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(service).crearYEnviar(input);
    }

    @Test
    void ultimasPagos_ok() {
        SolicitudPagoDTO dto = new SolicitudPagoDTO();
        dto.setId(1L);
        var entity = new SolicitudPago();
        entity.setId(1L);
        entity.setCreatedAt(LocalDateTime.now());

        when(repo.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(entity));
        when(service.toDTO(entity)).thenReturn(dto);

        List<SolicitudPagoDTO> result = controller.ultimas();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getPago_ok() {
        var entity = new SolicitudPago();
        entity.setId(10L);
        SolicitudPagoDTO dto = new SolicitudPagoDTO();
        dto.setId(10L);

        when(repo.findById(10L)).thenReturn(Optional.of(entity));
        when(service.toDTO(entity)).thenReturn(dto);

        SolicitudPagoDTO result = controller.get(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void getPago_notFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> controller.get(99L));
    }
}
