package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.service.UsersAuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersAuthService usersAuthService;

    @Test
    void loginDebeRetornarOk() throws Exception {
        Mockito.when(usersAuthService.login(any(LoginRequest.class)))
               .thenReturn(org.springframework.http.ResponseEntity.ok("token_123"));

        String body = """
                {
                  "email": "test@example.com",
                  "password": "1234"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("token_123"));
    }

    @Test
    void loginDebeRetornarBadRequestCuandoBodyInvalido() throws Exception {
        // Sin campos requeridos -> debería fallar validación
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
