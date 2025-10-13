package com.example.demo.controller;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final CotizacionService cotizacionService;

    /**
     * Procesa TODAS las solicitudes en estado CREADA:
     * - Cambia a COTIZANDO
     * - Invita al Top-3 de prestadores por rubro
     */
    @PostMapping("/invitar-top3")
    public ResponseEntity<List<SolicitudTop3Resultado>> invitarTop3ParaTodasLasCreadas() {
        List<SolicitudTop3Resultado> out = solicitudService.procesarTodasLasCreadas();
        return ResponseEntity.ok(out);
    }

    @PostMapping("/crear")
    public ResponseEntity<List<Solicitud>> crearSolicitudes(@RequestBody List<SolicitudesCreadasDTO> solicitudesDto) {
        List<Solicitud> creadas = solicitudService.crearDesdeEventos(solicitudesDto);
        return ResponseEntity.ok(creadas);
    }

    // ===== respuesta por solicitud =====
    public static class SolicitudTop3Resultado {
        private Long solicitudId;
        private String descripcion;
        private String estado; // COTIZANDO
        private List<InvitacionCotizacionDTO> top3;

        public Long getSolicitudId() { return solicitudId; }
        public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public List<InvitacionCotizacionDTO> getTop3() { return top3; }
        public void setTop3(List<InvitacionCotizacionDTO> top3) { this.top3 = top3; }
    }

    @PatchMapping("/{id}/cancelar")
    public void cancelar(@PathVariable Long id) {
        solicitudService.cancelarPorId(id);
    }

    @PutMapping("path/{id}/recotizar")
    public ResponseEntity<SolicitudTop3Resultado> recotizarSolicitud(@PathVariable Long id) {
        SolicitudTop3Resultado resultado = solicitudService.recotizar(id);
        return ResponseEntity.ok(resultado);
    }

    // com.example.demo.controller.SolicitudController.java
    @PostMapping("/recibirCotizacion")
    public ResponseEntity<Map<String,Object>> recibir(@Valid @RequestBody CotizacionesSubmit body) {
        cotizacionService.recibirCotizacion(body); // guarda/actualiza y envía al Core
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "solicitudID", body.getSolicitudId(),
            "prestadorID", body.getPrestadorId(),
            "monto", body.getMonto()
        ));
    }

    // Aceptar una cotización: asigna la solicitud y genera solicitud de pago
    @PostMapping("/asignar")
    public ResponseEntity<SolicitudPagoDTO> asignar(@Valid @RequestBody SolicitudAsignarDTO body) {
        SolicitudPagoDTO dto = cotizacionService.aceptarYAsignar(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }



    @GetMapping("/ws")
    public ResponseEntity<List<com.example.demo.websocket.SolicitudEventsPublisher.WsEvent>> listarTodasComoWs() {
        return ResponseEntity.ok(solicitudService.listarTodasComoWs());
    }
}
