package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.CalificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/prestadores")
@RequiredArgsConstructor
@Slf4j
public class CalificacionController {

  private final CalificacionService calificacionService;
  private final ModuleResponseFactory responseFactory;

  @PostMapping(
      value = "/calificaciones",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ModuleResponse<String>> agregarBatch(@RequestBody List<RecibirCalificacionesDTO> items) {
    int ok = 0, fail = 0;
    for (RecibirCalificacionesDTO it : items) {
      try {
        calificacionService.appendBatchItem(it);
        ok++;
      } catch (ResponseStatusException e) {
        fail++;
        log.warn("Calif batch: id={} -> {} {}", it != null ? it.getId() : null, e.getStatusCode(), e.getReason());
      } catch (Exception e) {
        fail++;
        log.error("Calif batch: id={} -> error {}", it != null ? it.getId() : null, e.toString());
      }
    }
    log.info("Batch calificaciones: ok={}, fail={}", ok, fail);
    return ResponseEntity.ok(responseFactory.build("calificaciones", "calificacionesBatchProcesadas", "ok"));
  }
}
