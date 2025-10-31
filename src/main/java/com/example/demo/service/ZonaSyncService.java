package com.example.demo.service;

import com.example.demo.dto.ZonaAltaWebhookDTO;
import com.example.demo.dto.ZonaModificacionWebhookDTO;
import com.example.demo.entity.Zona;
import com.example.demo.repository.ZonaRepository;
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
public class ZonaSyncService {

    private final ZonaRepository zonaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Zona upsertDesdeDTO(ZonaAltaWebhookDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de zona no puede ser null");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("ID de la zona es requerido");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de la zona es requerido");
        }

        // Buscar zona existente por ID
        Zona zonaExistente = zonaRepository.findByExternalId(dto.getId()).orElse(null);
        
        if (zonaExistente != null) {
            // Actualizar zona existente
            log.info("Actualizando zona existente: extId={}, nombre anterior={}, nombre nuevo={}", 
                    dto.getId(), zonaExistente.getNombre(), dto.getNombre());
            zonaExistente.setNombre(dto.getNombre().trim());
            return zonaRepository.save(zonaExistente);
        } else {
            // Crear nueva zona
            log.info("Creando nueva zona: extId={}, nombre={}", dto.getId(), dto.getNombre());
            Zona nuevaZona = Zona.builder()
                    .externalId(dto.getId())
                    .nombre(dto.getNombre().trim())
                    .build();
            return zonaRepository.save(nuevaZona);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Zona> upsertDesdeDTOs(List<ZonaAltaWebhookDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Zona> resultados = new ArrayList<>();
        for (ZonaAltaWebhookDTO dto : dtos) {
            try {
                Zona zona = upsertDesdeDTO(dto);
                resultados.add(zona);
            } catch (Exception e) {
                log.error("Error procesando zona extId={}: {}", dto.getId(), e.getMessage(), e);
                // Continuar con la siguiente zona en caso de error
            }
        }
        
        return resultados;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Zona actualizarDesdeDTO(ZonaModificacionWebhookDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de modificación de zona no puede ser null");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("ID de la zona es requerido para modificación");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de la zona es requerido para modificación");
        }

        // Buscar zona existente por ID
        Zona zonaExistente = zonaRepository.findByExternalId(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada para modificación: " + dto.getId()));

        // Actualizar zona existente
        log.info("Modificando zona: extId={}, nombre anterior={}, nombre nuevo={}", 
                dto.getId(), zonaExistente.getNombre(), dto.getNombre());
        zonaExistente.setNombre(dto.getNombre().trim());
        return zonaRepository.save(zonaExistente);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Zona> actualizarDesdeDTOs(List<ZonaModificacionWebhookDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Zona> resultados = new ArrayList<>();
        for (ZonaModificacionWebhookDTO dto : dtos) {
            try {
                Zona zona = actualizarDesdeDTO(dto);
                resultados.add(zona);
            } catch (Exception e) {
                log.error("Error procesando modificación de zona extId={}: {}", dto.getId(), e.getMessage(), e);
                // Continuar con la siguiente zona en caso de error
            }
        }
        
        return resultados;
    }
}

