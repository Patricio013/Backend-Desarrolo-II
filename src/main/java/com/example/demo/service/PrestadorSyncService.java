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
    Zona zona = (dto.getZonaId() != null)
        ? zonaRepository.findById(dto.getZonaId())
            .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada: " + dto.getZonaId()))
        : null;

    // 2) Habilidades (resolver existentes o crear)
    List<Habilidad> habilidades = resolveOrCreateHabilidades(dto.getHabilidades());

    // 3) Prestador (upsert)
    Prestador p = prestadorRepository.findById(dto.getId()).orElseGet(() -> new Prestador(dto.getId()));
    p.setNombre(dto.getNombre());
    p.setApellido(dto.getApellido());
    p.setEmail(dto.getEmail());
    p.setTelefono(dto.getTelefono());
    p.setDireccion(dto.getDireccion());
    p.setEstado(dto.getEstado());
    p.setPrecioHora(dto.getPrecioHora());
    p.setZona(zona);

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
    p.getHabilidades().clear();
    p.getHabilidades().addAll(habilidades);

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
        resolved = habilidadRepository.findById(hIn.getId()).orElse(null);
      }

      if (resolved == null) {
        String nombre = Objects.requireNonNull(hIn.getNombre(), "Habilidad.nombre requerido");
        Long rubroId = Objects.requireNonNull(
            hIn.getRubro() != null ? hIn.getRubro().getId() : null,
            "Habilidad.rubro.id requerido"
        );

        resolved = habilidadRepository.findByNombreAndRubro(nombre, rubroId)
            .orElseGet(() -> {
              Rubro rubro = rubroRepository.findById(rubroId)
                  .orElseThrow(() -> new IllegalArgumentException("Rubro inexistente: " + rubroId));
              Habilidad nueva = new Habilidad();
              nueva.setNombre(nombre);
              nueva.setRubro(rubro);
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
