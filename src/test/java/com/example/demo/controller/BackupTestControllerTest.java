package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import com.example.demo.service.BackupTestService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(BackupTestController.class)
class BackupTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BackupTestService backupTestService;

    @Test
    void testGenerarBackup_Success() throws Exception {
        // Mock del servicio
        String expectedResponse = "Backup generado correctamente";
        Mockito.when(backupTestService.generarBackup()).thenReturn(expectedResponse);

        // Ejecución del endpoint
        mockMvc.perform(get("/api/backup/generar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));

        // Verificación de interacción con el servicio
        Mockito.verify(backupTestService, Mockito.times(1)).generarBackup();
    }

    @Test
    void testGenerarBackup_Error() throws Exception {
        // Simular excepción
        Mockito.when(backupTestService.generarBackup()).thenThrow(new RuntimeException("Error interno"));

        mockMvc.perform(get("/api/backup/generar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        Mockito.verify(backupTestService, Mockito.times(1)).generarBackup();
    }
}
