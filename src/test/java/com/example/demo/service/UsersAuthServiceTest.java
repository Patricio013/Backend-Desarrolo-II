package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class UsersAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UsersAuthService usersAuthService;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // We inject a real ObjectMapper because we want to test the actual JSON parsing logic.
        ObjectMapper objectMapper = new ObjectMapper();
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(usersAuthService, "objectMapper", objectMapper);

        // Set configurable properties
        ReflectionTestUtils.setField(usersAuthService, "baseUrl", "http://users-service.com");
        ReflectionTestUtils.setField(usersAuthService, "loginPath", "/api/users/login");
        ReflectionTestUtils.setField(usersAuthService, "tokenFieldNames", "token,access_token");
        ReflectionTestUtils.setField(usersAuthService, "roleClaimNames", "role,roles,authorities");
        ReflectionTestUtils.setField(usersAuthService, "adminRoleValues", "ADMIN,SUPER_ADMIN");

        loginRequest = new LoginRequest("admin@test.com", "password");
    }

    private String createJwt(String role) {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format("{\"sub\":\"123\",\"name\":\"Admin User\",\"iat\":1516239022,\"roles\":[\"USER\",\"%s\"]}", role);
        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        return encodedHeader + "." + encodedPayload + ".signature";
    }

    @Test
    @DisplayName("login - Debe retornar 200 OK si el login es exitoso y el usuario es ADMIN")
    void testLogin_Success_AdminUser() {
        // Arrange
        String adminJwt = createJwt("ADMIN");
        String responseBody = String.format("{\"access_token\":\"%s\"}", adminJwt);
        ResponseEntity<String> externalResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(externalResponse);

        // Act
        ResponseEntity<String> result = usersAuthService.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(responseBody, result.getBody());
    }

    @Test
    @DisplayName("login - Debe retornar 403 Forbidden si el login es exitoso pero el usuario no es ADMIN")
    void testLogin_Success_NonAdminUser() {
        // Arrange
        String userJwt = createJwt("USER");
        String responseBody = String.format("{\"token\":\"%s\"}", userJwt);
        ResponseEntity<String> externalResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(externalResponse);

        // Act
        ResponseEntity<String> result = usersAuthService.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertTrue(result.getBody().contains("forbidden: admin only"));
    }

    @Test
    @DisplayName("login - Debe retornar 403 Forbidden si el login es exitoso pero no hay token en la respuesta")
    void testLogin_Success_NoTokenInResponse() {
        // Arrange
        String responseBody = "{\"message\":\"Login successful\"}";
        ResponseEntity<String> externalResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(externalResponse);

        // Act
        ResponseEntity<String> result = usersAuthService.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    @DisplayName("login - Debe propagar el error si el servicio de usuarios retorna un error HTTP (e.g., 401)")
    void testLogin_ExternalServiceReturnsHttpError() {
        // Arrange
        String errorBody = "{\"error\":\"Invalid credentials\"}";
        HttpStatusCodeException exception = new HttpStatusCodeException(HttpStatus.UNAUTHORIZED, "Unauthorized", errorBody.getBytes(), null) {};

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        // Act
        ResponseEntity<String> result = usersAuthService.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(errorBody, result.getBody());
    }

    @Test
    @DisplayName("login - Debe retornar 502 Bad Gateway si el servicio de usuarios no está disponible")
    void testLogin_ExternalServiceUnavailable() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        // Act
        ResponseEntity<String> result = usersAuthService.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, result.getStatusCode());
        assertTrue(result.getBody().contains("users service unavailable"));
    }

    @Test
    @DisplayName("isAdmin - Debe retornar true para un rol de admin en un array")
    void testIsAdmin_RoleInArray() throws Exception {
        // Arrange
        String adminJwt = createJwt("ADMIN");

        // Act
        boolean isAdmin = (boolean) ReflectionTestUtils.invokeMethod(usersAuthService, "isAdmin", adminJwt);

        // Assert
        assertTrue(isAdmin);
    }

    @Test
    @DisplayName("isAdmin - Debe retornar true para un rol de admin como string simple")
    void testIsAdmin_RoleAsString() throws Exception {
        // Arrange
        String header = "{\"alg\":\"HS256\"}";
        String payload = "{\"role\":\"SUPER_ADMIN\"}";
        String jwt = Base64.getUrlEncoder().encodeToString(header.getBytes()) + "." +
                     Base64.getUrlEncoder().encodeToString(payload.getBytes()) + ".sig";

        // Act
        boolean isAdmin = (boolean) ReflectionTestUtils.invokeMethod(usersAuthService, "isAdmin", jwt);

        // Assert
        assertTrue(isAdmin);
    }
}