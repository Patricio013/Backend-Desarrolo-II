package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrestadorAltaWebhookDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String direccion;
    private String email;
    private String telefono;
    private Integer activo;
}
