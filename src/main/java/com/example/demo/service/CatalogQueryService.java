package com.example.demo.service;

import com.example.demo.dto.catalog.HabilidadCatalogDTO;
import com.example.demo.dto.catalog.PrestadorCatalogDTO;
import com.example.demo.dto.catalog.RubroCatalogDTO;
import com.example.demo.dto.catalog.ZonaCatalogDTO;
import com.example.demo.repository.HabilidadRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.RubroRepository;
import com.example.demo.repository.ZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final PrestadorRepository prestadorRepository;
    private final HabilidadRepository habilidadRepository;
    private final RubroRepository rubroRepository;
    private final ZonaRepository zonaRepository;

    @Transactional(readOnly = true)
    public List<PrestadorCatalogDTO> listarPrestadores() {
        Sort sort = Sort.by(Sort.Order.asc("apellido"), Sort.Order.asc("nombre"), Sort.Order.asc("id"));
        return prestadorRepository.findAll(sort).stream()
                .map(PrestadorCatalogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabilidadCatalogDTO> listarHabilidades() {
        return habilidadRepository.findAll(Sort.by("nombre")).stream()
                .map(HabilidadCatalogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RubroCatalogDTO> listarRubros() {
        return rubroRepository.findAll(Sort.by("nombre")).stream()
                .map(RubroCatalogDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ZonaCatalogDTO> listarZonas() {
        return zonaRepository.findAll(Sort.by("nombre")).stream()
                .map(ZonaCatalogDTO::fromEntity)
                .toList();
    }
}
