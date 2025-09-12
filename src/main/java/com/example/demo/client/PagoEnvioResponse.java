package com.example.demo.client;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoEnvioResponse {
    private boolean aceptado;
    private String externoId; // id en el módulo de pagos
    private String mensaje;
}