// com.example.demo.service.CotizacionService.java
package com.example.demo.service;

import com.example.demo.client.CoreCotizacionesClient;
import com.example.demo.client.BusquedasClient;              // <-- comentá si no lo tenés
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.SolicitudCotizacionesPut;        // <-- comentá si no lo tenés
import com.example.demo.entity.Cotizacion;
// import com.example.demo.entity.Notificaciones;           // <-- comentá si no lo usás
import com.example.demo.websocket.SolicitudEventsPublisher;      // <-- ajustá el paquete si difiere
import com.example.demo.repository.CotizacionRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CotizacionService {

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private PrestadorRepository prestadorRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private CoreCotizacionesClient coreClient;

    @Autowired
    private SimulatedSolicitudesClient solicitudesClient;

    @Autowired
    private BusquedasClient busquedasClient;

    // @Autowired
    // private NotificacionesService notificacionesService;

    // Publisher de eventos (tu capa WS). Ajustá el tipo/paquete si es distinto.
    @Autowired
    private SolicitudEventsPublisher solicitudEventsPublisher;

    @Transactional
    public void recibirCotizacion(CotizacionesSubmit in) {

        var prestador = prestadorRepository.findById(in.getPrestadorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Prestador no encontrado: " + in.getPrestadorId()));

        var solicitud = solicitudRepository.findById(in.getSolicitudId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada: " + in.getSolicitudId()));

        var existente = cotizacionRepository
                .findByPrestador_IdAndSolicitud_Id(in.getPrestadorId(), in.getSolicitudId());

        Cotizacion cotizacion;
        boolean created;
        if (existente.isPresent()) {
            cotizacion = existente.get();
            cotizacion.setValor(in.getMonto().doubleValue());
            created = false;
        } else {
            cotizacion = Cotizacion.builder()
                    .prestador(prestador)
                    .solicitud(solicitud)
                    .valor(in.getMonto().doubleValue())
                    .build();
            created = true;
        }
        cotizacion = cotizacionRepository.save(cotizacion);

        // (1) Enviar al Core con JSON mínimo {idsolicitud,idprestador,monto}
        coreClient.enviarCotizacion(
                CotizacionesSubmit.builder()
                        .solicitudId(solicitud.getId())
                        .prestadorId(prestador.getId())
                        .monto(in.getMonto())
                        .build()
        );

        // ===== Si aún no tenés los clientes abajo, podés comentar todo este bloque =====
        // (2) Armar payload con TODAS las cotizaciones de la solicitud
        List<SolicitudCotizacionesPut.Item> items = cotizacionRepository.findBySolicitud_Id(solicitud.getId())
                .stream()
                .map(c -> SolicitudCotizacionesPut.Item.builder()
                        .idprestador(c.getPrestador().getId())
                        .monto(BigDecimal.valueOf(c.getValor()))
                        .build())
                .collect(Collectors.toList());

        SolicitudCotizacionesPut payload = SolicitudCotizacionesPut.builder()
                .idsolicitud(solicitud.getId())
                .cotizaciones(items)
                .build();

        // (3) PUT a Solicitudes
        solicitudesClient.putCotizaciones(payload);

        // (4) Enviar a módulo de Búsquedas
        busquedasClient.indexarSolicitudCotizaciones(payload);

        // (5) Notificación interna (opcional)
        // notificacionesService.crearNotificacion(
        //     Notificaciones.builder()
        //         .cotizacionId(cotizacion.getId())
        //         .titulo(created ? "Cotización recibida" : "Cotización actualizada")
        //         .mensaje("idsolicitud=" + solicitud.getId()
        //                 + ", idprestador=" + prestador.getId()
        //                 + ", monto=" + in.getMonto())
        //         .fecha(java.time.LocalDateTime.now())
        //         .leida(false)
        //         .build()
        // );

        // ===================== WebSocket post-commit =====================
        final boolean createdFinal   = created;
        final Long solicitudIdFinal  = solicitud.getId();
        final Long prestadorIdFinal  = prestador.getId();
        final Long cotizacionIdFinal = cotizacion.getId();
        final BigDecimal montoFinal  = in.getMonto();
        final int totalCotizaciones  = items.size();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                // Evento puntual (para toasts/notifs)
                solicitudEventsPublisher.notifySolicitudEvent(
                        solicitud,
                        createdFinal ? "COTIZACION_RECIBIDA" : "COTIZACION_ACTUALIZADA",
                        createdFinal ? "Cotización recibida" : "Cotización actualizada",
                        "Prestador " + prestadorIdFinal + " cotizó $" + montoFinal,
                        Map.of(
                                "solicitudId",  solicitudIdFinal,
                                "prestadorId",  prestadorIdFinal,
                                "cotizacionId", cotizacionIdFinal,
                                "monto",        montoFinal
                        )
                );

                // Evento de snapshot para refrescar listados/tablas
                solicitudEventsPublisher.notifySolicitudEvent(
                        solicitud,
                        "SOLICITUD_COTIZACIONES_SYNC",
                        "Cotizaciones sincronizadas",
                        "Se actualizó la lista completa de cotizaciones",
                        Map.of(
                                "solicitudId", solicitudIdFinal,
                                "total",       totalCotizaciones
                        )
                );
            }
        });
        // =================================================================
    }
}
