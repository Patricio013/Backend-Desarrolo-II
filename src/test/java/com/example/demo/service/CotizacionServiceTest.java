package com.example.demo.service;

import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.entity.Cotizacion;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import com.example.demo.repository.CotizacionRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotizacionServiceTest {

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @InjectMocks
    private CotizacionService cotizacionService;

    private CotizacionesSubmit dto;
    private Prestador prestador;
    private Solicitud solicitud;
    private Cotizacion cotizacion;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        prestador = new Prestador();
        prestador.setId(1L);

        solicitud = new Solicitud();
        solicitud.setId(10L);

        cotizacion = new Cotizacion();
        cotizacion.setId(100L);
        cotizacion.setPrestador(prestador);
        cotizacion.setSolicitud(solicitud);

        dto = new CotizacionesSubmit();
        dto.setIdSolicitud(10L);
        dto.setIdPrestador(1L);
        dto.setMonto(1500.0);
    }

    // ----------- CREATE -----------
    @Test
    @DisplayName("Debe crear una cotización válida correctamente")
    void testCreateCotizacionSuccess() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));
        when(cotizacionRepository.existsBySolicitudIdAndPrestadorId(10L, 1L)).thenReturn(false);
        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacion);

        Cotizacion result = cotizacionService.createCotizacion(dto);

        assertNotNull(result);
        verify(cotizacionRepository, times(1)).save(any(Cotizacion.class));
    }

    @Test
    @DisplayName("Debe lanzar NOT_FOUND si la solicitud no existe")
    void testCreateCotizacionSolicitudNoEncontrada() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> cotizacionService.createCotizacion(dto));
    }

    @Test
    @DisplayName("Debe lanzar BAD_REQUEST si ya existe una cotización del mismo prestador para la solicitud")
    void testCreateCotizacionDuplicada() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestador));
        when(cotizacionRepository.existsBySolicitudIdAndPrestadorId(10L, 1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> cotizacionService.createCotizacion(dto));
    }

    // ----------- UPDATE -----------
    @Test
    @DisplayName("Debe actualizar correctamente una cotización existente")
    void testUpdateCotizacionSuccess() {
        Cotizacion nueva = new Cotizacion();
        nueva.setMonto(2000.0);

        when(cotizacionRepository.findById(100L)).thenReturn(Optional.of(cotizacion));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacion);

        Cotizacion result = cotizacionService.updateCotizacion(100L, nueva);
        assertEquals(2000.0, result.getMonto());
        verify(cotizacionRepository, times(1)).save(any(Cotizacion.class));
    }

    @Test
    @DisplayName("Debe lanzar NOT_FOUND si la cotización no existe al actualizar")
    void testUpdateCotizacionNoEncontrada() {
        when(cotizacionRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> cotizacionService.updateCotizacion(100L, cotizacion));
    }

    // ----------- FIND BY PRESTADOR -----------
    @Test
    @DisplayName("Debe devolver lista de cotizaciones por prestador")
    void testFindByPrestadorSuccess() {
        when(cotizacionRepository.findByPrestadorId(1L)).thenReturn(List.of(cotizacion));

        List<Cotizacion> result = cotizacionService.findByPrestador(1L);
        assertEquals(1, result.size());
        verify(cotizacionRepository, times(1)).findByPrestadorId(1L);
    }

    @Test
    @DisplayName("Debe lanzar NOT_FOUND si el prestador no tiene cotizaciones")
    void testFindByPrestadorSinCotizaciones() {
        when(cotizacionRepository.findByPrestadorId(1L)).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> cotizacionService.findByPrestador(1L));
    }
}
