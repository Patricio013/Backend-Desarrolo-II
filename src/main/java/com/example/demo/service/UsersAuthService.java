package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersAuthService {

    private final RestTemplate restTemplate;

    @Value("${users.service.base-url}")
    private String baseUrl;

    @Value("${users.service.login-path:/api/users/login}")
    private String loginPath;

    public ResponseEntity<String> login(LoginRequest request) {
        String url = baseUrl + loginPath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            HttpHeaders respHeaders = new HttpHeaders();
            respHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(response.getBody(), respHeaders, response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            log.warn("Users login error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            HttpHeaders respHeaders = new HttpHeaders();
            respHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(e.getResponseBodyAsString(), respHeaders, e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Users login timeout/connection error", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"users service unavailable\"}");
        }
    }
}

