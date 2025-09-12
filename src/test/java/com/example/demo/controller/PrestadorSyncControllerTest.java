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

        doNothing().when(syncService).upsertDesdeDTO(dto);

        ResponseEntity<String> resp = controller.upsert(dto);

        assertEquals("ok", resp.getBody());
        verify(syncService).upsertDesdeDTO(dto);
    }

    @Test
    void upsertBatch_ok() {
        PrestadorDTO dto1 = new PrestadorDTO(); dto1.setId(1L);
        PrestadorDTO dto2 = new PrestadorDTO(); dto2.setId(2L);

        doNothing().when(syncService).upsertDesdeDTO(any());

        ResponseEntity<String> resp = controller.upsertBatch(List.of(dto1, dto2));

        assertEquals("ok", resp.getBody());
        verify(syncService, times(2)).upsertDesdeDTO(any());
    }
}
