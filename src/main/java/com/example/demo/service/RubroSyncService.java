package com.example.demo.service;

import com.example.demo.dto.RubroAltaWebhookDTO;
import com.example.demo.dto.RubroModificacionWebhookDTO;
import com.example.demo.entity.Rubro;
import com.example.demo.repository.RubroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RubroSyncService {

    private final RubroRepository rubroRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Rubro upsertDesdeDTO(RubroAltaWebhookDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de rubro no puede ser null");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("ID del rubro es requerido");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre del rubro es requerido");
        }

        // Buscar rubro existente por ID
        Rubro rubroExistente = rubroRepository.findByExternalId(dto.getId()).orElse(null);
        
        if (rubroExistente != null) {
            // Actualizar rubro existente
            log.info("Actualizando rubro existente: extId={}, nombre anterior={}, nombre nuevo={}", 
                    dto.getId(), rubroExistente.getNombre(), dto.getNombre());
            rubroExistente.setNombre(dto.getNombre().trim());
            return rubroRepository.save(rubroExistente);
        } else {
            // Crear nuevo rubro
            log.info("Creando nuevo rubro: extId={}, nombre={}", dto.getId(), dto.getNombre());
            Rubro nuevoRubro = Rubro.builder()
                    .externalId(dto.getId())
                    .nombre(dto.getNombre().trim())
                    .build();
            return rubroRepository.save(nuevoRubro);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Rubro> upsertDesdeDTOs(List<RubroAltaWebhookDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Rubro> resultados = new ArrayList<>();
        for (RubroAltaWebhookDTO dto : dtos) {
            try {
                Rubro rubro = upsertDesdeDTO(dto);
                resultados.add(rubro);
            } catch (Exception e) {
                log.error("Error procesando rubro extId={}: {}", dto.getId(), e.getMessage(), e);
                // Continuar con el siguiente rubro en caso de error
            }
        }
        
        return resultados;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Rubro actualizarDesdeDTO(RubroModificacionWebhookDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de modificación de rubro no puede ser null");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("ID del rubro es requerido para modificación");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre del rubro es requerido para modificación");
        }

        // Buscar rubro existente por ID
        Rubro rubroExistente = rubroRepository.findByExternalId(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Rubro no encontrado para modificación: " + dto.getId()));

        // Actualizar rubro existente
        log.info("Modificando rubro: extId={}, nombre anterior={}, nombre nuevo={}", 
                dto.getId(), rubroExistente.getNombre(), dto.getNombre());
        rubroExistente.setNombre(dto.getNombre().trim());
        return rubroRepository.save(rubroExistente);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Rubro> actualizarDesdeDTOs(List<RubroModificacionWebhookDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Rubro> resultados = new ArrayList<>();
        for (RubroModificacionWebhookDTO dto : dtos) {
            try {
                Rubro rubro = actualizarDesdeDTO(dto);
                resultados.add(rubro);
            } catch (Exception e) {
                log.error("Error procesando modificación de rubro extId={}: {}", dto.getId(), e.getMessage(), e);
                // Continuar con el siguiente rubro en caso de error
            }
        }
        
        return resultados;
    }
}

