package com.example.demo.service;

import com.example.demo.dto.HabilidadAltaWebhookDTO;
import com.example.demo.entity.Habilidad;
import com.example.demo.entity.Rubro;
import com.example.demo.repository.HabilidadRepository;
import com.example.demo.repository.RubroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HabilidadSyncService {

    private final HabilidadRepository habilidadRepository;
    private final RubroRepository rubroRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Habilidad upsertDesdeDTO(HabilidadAltaWebhookDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de habilidad no puede ser null");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("ID externo de la habilidad es requerido");
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new IllegalArgumentException("Nombre de la habilidad es requerido");
        }
        if (dto.getIdRubro() == null) {
            throw new IllegalArgumentException("id_rubro es requerido para la habilidad");
        }

        Rubro rubro = rubroRepository.findByExternalId(dto.getIdRubro())
            .orElseThrow(() -> new IllegalArgumentException("Rubro no encontrado: " + dto.getIdRubro()));

        Habilidad habilidad = habilidadRepository.findByExternalId(dto.getId())
            .orElseGet(Habilidad::new);

        boolean esNueva = habilidad.getId() == null;
        if (esNueva) {
            habilidad.setExternalId(dto.getId());
        }
        habilidad.setNombre(dto.getNombre().trim());
        habilidad.setRubro(rubro);

        Habilidad guardada = habilidadRepository.save(habilidad);
        log.info("{} habilidad extId={} nombre='{}' rubro={}",
            esNueva ? "Creada" : "Actualizada",
            dto.getId(),
            dto.getNombre(),
            rubro.getExternalId());
        return guardada;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Habilidad actualizarDesdeDTO(HabilidadAltaWebhookDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de modificación de habilidad no puede ser null");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("ID externo de la habilidad es requerido para modificación");
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new IllegalArgumentException("Nombre de la habilidad es requerido para modificación");
        }
        if (dto.getIdRubro() == null) {
            throw new IllegalArgumentException("id_rubro es requerido para modificación de la habilidad");
        }

        Habilidad habilidad = habilidadRepository.findByExternalId(dto.getId())
            .orElseThrow(() -> new IllegalArgumentException("Habilidad no encontrada: " + dto.getId()));

        Rubro rubro = rubroRepository.findByExternalId(dto.getIdRubro())
            .orElseThrow(() -> new IllegalArgumentException("Rubro no encontrado: " + dto.getIdRubro()));

        habilidad.setNombre(dto.getNombre().trim());
        habilidad.setRubro(rubro);

        Habilidad guardada = habilidadRepository.save(habilidad);
        log.info("Modificada habilidad extId={} nuevoNombre='{}' rubro={}",
            dto.getId(),
            dto.getNombre(),
            rubro.getExternalId());
        return guardada;
    }
}
