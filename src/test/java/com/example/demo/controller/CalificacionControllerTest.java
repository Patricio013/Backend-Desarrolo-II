package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.dto.RecibirCalificacionesDTO;
import com.example.demo.response.ModuleResponseFactory;
import com.example.demo.service.CalificacionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalificacionController.class)
class CalificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalificacionService calificacionService;

    @MockBean
    private ModuleResponseFactory responseFactory;

    @Test
    void agregarBatchDebeRetornarOk() throws Exception {
        // Arrange
        String requestBody = """
            [
              {"id": 1, "comentario": "Excelente servicio"},
              {"id": 2, "comentario": "Malo"}
            ]
            """;

        @SuppressWarnings("unchecked")
        ModuleResponse<String> mockResponse = Mockito.mock(ModuleResponse.class);
        when(responseFactory.build("calificaciones", "calificacionesBatchProcesadas", "ok"))
                .thenReturn(mockResponse);

        // Act + Assert
        mockMvc.perform(post("/api/prestadores/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void agregarBatchDebeManejarErroresEnAlgunosItems() throws Exception {
        // Arrange
        String requestBody = """
            [
              {"id": 1, "comentario": "ok"},
              {"id": 2, "comentario": "falla"}
            ]
            """;

        doThrow(new RuntimeException("Error simulado"))
                .when(calificacionService).appendBatchItem(any(RecibirCalificacionesDTO.class));

        @SuppressWarnings("unchecked")
        ModuleResponse<String> mockResponse = Mockito.mock(ModuleResponse.class);
        when(responseFactory.build("calificaciones", "calificacionesBatchProcesadas", "ok"))
                .thenReturn(mockResponse);

        // Act + Assert
        mockMvc.perform(post("/api/prestadores/calificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }
}
