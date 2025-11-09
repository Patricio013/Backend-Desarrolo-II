package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.catalog.HabilidadCatalogDTO;
import com.example.demo.dto.catalog.PrestadorCatalogDTO;
import com.example.demo.dto.catalog.RubroCatalogDTO;
import com.example.demo.dto.catalog.ZonaCatalogDTO;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.CatalogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogQueryService catalogQueryService;
    private final ModuleResponseFactory responseFactory;

    @GetMapping("/prestadores")
    public ResponseEntity<ModuleResponse<List<PrestadorCatalogDTO>>> listarPrestadores() {
        List<PrestadorCatalogDTO> prestadores = catalogQueryService.listarPrestadores();
        return ResponseEntity.ok(responseFactory.build("catalogo", "catalogPrestadores", prestadores));
    }

    @GetMapping("/habilidades")
    public ResponseEntity<ModuleResponse<List<HabilidadCatalogDTO>>> listarHabilidades() {
        List<HabilidadCatalogDTO> habilidades = catalogQueryService.listarHabilidades();
        return ResponseEntity.ok(responseFactory.build("catalogo", "catalogHabilidades", habilidades));
    }

    @GetMapping("/rubros")
    public ResponseEntity<ModuleResponse<List<RubroCatalogDTO>>> listarRubros() {
        List<RubroCatalogDTO> rubros = catalogQueryService.listarRubros();
        return ResponseEntity.ok(responseFactory.build("catalogo", "catalogRubros", rubros));
    }

    @GetMapping("/zonas")
    public ResponseEntity<ModuleResponse<List<ZonaCatalogDTO>>> listarZonas() {
        List<ZonaCatalogDTO> zonas = catalogQueryService.listarZonas();
        return ResponseEntity.ok(responseFactory.build("catalogo", "catalogZonas", zonas));
    }
}
