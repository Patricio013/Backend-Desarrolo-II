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
import java.util.Optional;

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
    Zona zona = resolveZona(dto);

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
    String estado = dto.getEstado() != null ? dto.getEstado() : p.getEstado();
    p.setEstado(estado != null ? estado : "ACTIVO");
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

    // direcciones (reemplazo controlado)
    if (dto.getDirecciones() != null) {
      p.getDirecciones().clear();
      for (var d : dto.getDirecciones()) {
        if (d == null) continue;
        com.example.demo.entity.PrestadorDireccion pd = com.example.demo.entity.PrestadorDireccion.builder()
            .state(safeTrim(d.getState()))
            .city(safeTrim(d.getCity()))
            .street(safeTrim(d.getStreet()))
            .number(safeTrim(d.getNumber()))
            .floor(safeTrim(d.getFloor()))
            .apartment(safeTrim(d.getApartment()))
            .build();
        p.getDirecciones().add(pd);
      }
    }

    return prestadorRepository.save(p);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void desactivarPorUsuarioId(Long userExternalId) {
    if (userExternalId == null) {
      throw new IllegalArgumentException("userExternalId requerido");
    }
    Prestador p = prestadorRepository.findByExternalId(userExternalId)
        .orElseThrow(() -> new IllegalArgumentException("Prestador no encontrado para usuario: " + userExternalId));
    p.setEstado("INACTIVO");
    prestadorRepository.save(p);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void rechazarPorUsuarioId(Long userExternalId) {
    if (userExternalId == null) {
      throw new IllegalArgumentException("userExternalId requerido");
    }
    Prestador p = prestadorRepository.findByExternalId(userExternalId)
        .orElseThrow(() -> new IllegalArgumentException("Prestador no encontrado para usuario: " + userExternalId));
    p.setEstado("RECHAZADO");
    prestadorRepository.save(p);
  }

  @Transactional(readOnly = true)
  public Optional<Prestador> buscarPorUsuarioId(Long userExternalId) {
    if (userExternalId == null) {
      return Optional.empty();
    }
    return prestadorRepository.findByExternalId(userExternalId);
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
        String nombre = hIn.getNombre();
        Long rubroExtId = (hIn.getRubro() != null ? hIn.getRubro().getId() : null);

        if (nombre != null && rubroExtId != null) {
          resolved = habilidadRepository.findByNombreAndRubro(nombre, rubroExtId)
              .orElseGet(() -> {
              Rubro rubro = rubroRepository.findByExternalId(rubroExtId)
                  .orElseGet(() -> rubroRepository.save(Rubro.builder()
                      .externalId(rubroExtId)
                      .nombre("Rubro " + rubroExtId)
                      .build()));
              Habilidad nueva = new Habilidad();
              nueva.setNombre(nombre);
              nueva.setRubro(rubro);
              nueva.setExternalId(hIn.getId());
              return habilidadRepository.save(nueva);
              });
        } else {
          // Fallback: crear habilidad con nombre genérico y rubro por defecto
          Rubro rubro = ensureDefaultRubro();
          Habilidad nueva = new Habilidad();
          String nombreGenerico = (nombre != null && !nombre.isBlank())
              ? nombre
              : ("Habilidad " + (hIn.getId() != null ? hIn.getId() : "sin_id"));
          nueva.setNombre(nombreGenerico);
          nueva.setRubro(rubro);
          nueva.setExternalId(hIn.getId());
          resolved = habilidadRepository.save(nueva);
        }
      }

      String key = resolved.getId() != null
          ? "ID:" + resolved.getId()
          : ("NR:" + resolved.getNombre().toLowerCase() + "#" + resolved.getRubro().getId());
      unique.putIfAbsent(key, resolved);
    }
    return new ArrayList<>(unique.values());
  }

  private String safeTrim(String s) { return s == null ? null : s.trim(); }

  private Rubro ensureDefaultRubro() {
    Long defaultExtId = -1L;
    return rubroRepository.findByExternalId(defaultExtId)
        .orElseGet(() -> rubroRepository.save(
            Rubro.builder()
                .externalId(defaultExtId)
                .nombre("Desconocido")
                .build()
        ));
  }

  private Zona resolveZona(PrestadorDTO dto) {
    List<Long> zonaIds = dto.getZonaIds();
    if ((zonaIds == null || zonaIds.isEmpty()) && dto.getZonaId() != null) {
      zonaIds = List.of(dto.getZonaId());
    }
    if (zonaIds == null || zonaIds.isEmpty()) {
      return null;
    }
    Zona primary = null;
    for (Long externalId : zonaIds) {
      if (externalId == null) {
        continue;
      }
      Zona zona = zonaRepository.findByExternalId(externalId)
          .orElseGet(() -> zonaRepository.save(
              Zona.builder()
                  .externalId(externalId)
                  .nombre("Zona " + externalId)
                  .build()
          ));
      if (primary == null) {
        primary = zona;
      }
    }
    return primary;
  }
}
