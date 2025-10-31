package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HabilidadAltaWebhookDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    @JsonProperty("id_rubro")
    private Long idRubro;
    private Integer activo;
}
