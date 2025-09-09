package com.example.demo.controller;

import com.example.demo.service.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitudController.class)
class SolicitudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SolicitudService solicitudService;

    @Test
    void invitarTop3_debeRetornar200() throws Exception {
        when(solicitudService.procesarTodasLasCreadas()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/solicitudes/invitar-top3"))
               .andExpect(status().isOk());
    }
}
