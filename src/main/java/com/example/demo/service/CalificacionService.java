package com.example.demo.service;

import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.entity.Prestador;
import com.example.demo.repository.PrestadorRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalificacionService {

  @Autowired private PrestadorRepository prestadorRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void appendBatchItem(RecibirCalificacionesDTO item) {
    if (item == null || item.getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item inválido: falta id");
    }
    var puntuaciones = item.getPuntuaciones();
    if (puntuaciones == null || puntuaciones.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item inválido: puntuaciones vacías");
    }

    List<Short> limpias = new ArrayList<>(puntuaciones.size());
    for (Short s : puntuaciones) {
      if (s == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puntuación nula no permitida");
      if (s < 1 || s > 5) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puntuación fuera de rango: " + s);
      limpias.add(s);
    }

    Prestador p = prestadorRepository.findById(item.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador no encontrado: " + item.getId()));

    if (p.getCalificacion() == null) {
      p.setCalificacion(new ArrayList<>());
    }

    int actuales = p.getCalificacion().size();
    int trabajos = p.getTrabajosFinalizados() == null ? 0 : p.getTrabajosFinalizados();
    int propuestas = limpias.size();

    if (actuales + propuestas > trabajos) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "No se pueden agregar " + propuestas + " calificaciones: ya tiene " + actuales +
              " y solo " + trabajos + " trabajos finalizados");
    }

    p.getCalificacion().addAll(limpias);
    prestadorRepository.save(p);
  }
}
