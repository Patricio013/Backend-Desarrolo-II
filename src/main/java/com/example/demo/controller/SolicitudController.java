package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.CotizacionService;
import com.example.demo.service.SolicitudService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.dto.SolicitudesCreadasDTO;
import com.example.demo.entity.Solicitud;
import com.example.demo.dto.SolicitudAsignarDTO;
import com.example.demo.dto.SolicitudPagoDTO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/solicitudes")
@CrossOrigin(origins = "http://alb-stg-31795251.us-east-2.elb.amazonaws.com")
@RequiredArgsConstructor
@Slf4j
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final CotizacionService cotizacionService;
    private final ModuleResponseFactory responseFactory;

    /**
     * Procesa TODAS las solicitudes en estado CREADA:
     * - Cambia a COTIZANDO
     * - Invita al Top-3 de prestadores por rubro
     */
    @PostMapping("/invitar-top3")
    public ResponseEntity<ModuleResponse<List<SolicitudTop3Resultado>>> invitarTop3ParaTodasLasCreadas() {
        List<SolicitudTop3Resultado> out = solicitudService.procesarTodasLasCreadas();
        return ResponseEntity.ok(responseFactory.build("solicitudes", "solicitudesTop3Invitadas", out));
    }

    @PostMapping("/crear")
    public ResponseEntity<ModuleResponse<List<Solicitud>>> crearSolicitudes(@RequestBody List<SolicitudesCreadasDTO> solicitudesDto) {
        List<Solicitud> creadas = solicitudService.crearDesdeEventos(solicitudesDto);
        return ResponseEntity.ok(responseFactory.build("solicitudes", "solicitudesCreadas", creadas));
    }

    // ===== respuesta por solicitud =====
    public static class SolicitudTop3Resultado {
        private Long solicitudId;
        private String descripcion;
        private String estado; // COTIZANDO
        private Boolean fueCotizada;
        private Boolean esCritica;
        private List<InvitacionCotizacionDTO> top3;

        public Long getSolicitudId() { return solicitudId; }
        public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public Boolean getFueCotizada() { return fueCotizada; }
        public void setFueCotizada(Boolean fueCotizada) { this.fueCotizada = fueCotizada; }
        public Boolean getEsCritica() { return esCritica; }
        public void setEsCritica(Boolean esCritica) { this.esCritica = esCritica; }
        public List<InvitacionCotizacionDTO> getTop3() { return top3; }
        public void setTop3(List<InvitacionCotizacionDTO> top3) { this.top3 = top3; }
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ModuleResponse<Map<String, Object>>> cancelar(@PathVariable Long id) {
        solicitudService.cancelarPorId(id);
        return ResponseEntity.ok(responseFactory.build(
                "solicitudes",
                "solicitudCancelada",
                Map.of("solicitudId", id, "status", "cancelled")));
    }

    @PutMapping("path/{id}/recotizar")
    public ResponseEntity<ModuleResponse<SolicitudTop3Resultado>> recotizarSolicitud(@PathVariable Long id) {
        SolicitudTop3Resultado resultado = solicitudService.recotizar(id);
        return ResponseEntity.ok(responseFactory.build("solicitudes", "solicitudRecotizada", resultado));
    }

    // com.example.demo.controller.SolicitudController.java
    @PostMapping("/recibirCotizacion")
    public ResponseEntity<ModuleResponse<Map<String,Object>>> recibir(@Valid @RequestBody CotizacionesSubmit body) {
        cotizacionService.recibirCotizacion(body); // guarda/actualiza y envía al Core
        Map<String, Object> payload = Map.of(
                "solicitudID", body.getSolicitudId(),
                "prestadorID", body.getPrestadorId(),
                "monto", body.getMonto()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseFactory.build("cotizaciones", "cotizacionRecibida", payload));
    }

    // Aceptar una cotización: asigna la solicitud y genera solicitud de pago
    @PostMapping("/asignar")
    public ResponseEntity<ModuleResponse<SolicitudPagoDTO>> asignar(@Valid @RequestBody SolicitudAsignarDTO body) {
        SolicitudPagoDTO dto = cotizacionService.aceptarYAsignar(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseFactory.build("solicitudes", "solicitudAsignada", dto));
    }



    @GetMapping("/ws")
    public ResponseEntity<ModuleResponse<List<com.example.demo.websocket.SolicitudEventsPublisher.WsEvent>>> listarTodasComoWs() {
        log.info("Listando eventos WS de solicitudes para frontend externo");
        return ResponseEntity.ok(responseFactory.build(
                "solicitudes",
                "solicitudesWsListado",
                solicitudService.listarTodasComoWs()));
    }
}
