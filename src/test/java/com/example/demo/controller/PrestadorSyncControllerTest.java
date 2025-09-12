package com.example.demo.controller;

import com.example.demo.dto.PrestadorDTO;
import com.example.demo.service.PrestadorSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PrestadorSyncControllerTest {

    @Mock
    private PrestadorSyncService syncService;

    @InjectMocks
    private PrestadorSyncController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void upsert_ok() {
        PrestadorDTO dto = new PrestadorDTO();
        dto.setId(1L);

        ResponseEntity<String> res = controller.upsert(dto);

        assertEquals("ok", res.getBody());
        assertEquals(200, res.getStatusCode().value());
        verify(syncService).upsertDesdeDTO(dto); // ✅ verifica que se llamó
    }

    @Test
    void upsertBatch_ok() {
        PrestadorDTO dto1 = new PrestadorDTO();
        dto1.setId(1L);
        PrestadorDTO dto2 = new PrestadorDTO();
        dto2.setId(2L);

        List<PrestadorDTO> list = List.of(dto1, dto2);

        ResponseEntity<String> res = controller.upsertBatch(list);

        assertEquals("ok", res.getBody());
        assertEquals(200, res.getStatusCode().value());
        verify(syncService, times(2)).upsertDesdeDTO(any());
    }
}
