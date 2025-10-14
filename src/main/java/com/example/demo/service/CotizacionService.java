// com.example.demo.service.CotizacionService.java
package com.example.demo.service;

import com.example.demo.client.CoreCotizacionesClient;
import com.example.demo.client.BusquedasClient;              // <-- comentá si no lo tenés
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.SolicitudCotizacionesPut;        // <-- comentá si no lo tenés
import com.example.demo.dto.SolicitudAsignarDTO;
import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.entity.Cotizacion;
import com.example.demo.entity.Solicitud;
// import com.example.demo.entity.Notificaciones;           // <-- comentá si no lo usás
import com.example.demo.websocket.SolicitudEventsPublisher;      // <-- ajustá el paquete si difiere
import com.example.demo.repository.CotizacionRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CotizacionService {

    private static final int MIN_COTIZACIONES_BATCH = 3;

    private static final Logger log = LoggerFactory.getLogger(CotizacionService.class);

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

    @Autowired
    private SolicitudPagoService solicitudPagoService;

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private MatchingPublisherService matchingPublisherService;

    @Value("${solicitudes.cotizaciones.wait-minutes:5}")
    private long waitMinutes;

    private Duration maxWaitBeforeExtraInvite;

    @jakarta.annotation.PostConstruct
    void init() {
        maxWaitBeforeExtraInvite = (waitMinutes <= 0)
            ? Duration.ZERO
            : Duration.ofMinutes(waitMinutes);
    }

    @Transactional
    public void recibirCotizacion(CotizacionesSubmit in) {

        var prestador = prestadorRepository.findByExternalId(in.getPrestadorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Prestador no encontrado: " + in.getPrestadorId()));

        var solicitud = solicitudRepository.findByExternalId(in.getSolicitudId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada: " + in.getSolicitudId()));

        final int currentRound = solicitud.getCotizacionRound();

        var existente = cotizacionRepository
                .findByPrestador_IdAndSolicitud_IdAndRound(prestador.getInternalId(), solicitud.getInternalId(), currentRound);

        Cotizacion cotizacion;
        boolean created;
        if (existente.isPresent()) {
            cotizacion = existente.get();
            cotizacion.setValor(in.getMonto().doubleValue());
            cotizacion.setRound(currentRound);
            created = false;
        } else {
            cotizacion = Cotizacion.builder()
                    .prestador(prestador)
                    .solicitud(solicitud)
                    .valor(in.getMonto().doubleValue())
                    .round(currentRound)
                    .build();
            created = true;
        }
        cotizacion = cotizacionRepository.save(cotizacion);

        List<Cotizacion> cotizacionesSolicitud = cotizacionRepository
                .findBySolicitud_IdAndRound(solicitud.getInternalId(), currentRound);
        final int totalCotizaciones = cotizacionesSolicitud.size();
        final int objetivoCotizaciones = calcularObjetivoCotizaciones(solicitud);
        final boolean listoParaDespacho = totalCotizaciones >= objetivoCotizaciones;

        if (listoParaDespacho) {
            // (1) Enviar al Core con las cotizaciones disponibles en un solo batch lógico
            cotizacionesSolicitud.forEach(c -> coreClient.enviarCotizacion(
                    CotizacionesSubmit.builder()
                            .solicitudId(solicitud.getId())
                            .prestadorId(c.getPrestador().getId())
                            .monto(BigDecimal.valueOf(c.getValor()))
                            .build()
            ));

            // (2) Armar payload con TODAS las cotizaciones de la solicitud
            List<SolicitudCotizacionesPut.Item> items = cotizacionesSolicitud.stream()
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
            // (4) Enviar a modulo de Busquedas (criticas recien con 3)
            if (!solicitud.isEsCritica() || totalCotizaciones == MIN_COTIZACIONES_BATCH) {
                busquedasClient.indexarSolicitudCotizaciones(payload);
            }

            if (created && totalCotizaciones == objetivoCotizaciones) {
                MatchingPublisherService.PublishResult publishResult =
                        matchingPublisherService.publishCotizaciones(solicitud, cotizacionesSolicitud, objetivoCotizaciones);
                if (publishResult.success()) {
                    log.info("Cotizaciones publicadas al hub messageId={} status={}",
                            publishResult.messageId(), publishResult.status());
                } else if (publishResult.messageId() == null) {
                    log.debug("Publicación de cotizaciones omitida: {}", publishResult.errorMessage());
                } else {
                    log.warn("Publicación de cotizaciones fallida status={} error={}",
                            publishResult.status(), publishResult.errorMessage());
                }

                Map<String, Object> details = new HashMap<>();
                details.put("solicitudId", solicitud.getId());
                details.put("objetivoCotizaciones", objetivoCotizaciones);
                details.put("totalCotizaciones", totalCotizaciones);
                details.put("cotizaciones", cotizacionesSolicitud.stream()
                        .map(c -> Map.of(
                                "cotizacionId", c.getId(),
                                "prestadorId", c.getPrestador().getId(),
                                "monto", BigDecimal.valueOf(c.getValor()),
                                "round", c.getRound()
                        ))
                        .toList());
                details.put("enviadoABusquedas", true);

                solicitudEventsPublisher.notifySolicitudEvent(
                        solicitud,
                        "SOLICITUD_COTIZACIONES_COMPLETAS",
                        "Cotizaciones completas",
                        "Se alcanzó el objetivo de cotizaciones y se envió a búsquedas",
                        details
                );
            }
        } else {
            if (debeInvitarPrestadorExtra(solicitud, totalCotizaciones, objetivoCotizaciones)) {
                boolean invited = solicitudService.invitarPrestadorAdicional(solicitud);
                if (invited) {
                    log.debug("Solicitud {}: se envió invitación adicional en round {}", solicitud.getId(), currentRound);
                } else {
                    log.debug("Solicitud {}: no fue posible invitar prestador adicional (round {}, total invitaciones actuales {})",
                        solicitud.getId(), currentRound, totalCotizaciones);
                }
            }
        }

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
                                "monto",        montoFinal,
                                "round",        currentRound
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
                                "total",       totalCotizaciones,
                                "round",       currentRound
                        )
                );
            }
        });
        // =================================================================
    }

    private boolean debeInvitarPrestadorExtra(Solicitud solicitud, int totalCotizaciones, int objetivoCotizaciones) {
        if (totalCotizaciones >= objetivoCotizaciones) {
            return false;
        }
        LocalDateTime inicio = solicitud.getCotizacionRoundStartedAt();
        if (inicio == null) {
            solicitud.setCotizacionRoundStartedAt(LocalDateTime.now());
            return false;
        }
        Duration transcurrido = Duration.between(inicio, LocalDateTime.now());
        return transcurrido.compareTo(maxWaitBeforeExtraInvite) >= 0;
    }

    private int calcularObjetivoCotizaciones(Solicitud solicitud) {
        return solicitud.getPrestadorAsignadoId() != null ? 1 : MIN_COTIZACIONES_BATCH;
    }

    /**
     * Acepta una cotización para una solicitud, marca la solicitud como ASIGNADA
     * con el prestador indicado y genera/envía una Solicitud de Pago.
     */
    @Transactional
    public SolicitudPagoDTO aceptarYAsignar(SolicitudAsignarDTO in) {
        if (in == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");

        var solicitud = solicitudRepository.findByExternalId(in.getSolicitudId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada: " + in.getSolicitudId()));

        var prestador = prestadorRepository.findByExternalId(in.getPrestadorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prestador no encontrado: " + in.getPrestadorId()));

        final int currentRound = solicitud.getCotizacionRound();

        // Validar estado razonable para asignar
        if (solicitud.getEstado() == com.example.demo.entity.enums.EstadoSolicitud.CANCELADA
                || solicitud.getEstado() == com.example.demo.entity.enums.EstadoSolicitud.COMPLETADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no puede asignarse en su estado actual");
        }

        // Debe existir una cotización de ese prestador para esa solicitud
        var opt = cotizacionRepository.findByPrestador_IdAndSolicitud_IdAndRound(prestador.getInternalId(), solicitud.getInternalId(), currentRound);
        var cotizacion = opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No existe cotización del prestador " + in.getPrestadorId() + " para la solicitud " + in.getSolicitudId()));

        // Pagos completos: el monto debe ser igual al total cotizado
        BigDecimal totalCotizado = BigDecimal.valueOf(cotizacion.getValor());
        if (in.getMonto() != null && totalCotizado.compareTo(in.getMonto()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El pago debe ser por el total cotizado: " + totalCotizado);
        }

        // Marcar solicitud como ASIGNADA y setear prestador asignado
        solicitud.setEstado(com.example.demo.entity.enums.EstadoSolicitud.ASIGNADA);
        solicitud.setPrestadorAsignadoId(prestador.getId());
        solicitudRepository.save(solicitud);

        // Crear y enviar solicitud de pago
        String concepto = (in.getConcepto() != null && !in.getConcepto().isBlank())
                ? in.getConcepto()
                : ("Pago total por solicitud " + solicitud.getId());

        SolicitudPagoCreateDTO pagoIn = SolicitudPagoCreateDTO.builder()
                .solicitudId(solicitud.getId())
                .ordenId(null)
                .prestadorId(prestador.getId())
                .cotizacionId(cotizacion.getId())
                .monto(totalCotizado)
                .concepto(concepto)
                .vencimiento(in.getVencimiento())
                .build();

        SolicitudPagoDTO pagoDTO = solicitudPagoService.crearYEnviar(pagoIn);

        // WS: notificar aceptación y cambio de estado a ASIGNADA luego del commit
        final Long solicitudIdFinal   = solicitud.getId();
        final Long prestadorIdFinal   = prestador.getId();
        final Long cotizacionIdFinal  = cotizacion.getId();
        final BigDecimal montoFinal   = totalCotizado;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                // Evento puntual de asignación
                solicitudEventsPublisher.notifySolicitudEvent(
                        solicitud,
                        "SOLICITUD_ASIGNADA",
                        "Solicitud asignada",
                        "Prestador " + prestadorIdFinal + " asignado. Pago total $" + montoFinal,
                        Map.of(
                                "solicitudId",  solicitudIdFinal,
                                "prestadorId",  prestadorIdFinal,
                                "cotizacionId", cotizacionIdFinal,
                                "monto",        montoFinal,
                                "estado",       com.example.demo.entity.enums.EstadoSolicitud.ASIGNADA.name()
                        )
                );

                // Snapshot/refresh de estado
                solicitudEventsPublisher.notifySolicitudEvent(
                        solicitud,
                        "SOLICITUD_STATUS",
                        "Estado de solicitud",
                        "La solicitud pasó a ASIGNADA",
                        Map.of(
                                "solicitudId",        solicitudIdFinal,
                                "estado",             com.example.demo.entity.enums.EstadoSolicitud.ASIGNADA.name(),
                                "prestadorAsignadoId", prestadorIdFinal
                        )
                );
            }
        });

        return pagoDTO;
    }
}

