package com.example.demo.service;

import com.example.demo.client.SimulatedCotizacionClient;
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.controller.SolicitudController.SolicitudTop3Resultado;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.SolicitudInvitacion;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudInvitacionRepository;
import com.example.demo.repository.SolicitudRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    private static final int INITIAL_INVITES_NON_CRITICA = 3;
    private static final int INITIAL_INVITES_CRITICA = 10;
    private static final int MAX_INVITES_NON_CRITICA = 6;
    private static final int MAX_INVITES_CRITICA = 12;
    private static final int CANDIDATE_BATCH_SIZE = 20;

    @Autowired private NotificacionesService notificacionesService;
    @Autowired private com.example.demo.websocket.SolicitudEventsPublisher solicitudEventsPublisher;
    @Autowired private PrestadorRepository prestadorRepository;
    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private SimulatedCotizacionClient cotizacionClient;
    @Autowired private SimulatedSolicitudesClient solicitudesClient;
    @Autowired private SolicitudInvitacionRepository solicitudInvitacionRepository;
    @Autowired private MatchingPublisherService matchingPublisherService;

    @Transactional
    public SolicitudTop3Resultado recotizar(Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findByExternalId(solicitudId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + solicitudId));

        if (solicitud.getEstado() != EstadoSolicitud.CANCELADA) {
            // podés relajar esto si querés recotizar igual
            throw new IllegalStateException("La solicitud no está CANCELADA");
        }

        Long rubroId = Objects.requireNonNull(solicitud.getRubroId(), "rubroId requerido");

        // Base: excluí todos los que YA COTIZARON esta solicitud
        List<Prestador> candidatos = prestadorRepository.findTopByRubroExcluyendoLosQueCotizaron(
            rubroId, solicitud.getId(), PageRequest.of(0, 10) // traigo 10 por si alguno lo filtro luego
        );

        // Extra: si hubo asignado directo y querés excluirlo aunque no haya cotizado
        Long asignado = obtenerPrestadorAsignadoId(solicitud);
        if (asignado != null) {
            candidatos = candidatos.stream().filter(p -> !p.getId().equals(asignado)).toList();
        }
        candidatos = candidatos.stream()
            .filter(p -> estaLibre(p, solicitud))
            .toList();

        int maxInicial = solicitud.isEsCritica() ? INITIAL_INVITES_CRITICA : INITIAL_INVITES_NON_CRITICA;
        List<Prestador> seleccion = candidatos.stream().limit(maxInicial).toList();
        if (seleccion.isEmpty()) {
            var out = new SolicitudTop3Resultado();
            out.setSolicitudId(solicitud.getId());
            out.setDescripcion(solicitud.getDescripcion());
            out.setEstado(solicitud.getEstado().name()); // sigue CANCELADA
            out.setFueCotizada(solicitud.isFueCotizada());
            out.setEsCritica(solicitud.isEsCritica());
            out.setTop3(List.of());
            return out;
        }

        // Volver a COTIZANDO y enviar
        solicitud.setCotizacionRound(solicitud.getCotizacionRound() + 1);
        solicitud.setCotizacionRoundStartedAt(LocalDateTime.now());
        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitud.setFueCotizada(true);
        solicitudRepository.save(solicitud);
        // Notificar cambio de estado a COTIZANDO (recotización)
        solicitudEventsPublisher.notifySolicitudEvent(
            solicitud,
            "SOLICITUD_STATUS_CHANGED",
            "Solicitud recotizada",
            "La solicitud volvió a COTIZANDO",
            Map.of()
        );

        List<InvitacionCotizacionDTO> invitaciones = new ArrayList<>();
        for (Prestador prestador : seleccion) {
            invitaciones.add(enviarInvitacion(
                solicitud,
                rubroId,
                prestador,
                "Recotización: invitación a cotizar",
                "Recotización: invitación enviada",
                "Recotización: invitación enviada",
                "Se invitó al prestador "
            ));
        }

        var out = new SolicitudTop3Resultado();
        out.setSolicitudId(solicitud.getId());
        out.setDescripcion(solicitud.getDescripcion());
        out.setEstado(solicitud.getEstado().name()); // COTIZANDO
        out.setFueCotizada(solicitud.isFueCotizada());
        out.setEsCritica(solicitud.isEsCritica());
        out.setTop3(invitaciones);
        return out;
    }


    @Transactional
    public List<SolicitudTop3Resultado> procesarTodasLasCreadas() {
        List<Solicitud> creadas = solicitudesClient.obtenerSolicitudesCreadas();
        log.info("Procesando {} solicitudes en estado CREADA", creadas.size());
        return procesarSolicitudesInterno(creadas);
    }

    @Transactional
    public List<SolicitudTop3Resultado> procesarSolicitudes(List<Solicitud> solicitudes) {
        if (solicitudes == null || solicitudes.isEmpty()) {
            return List.of();
        }
        return procesarSolicitudesInterno(solicitudes);
    }

    private List<SolicitudTop3Resultado> procesarSolicitudesInterno(List<Solicitud> solicitudes) {
        List<SolicitudTop3Resultado> resultados = solicitudes.stream()
            .filter(Objects::nonNull)
            .map(this::procesarUnaSolicitud)
            .collect(Collectors.toList());
        publicarResultados(resultados);
        return resultados;
    }

    private void publicarResultados(List<SolicitudTop3Resultado> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return;
        }
        try {
            MatchingPublisherService.PublishResult publishResult = matchingPublisherService.publishSolicitudesTop3(resultados);
            if (publishResult.success()) {
                log.info("Evento top3 publicado messageId={} status={}", publishResult.messageId(), publishResult.status());
            } else if (publishResult.messageId() == null) {
                log.info("Publicación top3 omitida: {}", publishResult.errorMessage());
            } else if (publishResult.errorMessage() != null) {
                log.warn("Publicación top3 no exitosa status={} error={}", publishResult.status(), publishResult.errorMessage());
            }
        } catch (Exception e) {
            log.error("Error inesperado al publicar evento top3", e);
        }
    }

    private SolicitudTop3Resultado procesarUnaSolicitud(Solicitud solicitud) {
        Long prestadorAsignadoId = obtenerPrestadorAsignadoId(solicitud);
        if (prestadorAsignadoId != null) {
            return procesarConPrestadorAsignado(solicitud, prestadorAsignadoId);
        }

        Long rubroId = Objects.requireNonNull(
            solicitud.getRubroId(),
            "categoriaId (rubro) requerido en solicitud " + solicitud.getId()
        );

        List<Prestador> candidatos = prestadorRepository.findTopByRubroRanked(
            rubroId, PageRequest.of(0, 10) // pedí más de 3 para que haya margen al filtrar
        );

        int maxInicial = solicitud.isEsCritica() ? INITIAL_INVITES_CRITICA : INITIAL_INVITES_NON_CRITICA;
        List<Prestador> seleccion = candidatos.stream()
            .filter(p -> estaLibre(p, solicitud))
            .limit(maxInicial)
            .toList();

        if (seleccion.isEmpty()) {
            log.warn("Sin candidatos ACTIVO para rubro {} (solicitud {}) — se mantiene en CREADA", rubroId, solicitud.getId());
            SolicitudTop3Resultado out = new SolicitudTop3Resultado();
            out.setSolicitudId(solicitud.getId());
            out.setDescripcion(solicitud.getDescripcion());
            out.setEstado(solicitud.getEstado().name());
            out.setFueCotizada(solicitud.isFueCotizada());
            out.setEsCritica(solicitud.isEsCritica());
            out.setTop3(List.of());
            return out;
        }

        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitud.setFueCotizada(true);
        solicitud.setCotizacionRoundStartedAt(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        // Notificar cambio de estado a COTIZANDO
        solicitudEventsPublisher.notifySolicitudEvent(
            solicitud,
            "SOLICITUD_STATUS_CHANGED",
            "Solicitud en cotización",
            "La solicitud pasó a COTIZANDO",
            Map.of()
        );

        List<InvitacionCotizacionDTO> invitaciones = new ArrayList<>();
        for (Prestador prestador : seleccion) {
            invitaciones.add(enviarInvitacion(
                solicitud,
                rubroId,
                prestador,
                "Invitación a cotizar",
                "Invitación de cotización enviada",
                "Invitación de cotización enviada",
                "Se invitó al prestador "
            ));
        }

        SolicitudTop3Resultado out = new SolicitudTop3Resultado();
        out.setSolicitudId(solicitud.getId());
        out.setDescripcion(solicitud.getDescripcion());
        out.setEstado(solicitud.getEstado().name()); // COTIZANDO
        out.setFueCotizada(solicitud.isFueCotizada());
        out.setEsCritica(solicitud.isEsCritica());
        out.setTop3(invitaciones);
        return out;
    }

    private SolicitudTop3Resultado procesarConPrestadorAsignado(Solicitud solicitud, Long prestadorId) {
        Prestador p = prestadorRepository.findByExternalId(prestadorId).orElse(null);

        SolicitudTop3Resultado out = new SolicitudTop3Resultado();
        out.setSolicitudId(solicitud.getId());
        out.setDescripcion(solicitud.getDescripcion());

        if (p == null) {
            log.error("prestador_asignado_id={} no existe (solicitud {})", prestadorId, solicitud.getId());
            out.setEstado(solicitud.getEstado().name());
            out.setFueCotizada(solicitud.isFueCotizada());
            out.setEsCritica(solicitud.isEsCritica());
            out.setTop3(List.of());
            return out;
        }

        InvitacionCotizacionDTO aviso = buildInvitacionDTO(
            solicitud,
            Objects.requireNonNull(solicitud.getRubroId()),
            p,
            "Asignación directa de la solicitud por favor cotizar"
        );

        aviso.setEnviado(cotizacionClient.enviarInvitacion(aviso));
        registrarInvitacion(solicitud, p);

        Long refCotizacion = (aviso.getCotizacionId() != null) ? aviso.getCotizacionId() : aviso.getSolicitudId();
        notificacionesService.crearNotificacion(
            com.example.demo.entity.Notificaciones.builder()
                .cotizacionId(refCotizacion)
                .titulo("Asignación directa: invitación enviada")
                .mensaje("Se notificó al prestador " + aviso.getPrestadorId()
                        + " por la solicitud " + aviso.getSolicitudId())
                .leida(false)
                .build()
        );

        // Evento WS por invitación (asignación directa)
        solicitudEventsPublisher.notifySolicitudEvent(
            solicitud,
            "INVITACION_ENVIADA",
            "Asignación directa: invitación enviada",
            "Se invitó al prestador " + aviso.getPrestadorId(),
            Map.of("prestadorId", aviso.getPrestadorId())
        );

        solicitud.setEstado(EstadoSolicitud.COTIZANDO);
        solicitud.setFueCotizada(true);
        solicitud.setCotizacionRoundStartedAt(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        // Notificar cambio de estado a COTIZANDO
        solicitudEventsPublisher.notifySolicitudEvent(
            solicitud,
            "SOLICITUD_STATUS_CHANGED",
            "Solicitud en cotización",
            "La solicitud pasó a COTIZANDO",
            Map.of()
        );

        out.setEstado(solicitud.getEstado().name());
        out.setFueCotizada(solicitud.isFueCotizada());
        out.setEsCritica(solicitud.isEsCritica());
        out.setTop3(List.of(aviso));
        return out;
    }

    private static InvitacionCotizacionDTO buildInvitacionDTO(Solicitud s, Long rubroId, Prestador p, String mensajeBase) {
        return InvitacionCotizacionDTO.builder()
            .solicitudId(s.getId())
            .rubroId(rubroId)
            .prestadorId(p.getId())
            .prestadorNombre(p.getNombre() + " " + p.getApellido())
            .mensaje(mensajeBase + " " + s.getId())
            .timestamp(LocalDateTime.now())
            .enviado(false)
            .build();
    }

    private InvitacionCotizacionDTO enviarInvitacion(
        Solicitud solicitud,
        Long rubroId,
        Prestador prestador,
        String mensajeBase,
        String tituloNotificacion,
        String tituloEvento,
        String descripcionEventoPrefix
    ) {
        InvitacionCotizacionDTO dto = buildInvitacionDTO(solicitud, rubroId, prestador, mensajeBase);
        dto.setEnviado(cotizacionClient.enviarInvitacion(dto));

        Long referencia = dto.getCotizacionId() != null ? dto.getCotizacionId() : dto.getSolicitudId();
        notificacionesService.notificarInvitacionCotizacion(
            referencia,
            tituloNotificacion,
            "Se envió invitación al prestador " + prestador.getId() + " para la solicitud " + dto.getSolicitudId()
        );

        solicitudEventsPublisher.notifySolicitudEvent(
            solicitud,
            "INVITACION_ENVIADA",
            tituloEvento,
            descripcionEventoPrefix + prestador.getId(),
            Map.of("prestadorId", prestador.getId())
        );

        registrarInvitacion(solicitud, prestador);
        return dto;
    }

    private void registrarInvitacion(Solicitud solicitud, Prestador prestador) {
        int round = solicitud.getCotizacionRound();
        Long solicitudId = solicitud.getId();
        Long prestadorId = prestador.getId();
        if (solicitudInvitacionRepository.existsBySolicitud_IdAndPrestador_IdAndRound(solicitudId, prestadorId, round)) {
            return;
        }
        SolicitudInvitacion invitacion = SolicitudInvitacion.builder()
            .solicitud(solicitud)
            .prestador(prestador)
            .round(round)
            .enviadoAt(LocalDateTime.now())
            .build();
        solicitudInvitacionRepository.save(invitacion);
    }

    @Transactional
    public boolean invitarPrestadorAdicional(Solicitud solicitud) {
        Long rubroId = Objects.requireNonNull(solicitud.getRubroId(), "rubroId requerido");
        int round = solicitud.getCotizacionRound();
        Set<Long> invitados = new HashSet<>(
            solicitudInvitacionRepository.findPrestadorIdsBySolicitudAndRound(solicitud.getId(), round)
        );

        int maxInvitaciones = solicitud.isEsCritica() ? MAX_INVITES_CRITICA : MAX_INVITES_NON_CRITICA;
        if (invitados.size() >= maxInvitaciones) {
            log.debug("Solicitud {} alcanzó el máximo de invitaciones ({}) en round {}", solicitud.getId(), maxInvitaciones, round);
            return false;
        }

        List<Prestador> candidatos = prestadorRepository.findTopByRubroExcluyendoLosQueCotizaron(
            rubroId,
            solicitud.getId(),
            PageRequest.of(0, CANDIDATE_BATCH_SIZE)
        );

        Long asignado = obtenerPrestadorAsignadoId(solicitud);
        Prestador elegido = candidatos.stream()
            .filter(p -> asignado == null || !p.getId().equals(asignado))
            .filter(p -> estaLibre(p, solicitud))
            .filter(p -> !invitados.contains(p.getId()))
            .findFirst()
            .orElse(null);

        if (elegido == null) {
            log.debug("Sin prestadores adicionales disponibles para solicitud {} round {}", solicitud.getId(), round);
            return false;
        }

        enviarInvitacion(
            solicitud,
            rubroId,
            elegido,
            "Invitación adicional a cotizar",
            "Invitación adicional de cotización enviada",
            "Invitación adicional de cotización enviada",
            "Se invitó al prestador "
        );

        solicitud.setCotizacionRoundStartedAt(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        return true;
    }

    private Long obtenerPrestadorAsignadoId(Solicitud s) {
        try { if (s.getPrestadorAsignadoId() != null) return s.getPrestadorAsignadoId(); } catch (Exception ignored) {}
        try {
            var m = Solicitud.class.getMethod("getPrestadorAsignadoId");
            Object v = m.invoke(s);
            if (v instanceof Long) return (Long) v;
        } catch (Exception ignored) {}
        return null;
    }

    public void cancelarPorId(Long solicitudId){
        Optional<Solicitud> Opt = solicitudRepository.findByExternalId(solicitudId);
        Solicitud s = Opt.get();

        if (Opt.isEmpty()){
            System.out.println("solicitud no encontrada.");
        }else if(s.getEstado() == EstadoSolicitud.COMPLETADA){
            System.out.println("no se puede cancelar una solicitu completada.");
        }else if (s.getEstado() == EstadoSolicitud.CANCELADA){
            System.out.println("la solicitud ya esta cancelada.");
        }else{
            s.setEstado(EstadoSolicitud.CANCELADA);
            System.out.println("solicitud cancelada correctamente");
        }
        s.setEstado(EstadoSolicitud.CANCELADA);
        solicitudRepository.save(s);
        // Notificar cancelación
        solicitudEventsPublisher.notifySolicitudEvent(
            s,
            "SOLICITUD_STATUS_CHANGED",
            "Solicitud cancelada",
            "La solicitud fue cancelada",
            Map.of()
        );
    }

    private boolean estaLibre(Prestador prestador, Solicitud solicitud) {
        if (solicitud.getPreferenciaDia() == null ||
            solicitud.getPreferenciaDesde() == null ||
            solicitud.getPreferenciaHasta() == null) {
            return true; // si no hay preferencia, no filtramos
        }
    
        List<Solicitud> asignadas = solicitudRepository.findAsignadasEnDiaYFranja(
            prestador.getId(),
            solicitud.getPreferenciaDia(),
            solicitud.getPreferenciaDesde(),
            solicitud.getPreferenciaHasta()
        );
    
        return asignadas.isEmpty(); // libre si no hay choque
    }

    /**
     * Procesa un lote de solicitudes y las persiste.
     * Si alguna ya existe (mismo ID), la ignora (idempotencia).
     */
    @Transactional
    public List<Solicitud> crearDesdeEventos(List<SolicitudesCreadasDTO> eventos) {
        List<CreacionResultado> resultados = eventos.stream()
            .map(this::mapearYGuardar)
            .filter(Objects::nonNull)
            .toList();

        List<Solicitud> solicitudes = resultados.stream()
            .map(CreacionResultado::solicitud)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<Solicitud> nuevas = resultados.stream()
            .filter(CreacionResultado::esNueva)
            .map(CreacionResultado::solicitud)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (!nuevas.isEmpty()) {
            procesarSolicitudes(nuevas);
        }

        return solicitudes;
    }

    private CreacionResultado mapearYGuardar(SolicitudesCreadasDTO e) {
        if (solicitudRepository.existsById(e.getSolicitudId())) {
            Solicitud existente = solicitudRepository.findByExternalId(e.getSolicitudId()).orElse(null);
            return existente != null ? new CreacionResultado(existente, false) : null;
        }

        Solicitud.SolicitudBuilder b = Solicitud.builder()
            .id(e.getSolicitudId())
            .usuarioId(e.getUsuarioId())
            .rubroId(e.getRubro())
            .descripcion(e.getDescripcion())
            .estado(EstadoSolicitud.CREADA)
            .prestadorAsignadoId(e.getPrestadorId())
            .fueCotizada(Boolean.TRUE.equals(e.getFueCotizada()))
            .esCritica(Boolean.TRUE.equals(e.getEsCritica()));

        // Preferencia horaria
        var ph = e.getPreferenciaHoraria();
        if (ph != null) {
            if (ph.getDia() != null && !ph.getDia().isBlank()) {
                b.preferenciaDia(LocalDate.parse(ph.getDia()));
            }
            if (ph.getVentana() != null && !ph.getVentana().isBlank()) {
                b.preferenciaVentanaStr(ph.getVentana());
                var v = parseVentana(ph.getVentana());
                b.preferenciaDesde(v.desde);
                b.preferenciaHasta(v.hasta);
            }
        }

        Solicitud creada = solicitudRepository.save(b.build());
        // Notificar creación en estado CREADA
        try {
            solicitudEventsPublisher.notifySolicitudEvent(
                creada,
                "SOLICITUD_CREATED",
                "Solicitud creada",
                "Se creó la solicitud",
                Map.of()
            );
        } catch (Exception ignored) {}
        return new CreacionResultado(creada, true);
    }

    private Ventana parseVentana(String ventana) {
        String[] p = ventana.split("-");
        if (p.length != 2) throw new IllegalArgumentException("Ventana inválida: " + ventana);
        LocalTime desde = LocalTime.parse(p[0].trim());
        LocalTime hasta = LocalTime.parse(p[1].trim());
        return new Ventana(desde, hasta);
    }

    private record CreacionResultado(Solicitud solicitud, boolean esNueva) {}

    private record Ventana(LocalTime desde, LocalTime hasta) {}

    // --- Listado en formato WS ---
    public List<com.example.demo.websocket.SolicitudEventsPublisher.WsEvent> listarTodasComoWs() {
        return solicitudRepository.findAll().stream()
            .map(this::buildWsEventSegunEstado)
            .collect(Collectors.toList());
    }

    private com.example.demo.websocket.SolicitudEventsPublisher.WsEvent buildWsEventSegunEstado(Solicitud s) {
        var estado = s.getEstado();
        String status = (estado != null) ? estado.name() : null;
        String type = "";
        String title = "";
        String description = "";
        Map<String, Object> details = new HashMap<>();
        details.put("solicitudId", s.getId());
        details.put("fueCotizada", s.isFueCotizada());
        details.put("esCritica", s.isEsCritica());

        if (estado == null) {
            type = "SOLICITUD_STATUS";
            title = "Estado de solicitud";
            description = "La solicitud está en estado desconocido";
        } else switch (estado) {
            case CREADA -> {
                type = "SOLICITUD_CREADA";
                title = "Solicitud creada";
                description = "La solicitud está CREADA";
                putIfNotNull(details, "usuarioId", s.getUsuarioId());
                putIfNotNull(details, "rubroId", s.getRubroId());
                putIfNotNull(details, "descripcion", s.getDescripcion());
                putIfNotNull(details, "preferenciaDia", s.getPreferenciaDia());
                putIfNotNull(details, "preferenciaDesde", s.getPreferenciaDesde());
                putIfNotNull(details, "preferenciaHasta", s.getPreferenciaHasta());
                putIfNotNull(details, "preferenciaVentana", s.getPreferenciaVentanaStr());
            }
            case COTIZANDO -> {
                type = "SOLICITUD_COTIZANDO";
                title = "Solicitud en cotización";
                description = "La solicitud está COTIZANDO";
                putIfNotNull(details, "rubroId", s.getRubroId());
                putIfNotNull(details, "preferenciaDia", s.getPreferenciaDia());
                putIfNotNull(details, "preferenciaDesde", s.getPreferenciaDesde());
                putIfNotNull(details, "preferenciaHasta", s.getPreferenciaHasta());
                putIfNotNull(details, "preferenciaVentana", s.getPreferenciaVentanaStr());
            }
            case ASIGNADA -> {
                type = "SOLICITUD_ASIGNADA";
                title = "Solicitud asignada";
                description = "La solicitud está ASIGNADA";
                putIfNotNull(details, "prestadorAsignadoId", s.getPrestadorAsignadoId());
            }
            case EN_PROGRESO -> {
                type = "SOLICITUD_EN_PROGRESO";
                title = "Solicitud en progreso";
                description = "La solicitud está EN_PROGRESO";
                putIfNotNull(details, "prestadorAsignadoId", s.getPrestadorAsignadoId());
            }
            case COMPLETADA -> {
                type = "SOLICITUD_COMPLETADA";
                title = "Solicitud completada";
                description = "La solicitud está COMPLETADA";
                putIfNotNull(details, "prestadorAsignadoId", s.getPrestadorAsignadoId());
                putIfNotNull(details, "completedAt", s.getUpdatedAt());
            }
            case CANCELADA -> {
                type = "SOLICITUD_CANCELADA";
                title = "Solicitud cancelada";
                description = "La solicitud está CANCELADA";
                putIfNotNull(details, "canceledAt", s.getUpdatedAt());
            }
        }

        return new com.example.demo.websocket.SolicitudEventsPublisher.WsEvent(
                type, status, title, description, details);
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    public Solicitud obtenerDetalle(Long id) {
        return solicitudRepository.findByExternalId(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + id));
    }
}

