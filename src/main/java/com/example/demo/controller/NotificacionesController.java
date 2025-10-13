package com.example.demo.controller;

import com.example.demo.dto.NotificacionDTO;
import com.example.demo.entity.Notificaciones;
import com.example.demo.service.NotificacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionesController {

    private final NotificacionesService notificacionesService;

    @GetMapping("/pendientes")
    public ResponseEntity<List<NotificacionDTO>> getNotificacionesPendientes() {
        List<Notificaciones> pendientes = notificacionesService.pendientes();
        List<NotificacionDTO> dtos = pendientes.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<NotificacionDTO> marcarComoLeida(@PathVariable Long id) {
        return notificacionesService.marcarComoLeida(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notificación no encontrada: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Long id) {
        notificacionesService.eliminarNotificacion(id);
        return ResponseEntity.noContent().build();
    }

    private NotificacionDTO toDTO(Notificaciones n) {
        return NotificacionDTO.builder()
                .id(n.getId())
                .cotizacionId(n.getCotizacionId())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .leida(n.isLeida())
                .fecha(n.getFecha())
                .build();
    }
}