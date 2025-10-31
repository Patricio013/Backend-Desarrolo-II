package com.example.demo.service;

import com.example.demo.client.SimulatedCotizacionClient;
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.Habilidad;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Rubro;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.SolicitudInvitacion;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.HabilidadRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudInvitacionRepository;
import com.example.demo.repository.SolicitudRepository;
import com.example.demo.websocket.SolicitudEventsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class SolicitudServiceTest {

    @InjectMocks
    private SolicitudService solicitudService;

    @Mock private NotificacionesService notificacionesService;
    @Mock private SolicitudEventsPublisher solicitudEventsPublisher;
    @Mock private PrestadorRepository prestadorRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private SimulatedCotizacionClient cotizacionClient;
    @Mock private SimulatedSolicitudesClient solicitudesClient;
    @Mock private SolicitudInvitacionRepository solicitudInvitacionRepository;
    @Mock private MatchingPublisherService matchingPublisherService;
    @Mock private HabilidadRepository habilidadRepository;

    private Solicitud solicitud;
    private Prestador prestador;
    private Habilidad habilidad;
    private Rubro rubro;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        rubro = new Rubro();
        rubro.setId(1L);
        rubro.setExternalId(200L);
        rubro.setNombre("Plomería");

        habilidad = new Habilidad();
        habilidad.setId(2L);
        habilidad.setExternalId(100L);
        habilidad.setNombre("Arreglo de cañerías");
        habilidad.setRubro(rubro);

        solicitud = Solicitud.builder()
                .id(1L) // External ID
                .internalId(101L)
                .descripcion("Fuga de agua en la cocina")
                .estado(EstadoSolicitud.CREADA)
                .habilidadId(habilidad.getExternalId())
                .cotizacionRound(0)
                .build();

        prestador = new Prestador();
        prestador.setId(300L); // External ID
        prestador.setInternalId(201L);
        prestador.setNombre("Juan");
        prestador.setApellido("Perez");

        when(matchingPublisherService.publishSolicitudesTop3(any()))
                .thenReturn(new MatchingPublisherService.PublishResult("msg-123", true, null, null));
    }

    @Test
    @DisplayName("procesarTodasLasCreadas - Procesa una solicitud y la pasa a COTIZANDO")
    void testProcesarTodasLasCreadas_Success() {
        // Arrange
        when(solicitudesClient.obtenerSolicitudesCreadas()).thenReturn(List.of(solicitud));
        when(habilidadRepository.findByExternalId(habilidad.getExternalId())).thenReturn(Optional.of(habilidad));
        when(prestadorRepository.findTopByHabilidadRanked(anyLong(), any(PageRequest.class))).thenReturn(List.of(prestador));
        when(cotizacionClient.enviarInvitacion(any())).thenReturn(true);

        // Act
        List<SolicitudTop3Resultado> resultados = solicitudService.procesarTodasLasCreadas();

        // Assert
        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals(EstadoSolicitud.COTIZANDO.name(), resultados.get(0).getEstado());
        assertEquals(1, resultados.get(0).getTop3().size());

        verify(solicitudRepository, times(1)).save(solicitud);
        assertEquals(EstadoSolicitud.COTIZANDO, solicitud.getEstado());
        assertTrue(solicitud.isFueCotizada());
        assertNotNull(solicitud.getCotizacionRoundStartedAt());

        verify(cotizacionClient, times(1)).enviarInvitacion(any());
        verify(solicitudInvitacionRepository, times(1)).save(any(SolicitudInvitacion.class));
        verify(matchingPublisherService, times(1)).publishSolicitudesTop3(any());
    }

    @Test
    @DisplayName("procesarTodasLasCreadas - No encuentra candidatos y la solicitud queda en CREADA")
    void testProcesarTodasLasCreadas_NoCandidates() {
        // Arrange
        when(solicitudesClient.obtenerSolicitudesCreadas()).thenReturn(List.of(solicitud));
        when(habilidadRepository.findByExternalId(habilidad.getExternalId())).thenReturn(Optional.of(habilidad));
        when(prestadorRepository.findTopByHabilidadRanked(anyLong(), any(PageRequest.class))).thenReturn(Collections.emptyList());
        when(prestadorRepository.findTopByRubroRanked(anyLong(), any(PageRequest.class))).thenReturn(Collections.emptyList());

        // Act
        List<SolicitudTop3Resultado> resultados = solicitudService.procesarTodasLasCreadas();

        // Assert
        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals(EstadoSolicitud.CREADA.name(), resultados.get(0).getEstado());
        assertTrue(resultados.get(0).getTop3().isEmpty());

        verify(solicitudRepository, never()).save(any());
        assertEquals(EstadoSolicitud.CREADA, solicitud.getEstado());
    }

    @Test
    @DisplayName("procesarTodasLasCreadas - Procesa con prestador asignado")
    void testProcesarTodasLasCreadas_WithAssignedProvider() {
        // Arrange
        solicitud.setPrestadorAsignadoId(prestador.getId());
        when(solicitudesClient.obtenerSolicitudesCreadas()).thenReturn(List.of(solicitud));
        when(prestadorRepository.findByExternalId(prestador.getId())).thenReturn(Optional.of(prestador));
        when(habilidadRepository.findByExternalId(habilidad.getExternalId())).thenReturn(Optional.of(habilidad));
        when(cotizacionClient.enviarInvitacion(any())).thenReturn(true);

        // Act
        List<SolicitudTop3Resultado> resultados = solicitudService.procesarTodasLasCreadas();

        // Assert
        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals(EstadoSolicitud.COTIZANDO.name(), resultados.get(0).getEstado());
        assertEquals(1, resultados.get(0).getTop3().size());

        verify(solicitudRepository, times(1)).save(solicitud);
        assertEquals(EstadoSolicitud.COTIZANDO, solicitud.getEstado());
    }

    @Test
    @DisplayName("recotizar - Recotiza una solicitud CANCELADA exitosamente")
    void testRecotizar_Success() {
        // Arrange
        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitud.setCotizacionRound(1);
        when(solicitudRepository.findByExternalId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        when(habilidadRepository.findByExternalId(habilidad.getExternalId())).thenReturn(Optional.of(habilidad));
        when(prestadorRepository.findTopByHabilidadExcluyendoLosQueCotizaron(anyLong(), anyLong(), any(PageRequest.class))).thenReturn(List.of(prestador));
        when(solicitudInvitacionRepository.findPrestadorIdsBySolicitud(anyLong())).thenReturn(Collections.emptyList());
        when(cotizacionClient.enviarInvitacion(any())).thenReturn(true);

        // Act
        SolicitudTop3Resultado resultado = solicitudService.recotizar(solicitud.getId());

        // Assert
        assertNotNull(resultado);
        assertEquals(EstadoSolicitud.COTIZANDO.name(), resultado.getEstado());
        assertEquals(2, solicitud.getCotizacionRound());
        verify(solicitudRepository, times(1)).save(solicitud);
    }

    @Test
    @DisplayName("recotizar - Lanza excepción si la solicitud no está CANCELADA")
    void testRecotizar_IllegalState() {
        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        when(solicitudRepository.findByExternalId(solicitud.getId())).thenReturn(Optional.of(solicitud));

        assertThrows(IllegalStateException.class, () -> solicitudService.recotizar(solicitud.getId()));
    }

    @Test
    @DisplayName("crearDesdeEventos - Ignora DTO si la solicitud ya existe (idempotencia)")
    void testCrearDesdeEventos_Idempotency() {
        // Arrange
        SolicitudesCreadasDTO dto = new SolicitudesCreadasDTO();
        dto.setSolicitudId(solicitud.getId());

        when(solicitudRepository.findByExternalId(solicitud.getId())).thenReturn(Optional.of(solicitud));

        // Act
        List<Solicitud> resultados = solicitudService.crearDesdeEventos(List.of(dto));

        // Assert
        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals(solicitud.getId(), resultados.get(0).getId());

        verify(solicitudRepository, never()).save(any());
        verify(solicitudesClient, never()).obtenerSolicitudesCreadas();
        verify(matchingPublisherService, never()).publishSolicitudesTop3(any());
    }
}