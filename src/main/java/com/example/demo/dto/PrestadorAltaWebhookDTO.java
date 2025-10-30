package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrestadorAltaWebhookDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String direccion;
    private String estado;
    private String ciudad;
    private String calle;
    private String numero;
    private String piso;
    private String departamento;
    private String email;
    private String telefono;
    private Integer activo;
    private String dni;
    private String foto;
    private List<ZonaItem> zonas;
    private List<HabilidadItem> habilidades;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZonaItem {
        private Long id;
        private String nombre;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HabilidadItem {
        private Long id;
        private String nombre;
        private String descripcion;
        @JsonProperty("id_rubro")
        private Long idRubro;
        @JsonProperty("nombre_rubro")
        private String nombreRubro;
    }
}
