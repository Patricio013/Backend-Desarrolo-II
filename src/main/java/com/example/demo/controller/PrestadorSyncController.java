package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.PrestadorDTO;
import com.example.demo.response.ModuleResponseFactory;
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
@RequestMapping("/api/prestadores-sync")
@RequiredArgsConstructor
@Slf4j

public class PrestadorSyncController {

  private final PrestadorSyncService syncService;
  private final ModuleResponseFactory responseFactory;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<ModuleResponse<String>> upsert(@RequestBody PrestadorDTO dto) {
    syncService.upsertDesdeDTO(dto); // ignoramos la entidad resultante
    return ResponseEntity.ok(responseFactory.build("prestadores", "prestadorActualizado", "ok"));
  }

  @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ModuleResponse<String>> upsertBatch(@RequestBody List<PrestadorDTO> dtos) {
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
    return ResponseEntity.ok(responseFactory.build("prestadores", "prestadoresBatchProcesados", "ok"));
  }
}
