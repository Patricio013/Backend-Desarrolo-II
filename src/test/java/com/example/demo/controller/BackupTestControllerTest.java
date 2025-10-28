package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(BackupTestController.class)
class BackupTestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testPing_Success() throws Exception {
        mockMvc.perform(get("/api/test/ping")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("health"))
                .andExpect(jsonPath("$.operation").value("ping"))
                .andExpect(jsonPath("$.payload").value("Backend funcionando OK 🚀"));
    }

    @Test
    void testPing_Error() throws Exception {
        // Test for a non-existent endpoint to ensure 404 is returned
        mockMvc.perform(get("/api/test/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
