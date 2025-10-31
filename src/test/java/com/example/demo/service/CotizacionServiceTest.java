package com.example.demo.service;

import com.example.demo.client.BusquedasClient;
import com.example.demo.client.CoreCotizacionesClient;
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.SolicitudAsignarDTO;
import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.entity.Cotizacion;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.CotizacionRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudInvitacionRepository;
import com.example.demo.repository.SolicitudRepository;
import com.example.demo.websocket.SolicitudEventsPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CotizacionServiceTest {

    @InjectMocks
    private CotizacionService cotizacionService;

    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private PrestadorRepository prestadorRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private CoreCotizacionesClient coreClient;
    @Mock private SimulatedSolicitudesClient solicitudesClient;
    @Mock private BusquedasClient busquedasClient;
    @Mock private SolicitudEventsPublisher solicitudEventsPublisher;
    @Mock private SolicitudPagoService solicitudPagoService;
    @Mock private SolicitudService solicitudService;
    @Mock private MatchingPublisherService matchingPublisherService;
    @Mock private SolicitudInvitacionRepository solicitudInvitacionRepository;

    private Prestador prestador;
    private Solicitud solicitud;
    private CotizacionesSubmit cotizacionSubmit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock static TransactionSynchronizationManager
        TransactionSynchronizationManager.initSynchronization();

        prestador = new Prestador();
        prestador.setInternalId(10L);
        prestador.setId(100L); // External ID

        solicitud = new Solicitud();
        solicitud.setInternalId(20L);
        solicitud.setId(200L); // External ID
        solicitud.setCotizacionRound(1);
        solicitud.setCotizacionRoundStartedAt(LocalDateTime.now());

        cotizacionSubmit = CotizacionesSubmit.builder()
                .solicitudId(solicitud.getId())
                .prestadorId(prestador.getId())
                .monto(BigDecimal.valueOf(1500.00))
                .build();

        when(prestadorRepository.findByExternalId(prestador.getId())).thenReturn(Optional.of(prestador));
        when(solicitudRepository.findByExternalId(solicitud.getId())).thenReturn(Optional.of(solicitud));
        cotizacionService.init(); // Initialize @PostConstruct fields
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void executeAfterCommitHooks() {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
    }

    @Test
    @DisplayName("recibirCotizacion - Lanza 404 si el prestador no existe")
    void testRecibirCotizacion_PrestadorNoEncontrado() {
        when(prestadorRepository.findByExternalId(anyLong())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cotizacionService.recibirCotizacion(cotizacionSubmit));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Prestador no encontrado"));
    }

    @Test
    @DisplayName("recibirCotizacion - Lanza 404 si la solicitud no existe")
    void testRecibirCotizacion_SolicitudNoEncontrada() {
        when(solicitudRepository.findByExternalId(anyLong())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cotizacionService.recibirCotizacion(cotizacionSubmit));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Solicitud no encontrada"));
    }

    @Test
    @DisplayName("recibirCotizacion - Crea cotización nueva y no completa el objetivo")
    void testRecibirCotizacion_CreaNueva_NoCompletaObjetivo() {
        when(cotizacionRepository.findByPrestador_InternalIdAndSolicitud_InternalIdAndRound(any(), any(), anyInt())).thenReturn(Optional.empty());
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cotizacionRepository.findBySolicitud_InternalIdAndRound(any(), anyInt())).thenReturn(List.of(new Cotizacion())); // Solo 1 cotizacion
        when(solicitudInvitacionRepository.findPrestadorIdsBySolicitudAndRound(any(), anyInt())).thenReturn(List.of(1L, 2L, 3L)); // Objetivo es 3

        cotizacionService.recibirCotizacion(cotizacionSubmit);

        verify(cotizacionRepository, times(1)).save(any(Cotizacion.class));
        verify(coreClient, never()).enviarCotizacion(any());
        verify(solicitudesClient, never()).putCotizaciones(any());
        verify(busquedasClient, never()).indexarSolicitudCotizaciones(any());
        verify(matchingPublisherService, never()).publishCotizaciones(any(), any(), anyInt());

        executeAfterCommitHooks();
        verify(solicitudEventsPublisher, times(2)).notifySolicitudEvent(eq(solicitud), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("recibirCotizacion - Completa objetivo y despacha a sistemas externos")
    void testRecibirCotizacion_CompletaObjetivo_YDespacha() {
        Prestador p2 = new Prestador(); p2.setId(101L);
        Prestador p3 = new Prestador(); p3.setId(102L);

        Cotizacion c1 = Cotizacion.builder().id(1L).prestador(prestador).valor(1500.0).round(1).build();
        Cotizacion c2 = Cotizacion.builder().id(2L).prestador(p2).valor(1600.0).round(1).build();
        Cotizacion c3 = Cotizacion.builder().id(3L).prestador(p3).valor(1700.0).round(1).build();
        List<Cotizacion> cotizacionesCompletas = List.of(c1, c2, c3);

        when(cotizacionRepository.findByPrestador_InternalIdAndSolicitud_InternalIdAndRound(any(), any(), anyInt())).thenReturn(Optional.empty());
        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(c1);
        when(cotizacionRepository.findBySolicitud_InternalIdAndRound(any(), anyInt())).thenReturn(cotizacionesCompletas);
        when(solicitudInvitacionRepository.findPrestadorIdsBySolicitudAndRound(any(), anyInt())).thenReturn(List.of(1L, 2L, 3L)); // Objetivo es 3
        when(matchingPublisherService.publishCotizaciones(any(), any(), anyInt())).thenReturn(new MatchingPublisherService.PublishResult("msg-123", true, HttpStatus.OK, null));

        cotizacionService.recibirCotizacion(cotizacionSubmit);

        verify(cotizacionRepository, times(1)).save(any(Cotizacion.class));
        verify(coreClient, times(3)).enviarCotizacion(any());
        verify(solicitudesClient, times(1)).putCotizaciones(any());
        verify(busquedasClient, times(1)).indexarSolicitudCotizaciones(any());
        verify(matchingPublisherService, times(1)).publishCotizaciones(eq(solicitud), eq(cotizacionesCompletas), eq(3));

        executeAfterCommitHooks();
        // 1 for COTIZACIONES_COMPLETAS, 1 for COTIZACION_RECIBIDA, 1 for COTIZACIONES_SYNC
        verify(solicitudEventsPublisher, times(3)).notifySolicitudEvent(eq(solicitud), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("recibirCotizacion - Invita prestador extra si pasa el tiempo de espera")
    void testRecibirCotizacion_InvitaExtraPorTimeout() {
        // Set wait time to 1 minute for testing
        ReflectionTestUtils.setField(cotizacionService, "waitMinutes", 1);
        cotizacionService.init();

        // Set round start time to be in the past
        solicitud.setCotizacionRoundStartedAt(LocalDateTime.now().minus(Duration.ofMinutes(2)));

        when(cotizacionRepository.findByPrestador_InternalIdAndSolicitud_InternalIdAndRound(any(), any(), anyInt())).thenReturn(Optional.empty());
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cotizacionRepository.findBySolicitud_InternalIdAndRound(any(), anyInt())).thenReturn(List.of(new Cotizacion())); // Solo 1
        when(solicitudInvitacionRepository.findPrestadorIdsBySolicitudAndRound(any(), anyInt())).thenReturn(List.of(1L, 2L, 3L)); // Objetivo 3

        cotizacionService.recibirCotizacion(cotizacionSubmit);

        verify(solicitudService, times(1)).invitarPrestadorAdicional(solicitud);
    }


    @Test
    @DisplayName("aceptarYAsignar - Proceso exitoso")
    void testAceptarYAsignar_Success() {
        SolicitudAsignarDTO asignarDTO = SolicitudAsignarDTO.builder()
                .solicitudId(solicitud.getId())
                .prestadorId(prestador.getId())
                .monto(BigDecimal.valueOf(1200.00))
                .build();

        Cotizacion cotizacion = Cotizacion.builder()
                .id(99L)
                .solicitud(solicitud)
                .prestador(prestador)
                .valor(1200.00)
                .build();

        SolicitudPagoDTO pagoDTO = SolicitudPagoDTO.builder().id(300L).monto(BigDecimal.valueOf(1200.00)).build();

        when(cotizacionRepository.findByPrestador_InternalIdAndSolicitud_InternalIdAndRound(prestador.getInternalId(), solicitud.getInternalId(), 1))
                .thenReturn(Optional.of(cotizacion));
        when(solicitudPagoService.crearYEnviar(any(SolicitudPagoCreateDTO.class))).thenReturn(pagoDTO);

        SolicitudPagoDTO result = cotizacionService.aceptarYAsignar(asignarDTO);

        assertNotNull(result);
        assertEquals(pagoDTO.getId(), result.getId());
        verify(solicitudRepository, times(1)).save(solicitud);
        assertEquals(EstadoSolicitud.ASIGNADA, solicitud.getEstado());
        assertEquals(prestador.getId(), solicitud.getPrestadorAsignadoId());
        verify(solicitudPagoService, times(1)).crearYEnviar(any(SolicitudPagoCreateDTO.class));
        verify(matchingPublisherService, times(1)).publishSolicitudPagoEmitida(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        executeAfterCommitHooks();
        verify(solicitudEventsPublisher, times(2)).notifySolicitudEvent(eq(solicitud), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("aceptarYAsignar - Lanza 404 si la cotización no existe")
    void testAceptarYAsignar_CotizacionNoEncontrada() {
        SolicitudAsignarDTO asignarDTO = SolicitudAsignarDTO.builder()
                .solicitudId(solicitud.getId())
                .prestadorId(prestador.getId())
                .build();

        when(cotizacionRepository.findByPrestador_InternalIdAndSolicitud_InternalIdAndRound(any(), any(), anyInt()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cotizacionService.aceptarYAsignar(asignarDTO));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("No existe cotización del prestador"));
    }

    @Test
    @DisplayName("aceptarYAsignar - Lanza 409 si la solicitud está en estado final")
    void testAceptarYAsignar_EstadoConflicto() {
        solicitud.setEstado(EstadoSolicitud.COMPLETADA);
        SolicitudAsignarDTO asignarDTO = SolicitudAsignarDTO.builder()
                .solicitudId(solicitud.getId())
                .prestadorId(prestador.getId())
                .build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cotizacionService.aceptarYAsignar(asignarDTO));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("no puede asignarse en su estado actual"));
    }

    @Test
    @DisplayName("aceptarYAsignar - Lanza 400 si el monto del pago no coincide con el cotizado")
    void testAceptarYAsignar_MontoNoCoincide() {
        SolicitudAsignarDTO asignarDTO = SolicitudAsignarDTO.builder()
                .solicitudId(solicitud.getId())
                .prestadorId(prestador.getId())
                .monto(BigDecimal.valueOf(999.99)) // Monto incorrecto
                .build();

        Cotizacion cotizacion = Cotizacion.builder().valor(1200.00).build();

        when(cotizacionRepository.findByPrestador_InternalIdAndSolicitud_InternalIdAndRound(any(), any(), anyInt()))
                .thenReturn(Optional.of(cotizacion));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> cotizacionService.aceptarYAsignar(asignarDTO));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("El pago debe ser por el total cotizado"));
    }
}