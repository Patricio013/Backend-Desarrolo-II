package com.example.demo.controller;

import com.example.demo.dto.NotificacionDTO;
import com.example.demo.entity.Notificaciones;
import com.example.demo.service.NotificacionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificacionesController.class)
class NotificacionesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificacionesService notificacionesService;

    private Notificaciones notificacion;

    @BeforeEach
    void setUp() {
        notificacion = Notificaciones.builder()
                .id(1L)
                .cotizacionId(101L)
                .titulo("Notificación de prueba")
                .mensaje("Este es un mensaje de prueba.")
                .leida(false)
                .fecha(LocalDateTime.now())
                .build();
    }

    @Test
    void getNotificacionesPendientes_debeRetornarListaDeNotificacionesDTO() throws Exception {
        when(notificacionesService.pendientes()).thenReturn(List.of(notificacion));

        mockMvc.perform(get("/api/notificaciones/pendientes"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].titulo", is("Notificación de prueba")))
                .andExpect(jsonPath("$[0].leida", is(false)));
    }

    @Test
    void marcarComoLeida_cuandoExiste_debeRetornarNotificacionActualizada() throws Exception {
        notificacion.setLeida(true);
        when(notificacionesService.marcarComoLeida(1L)).thenReturn(Optional.of(notificacion));

        mockMvc.perform(post("/api/notificaciones/1/leida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.leida", is(true)));
    }

    @Test
    void marcarComoLeida_cuandoNoExiste_debeRetornar404() throws Exception {
        when(notificacionesService.marcarComoLeida(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/notificaciones/99/leida"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarNotificacion_debeRetornar204NoContent() throws Exception {
        mockMvc.perform(delete("/api/notificaciones/1"))
                .andExpect(status().isNoContent());
    }
}