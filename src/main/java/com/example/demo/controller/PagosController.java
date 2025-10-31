package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.repository.SolicitudPagoRepository;
import com.example.demo.service.SolicitudPagoService;
import com.example.demo.response.ModuleResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagosController {

    private final SolicitudPagoService service;
    private final SolicitudPagoRepository repo;
    private final ModuleResponseFactory responseFactory;

    // HU-06: Crear solicitud de pago
    @PostMapping
    public ModuleResponse<SolicitudPagoDTO> crear(@Valid @RequestBody SolicitudPagoCreateDTO body) {
        var dto = service.crearYEnviar(body);
        return responseFactory.build("pagos", "solicitudPagoCreated", dto);
    }

    // Listado rápido (dev)
    @GetMapping("/ultimas")
    public ModuleResponse<List<SolicitudPagoDTO>> ultimas() {
        List<SolicitudPagoDTO> latest = repo.findTop50ByOrderByCreatedAtDesc().stream()
                .map(service::toDTO)
                .toList();
        return responseFactory.build("pagos", "solicitudesPagoLatest", latest);
    }

    @GetMapping("/{id}")
    public ModuleResponse<SolicitudPagoDTO> get(@PathVariable Long id) {
        var sp = repo.findById(id).orElseThrow();
        return responseFactory.build("pagos", "solicitudPagoDetail", service.toDTO(sp));
    }
}
