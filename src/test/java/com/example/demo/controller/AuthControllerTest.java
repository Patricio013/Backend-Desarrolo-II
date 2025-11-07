package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.service.UsersAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersAuthService usersAuthService;

    @Test
    void testLogin_Success() throws Exception {
        String responseBody = "{\"token\":\"jwt-token\"}";
        when(usersAuthService.login(any(LoginRequest.class))).thenReturn(new org.springframework.http.ResponseEntity<>(responseBody, HttpStatus.OK));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"usuario@mail.com\",\"password\":\"12345\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
    }

    @Test
    void testLogin_Fail() throws Exception {
        when(usersAuthService.login(any(LoginRequest.class))).thenReturn(new org.springframework.http.ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bad@mail.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
