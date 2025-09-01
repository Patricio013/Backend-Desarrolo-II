package com.example.demo.controller;

import com.example.demo.entity.Prestador;
import com.example.demo.dto.InvitacionCotizacionDTO;
import com.example.demo.entity.Solicitud;
import com.example.demo.service.SolicitudService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    // Flujo anterior: crear 3 solicitudes (mantengo por compatibilidad)
    @PostMapping("/cotizar/top3")
    public ResponseEntity<List<Solicitud>> crearSolicitudesTop3(@RequestBody CrearCotizacionesRequest req) {
        List<Solicitud> creadas = solicitudService.crearSolicitudesParaTop3Prestadores(
                req.getUsuarioId(), req.getRubroId(), req.getServicioId(), req.getDescripcion()
        );
        return ResponseEntity.ok(creadas);
    }

    // Nuevo flujo: una solicitud CREADA -> invitar top 3 a cotizar (simulado)
    @PostMapping("/{id}/invitar/top3")
    public ResponseEntity<List<InvitacionCotizacionDTO>> invitarTop3(@PathVariable("id") Long solicitudId) {
        return ResponseEntity.ok(solicitudService.invitarTop3ParaCotizar(solicitudId));
    }

    // Debug: listar candidatos por rubroId para verificar Postman vs DB
    @GetMapping("/candidatos")
    public ResponseEntity<List<Prestador>> listarCandidatosPorRubro(@RequestParam("rubroId") Long rubroId) {
        return ResponseEntity.ok(solicitudService.buscarPrestadoresPorRubro(rubroId));
    }

    public static class CrearCotizacionesRequest {
        private Long usuarioId;
        private Long rubroId;
        private Long servicioId;
        private String descripcion;

        public Long getUsuarioId() { return usuarioId; }
        public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
        public Long getRubroId() { return rubroId; }
        public void setRubroId(Long rubroId) { this.rubroId = rubroId; }
        public Long getServicioId() { return servicioId; }
        public void setServicioId(Long servicioId) { this.servicioId = servicioId; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    }
}
