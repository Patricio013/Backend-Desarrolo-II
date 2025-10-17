package com.example.demo.service;

import com.example.demo.dto.PrestadorDTO;
import com.example.demo.entity.Habilidad;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Rubro;
import com.example.demo.entity.Zona;
import com.example.demo.repository.HabilidadRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.RubroRepository;
import com.example.demo.repository.ZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PrestadorSyncService {

  private final PrestadorRepository prestadorRepository;
  private final HabilidadRepository habilidadRepository;
  private final RubroRepository rubroRepository;
  private final ZonaRepository zonaRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Prestador upsertDesdeDTO(PrestadorDTO dto) {
    // 1) Zona
    Zona zona = null;
    if (dto.getZonaId() != null) {
      zona = zonaRepository.findByExternalId(dto.getZonaId())
          .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada: " + dto.getZonaId()));
    }

    // 2) Habilidades (resolver existentes o crear)
    List<Habilidad> habilidades = null;
    if (dto.getHabilidades() != null) {
      habilidades = resolveOrCreateHabilidades(dto.getHabilidades());
    }

    // 3) Prestador (upsert)
    Prestador p = prestadorRepository.findByExternalId(dto.getId()).orElseGet(Prestador::new);
    p.setId(dto.getId()); // guardar ID externo
    p.setNombre(dto.getNombre());
    p.setApellido(dto.getApellido());
    p.setEmail(dto.getEmail());
    p.setTelefono(dto.getTelefono());
    p.setDireccion(dto.getDireccion());
    p.setEstado(dto.getEstado());
    p.setPrecioHora(dto.getPrecioHora());
    if (zona != null) {
      p.setZona(zona);
    }

    // calificaciones (defensivo)
    if (p.getCalificacion() == null) {
      p.setCalificacion(new ArrayList<>());
    } else {
      p.getCalificacion().clear();
    }
    if (dto.getCalificacion() != null && !dto.getCalificacion().isEmpty()) {
      p.getCalificacion().addAll(dto.getCalificacion());
    }

    // trabajos finalizados
    p.setTrabajosFinalizados(dto.getTrabajosFinalizados() != null ? dto.getTrabajosFinalizados() : 0);

    // vincular habilidades (reemplazo controlado)
    if (habilidades != null) {
      p.getHabilidades().clear();
      p.getHabilidades().addAll(habilidades);
    }

    return prestadorRepository.save(p);
  }

  private List<Habilidad> resolveOrCreateHabilidades(List<Habilidad> incoming) {
    if (incoming == null || incoming.isEmpty()) return List.of();

    // dedup por (id) o (nombre+rubroId)
    Map<String, Habilidad> unique = new LinkedHashMap<>();
    for (Habilidad hIn : incoming) {
      if (hIn == null) continue;

      Habilidad resolved = null;

      if (hIn.getId() != null) {
        // Interpretamos hIn.id como ID externo recibido
        resolved = habilidadRepository.findByExternalId(hIn.getId()).orElse(null);
      }

      if (resolved == null) {
        String nombre = Objects.requireNonNull(hIn.getNombre(), "Habilidad.nombre requerido");
        Long rubroExtId = Objects.requireNonNull(
            hIn.getRubro() != null ? hIn.getRubro().getId() : null,
            "Habilidad.rubro.id requerido"
        );

        resolved = habilidadRepository.findByNombreAndRubro(nombre, rubroExtId)
            .orElseGet(() -> {
              Rubro rubro = rubroRepository.findByExternalId(rubroExtId)
                  .orElseThrow(() -> new IllegalArgumentException("Rubro inexistente: " + rubroExtId));
              Habilidad nueva = new Habilidad();
              nueva.setNombre(nombre);
              nueva.setRubro(rubro);
              nueva.setExternalId(hIn.getId());
              return habilidadRepository.save(nueva);
            });
      }

      String key = resolved.getId() != null
          ? "ID:" + resolved.getId()
          : ("NR:" + resolved.getNombre().toLowerCase() + "#" + resolved.getRubro().getId());
      unique.putIfAbsent(key, resolved);
    }
    return new ArrayList<>(unique.values());
  }
}
