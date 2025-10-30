package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersAuthService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${users.service.base-url}")
    private String baseUrl;

    @Value("${users.service.login-path:/api/users/login}")
    private String loginPath;

    // Configurables con defaults seguros
    @Value("${auth.jwt.token-field-names:token,access_token,jwt,idToken}")
    private String tokenFieldNames;

    @Value("${auth.jwt.role-claim-names:role,roles,authorities}")
    private String roleClaimNames;

    @Value("${auth.jwt.admin-role-values:ADMIN}")
    private String adminRoleValues;

    public ResponseEntity<String> login(LoginRequest request) {
        String url = baseUrl + loginPath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // Si el login fue exitoso, verificamos rol admin en el JWT devuelto
            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                try {
                    String jwt = extractTokenFromBody(body);
                    if (jwt == null) {
                        log.warn("Login success but no token field found in body");
                        return forbidNonAdmin();
                    }
                    if (isAdmin(jwt)) {
                        HttpHeaders respHeaders = new HttpHeaders();
                        respHeaders.setContentType(MediaType.APPLICATION_JSON);
                        return new ResponseEntity<>(body, respHeaders, response.getStatusCode());
                    } else {
                        return forbidNonAdmin();
                    }
                } catch (Exception ex) {
                    log.warn("Failed to parse/validate JWT for admin-only login", ex);
                    return forbidNonAdmin();
                }
            }

            // Si no es 2xx, propagamos tal cual
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

    private ResponseEntity<String> forbidNonAdmin() {
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>("{\"error\":\"forbidden: admin only\"}", respHeaders, HttpStatus.FORBIDDEN);
    }

    private String extractTokenFromBody(String body) {
        if (body == null || body.isEmpty()) return null;
        try {
            JsonNode root = objectMapper.readTree(body);
            for (String field : splitCsv(tokenFieldNames)) {
                JsonNode node = root.get(field);
                if (node != null && node.isTextual()) {
                    String val = node.asText();
                    if (looksLikeJwt(val)) return val;
                }
            }
        } catch (Exception ignore) {
            // Si no es JSON, intentamos ver si el body es el token directamente
        }
        if (looksLikeJwt(body)) return body;
        return null;
    }

    private boolean isAdmin(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return false;
        String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
        JsonNode payload = objectMapper.readTree(payloadJson);

        Set<String> adminValues = toLowerSet(splitCsv(adminRoleValues));
        List<String> roleClaims = splitCsv(roleClaimNames);

        for (String claim : roleClaims) {
            JsonNode node = payload.get(claim);
            if (node == null || node.isNull()) continue;
            // String simple
            if (node.isTextual()) {
                if (adminValues.contains(node.asText().toLowerCase(Locale.ROOT))) return true;
            }
            // Array de strings
            if (node.isArray()) {
                for (JsonNode n : node) {
                    if (n.isTextual() && adminValues.contains(n.asText().toLowerCase(Locale.ROOT))) return true;
                }
            }
        }
        return false;
    }

    private static boolean looksLikeJwt(String s) {
        return s != null && s.chars().filter(c -> c == '.').count() == 2;
    }

    private static byte[] base64UrlDecode(String part) {
        String p = part;
        int rem = p.length() % 4;
        if (rem > 0) {
            p = p + "====".substring(rem);
        }
        return java.util.Base64.getUrlDecoder().decode(p);
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isEmpty()) return java.util.Collections.emptyList();
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String s : csv.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }

    private static Set<String> toLowerSet(List<String> list) {
        Set<String> set = new HashSet<>();
        for (String s : list) set.add(s.toLowerCase(Locale.ROOT));
        return set;
    }
}
