package com.example.demo.controller;

import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.repository.SolicitudPagoRepository;
import com.example.demo.service.SolicitudPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagosController {

    private final SolicitudPagoService service;
    private final SolicitudPagoRepository repo;

    // HU-06: Crear solicitud de pago
    @PostMapping
    public SolicitudPagoDTO crear(@Valid @RequestBody SolicitudPagoCreateDTO body) {
        return service.crearYEnviar(body);
    }

    // Listado rápido (dev)
    @GetMapping("/ultimas")
    public List<SolicitudPagoDTO> ultimas() {
        return repo.findTop50ByOrderByCreatedAtDesc().stream()
                .map(service::toDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public SolicitudPagoDTO get(@PathVariable Long id) {
        var sp = repo.findById(id).orElseThrow();
        return service.toDTO(sp);
    }
}
