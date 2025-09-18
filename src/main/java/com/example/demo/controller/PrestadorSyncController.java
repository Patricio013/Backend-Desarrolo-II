package com.example.demo.controller;

import com.example.demo.dto.PrestadorDTO;
import com.example.demo.service.PrestadorSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;


import java.util.List;

@RestController
@RequestMapping("/prestadores-sync")
@RequiredArgsConstructor
@Slf4j

public class PrestadorSyncController {

  private final PrestadorSyncService syncService;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  @Transactional
  public ResponseEntity<String> upsert(@RequestBody PrestadorDTO dto) {
    syncService.upsertDesdeDTO(dto); // ignoramos la entidad resultante
    return ResponseEntity.ok("ok");
  }

  @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> upsertBatch(@RequestBody List<PrestadorDTO> dtos) {
    int ok = 0, fail = 0;
    for (PrestadorDTO dto : dtos) {
      try {
        syncService.upsertDesdeDTO(dto); // tiene REQUIRES_NEW -> commit por item
        ok++;
      } catch (ResponseStatusException e) {
        fail++;
        log.warn("Batch: id={} -> {} {}", dto.getId(), e.getStatusCode(), e.getReason());
      } catch (IllegalArgumentException e) {
        fail++;
        log.warn("Batch: id={} -> 400 {}", dto.getId(), e.getMessage());
      } catch (Exception e) {
        fail++;
        log.error("Batch: id={} -> 500 {}", dto.getId(), e.toString());
      }
    }
    log.info("Batch prestadores: ok={}, fail={}", ok, fail);
    return ResponseEntity.ok("ok");
  }
}
