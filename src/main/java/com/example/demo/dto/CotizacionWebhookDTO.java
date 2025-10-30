package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CotizacionWebhookDTO {
    private Long id;
    private String estado;
    private String descripcion;
    private Double tarifa;
    @JsonProperty("fecha_creacion")
    private String fechaCreacion;
    @JsonProperty("fecha_ultima_actualizacion")
    private String fechaUltimaActualizacion;
    private String fecha;
    @JsonProperty("id_prestador")
    private Long idPrestador;
    @JsonProperty("id_usuario")
    private Long idUsuario;
    @JsonProperty("id_pedido")
    private Long idPedido;
    @JsonProperty("id_habilidad")
    private Long idHabilidad;
    @JsonProperty("es_critico")
    private Integer esCritico;
}
