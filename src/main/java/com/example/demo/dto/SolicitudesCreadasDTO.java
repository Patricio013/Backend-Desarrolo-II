package com.example.demo.dto;   

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolicitudesCreadasDTO {

    @JsonProperty("solicitud_id")
    private Long solicitudId;

    @JsonProperty("usuario_id")
    private Long usuarioId;
 
    @JsonAlias({"rubro", "rubroId"})
    private Long rubro;

    // Nuevo: ID de habilidad (externo)
    @JsonProperty("habilidad_id")
    private Long habilidadId;

    @JsonProperty("descripcion")
    private String descripcion;

    // Nuevo: título
    @JsonProperty("titulo")
    private String titulo;

    @JsonProperty("prestador_id")
    private Long prestadorId; // null => abierta

    @JsonProperty("fue_cotizada")
    private Boolean fueCotizada;

    @JsonProperty("es_critica")
    private Boolean esCritica;
    // Nuevo alias: es_urgente -> esCritica
    @JsonProperty("es_urgente")
    private Boolean esUrgente;

    // Nuevo: fecha y horario
    @JsonProperty("fecha")
    private String fecha;   // yyyy-MM-dd
    @JsonProperty("horario")
    private String horario; // HH:mm

    // Opcional: estado como string
    @JsonProperty("estado")
    private String estado;

    @JsonProperty("direccion")
    private DireccionDTO direccion;

    @JsonProperty("preferencia_horaria")
    private PreferenciaHorariaDTO preferenciaHoraria;

    @Data
    public static class PreferenciaHorariaDTO {
        private String dia;      // "2025-09-12"
        private String ventana;  // "09:00-13:00"
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DireccionDTO {
        private Long id;

        @JsonAlias({"provincia", "state"})
        private String provincia;

        @JsonAlias({"ciudad", "city"})
        private String ciudad;

        @JsonAlias({"calle", "street"})
        private String calle;

        @JsonAlias({"numero", "number"})
        private String numero;

        @JsonAlias({"piso", "floor"})
        private String piso;

        @JsonAlias({"depto", "apartment"})
        private String depto;

        @JsonAlias({"codigo_postal", "zip", "postalCode"})
        private String codigoPostal;
    }
}
