package com.example.demo.service;

import com.example.demo.client.PagoEnvioResponse;
import com.example.demo.client.PagosClient;
import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.entity.Notificaciones;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.SolicitudPago;
import com.example.demo.entity.enums.EstadoSolicitudPago;
import com.example.demo.repository.SolicitudPagoRepository;
import com.example.demo.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitudPagoServiceTest {

    @Mock
    private SolicitudPagoRepository solicitudPagoRepository;

    @Mock
    private PagosClient pagosClient;

    @Mock
    private MatchingPublisherService matchingPublisherService;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private NotificacionesService notificacionesService;

    @InjectMocks
    private SolicitudPagoService solicitudPagoService;

    private SolicitudPagoCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        createDTO = SolicitudPagoCreateDTO.builder()
                .solicitudId(100L)
                .prestadorId(200L)
                .monto(BigDecimal.valueOf(1500.50))
                .concepto("Pago por servicio de plomería")
                .build();
    }

    @Test
    @DisplayName("Debe crear y enviar una solicitud de pago exitosamente")
    void testCrearYEnviar_Success() {
        // Arrange
        SolicitudPago spPendiente = SolicitudPago.builder().id(1L).estado(EstadoSolicitudPago.PENDIENTE).build();
        when(solicitudPagoRepository.save(any(SolicitudPago.class))).thenReturn(spPendiente);

        PagoEnvioResponse pagoResponse = new PagoEnvioResponse(true, "ext-pago-123");
        when(pagosClient.enviarSolicitudPago(any(SolicitudPagoDTO.class))).thenReturn(pagoResponse);

        Solicitud solicitudAsociada = Solicitud.builder().id(100L).usuarioId(300L).descripcion("Arreglo de canilla").build();
        when(solicitudRepository.findByExternalId(100L)).thenReturn(Optional.of(solicitudAsociada));

        // Act
        SolicitudPagoDTO result = solicitudPagoService.crearYEnviar(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals(EstadoSolicitudPago.ENVIADA, result.getEstado());
        assertEquals("ext-pago-123", result.getExternoId());

        ArgumentCaptor<SolicitudPago> captor = ArgumentCaptor.forClass(SolicitudPago.class);
        verify(solicitudPagoRepository, times(2)).save(captor.capture());
        
        SolicitudPago primeraGrabacion = captor.getAllValues().get(0);
        assertEquals(EstadoSolicitudPago.PENDIENTE, primeraGrabacion.getEstado());

        SolicitudPago segundaGrabacion = captor.getAllValues().get(1);
        assertEquals(EstadoSolicitudPago.ENVIADA, segundaGrabacion.getEstado());
        assertEquals("ext-pago-123", segundaGrabacion.getExternoId());

        verify(pagosClient, times(1)).enviarSolicitudPago(any());
        verify(notificacionesService, times(1)).crearNotificacion(any(Notificaciones.class));
        verify(matchingPublisherService, times(1)).publishSolicitudPagoEmitida(
                anyString(), eq(300L), eq(200L), eq(100L), any(), any(), any(), any(), any(), any(), eq("Arreglo de canilla")
        );
    }

    @Test
    @DisplayName("Debe marcar la solicitud como ERROR si el cliente de pagos la rechaza")
    void testCrearYEnviar_PagosClientRejects() {
        // Arrange
        SolicitudPago spPendiente = SolicitudPago.builder().id(1L).estado(EstadoSolicitudPago.PENDIENTE).build();
        when(solicitudPagoRepository.save(any(SolicitudPago.class))).thenReturn(spPendiente);

        PagoEnvioResponse pagoResponse = new PagoEnvioResponse(false, null);
        when(pagosClient.enviarSolicitudPago(any(SolicitudPagoDTO.class))).thenReturn(pagoResponse);

        when(solicitudRepository.findByExternalId(anyLong())).thenReturn(Optional.empty());

        // Act
        SolicitudPagoDTO result = solicitudPagoService.crearYEnviar(createDTO);

        // Assert
        assertNotNull(result);
        assertEquals(EstadoSolicitudPago.ERROR, result.getEstado());
        assertNull(result.getExternoId());

        ArgumentCaptor<SolicitudPago> captor = ArgumentCaptor.forClass(SolicitudPago.class);
        verify(solicitudPagoRepository, times(2)).save(captor.capture());
        assertEquals(EstadoSolicitudPago.ERROR, captor.getValue().getEstado());

        verify(pagosClient, times(1)).enviarSolicitudPago(any());
        verify(notificacionesService, times(1)).crearNotificacion(any(Notificaciones.class));
        // Se debe intentar publicar igual
        verify(matchingPublisherService, times(1)).publishSolicitudPagoEmitida(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si faltan solicitudId y ordenId")
    void testCrearYEnviar_ValidationError_MissingIds() {
        createDTO.setSolicitudId(null);
        createDTO.setOrdenId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            solicitudPagoService.crearYEnviar(createDTO);
        });

        assertTrue(ex.getMessage().contains("Debe informarse solicitudId u ordenId"));
        verifyNoInteractions(solicitudPagoRepository, pagosClient, notificacionesService, matchingPublisherService);
    }

    @Test
    @DisplayName("Debe lanzar excepción si falta prestadorId")
    void testCrearYEnviar_ValidationError_MissingPrestadorId() {
        createDTO.setPrestadorId(null);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> {
            solicitudPagoService.crearYEnviar(createDTO);
        });

        assertTrue(ex.getMessage().contains("prestadorId requerido"));
        verifyNoInteractions(solicitudPagoRepository, pagosClient, notificacionesService, matchingPublisherService);
    }
}