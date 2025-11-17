package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.response.ModuleResponseFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BackupTestController.class)
class BackupTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModuleResponseFactory responseFactory;

    @Test
    void pingDebeRetornarRespuestaOk() throws Exception {
        // Arrange: creamos un mock del ModuleResponse directamente
        @SuppressWarnings("unchecked")
        ModuleResponse<String> mockResponse = Mockito.mock(ModuleResponse.class);

        when(responseFactory.build("health", "ping", "Backend funcionando OK 🚀"))
                .thenReturn(mockResponse);

        // Act + Assert
        mockMvc.perform(get("/api/test/ping")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
