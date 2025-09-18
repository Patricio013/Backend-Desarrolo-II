package com.example.demo.controller;

import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.service.CalificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/prestadores")
@RequiredArgsConstructor
@Slf4j
public class CalificacionController {

  @Autowired private CalificacionService calificacionService;

  @PostMapping(value = "/calificaciones", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> agregarBatch(@RequestBody List<RecibirCalificacionesDTO> items) {
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
    return ResponseEntity.ok("ok");
  }
}
