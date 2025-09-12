package com.example.demo.dto;   

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SolicitudesCreadasDTO {

    @JsonProperty("solicitud_id")
    private Long solicitudId;

    @JsonProperty("usuario_id")
    private Long usuarioId;
 
    @JsonProperty("rubro")
    private String rubro;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("prestador_id")
    private Long prestadorId; // null => abierta

    @JsonProperty("preferencia_horaria")
    private PreferenciaHorariaDTO preferenciaHoraria;

    @Data
    public static class PreferenciaHorariaDTO {
        private String dia;      // "2025-09-12"
        private String ventana;  // "09:00-13:00"
    }
}
