package com.example.demo.controller;

import com.example.demo.service.SolicitudService;

import com.example.demo.dto.InvitacionCotizacionDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

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
    public void recotizarSolicitud(@PathVariable String id) {
        
    }
}
