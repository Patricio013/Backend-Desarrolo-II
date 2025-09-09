package com.example.demo.dto;

import org.antlr.v4.runtime.misc.NotNull;

import com.example.demo.entity.enums.EstadoSolicitud;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SolicitudesCreadasDTO {
        
    @JsonProperty("usuario_id")
    private Long usuarioId;
    
    @JsonProperty("descripcion")
    private String descripcion;
    
    @JsonProperty("estado")
    private EstadoSolicitud estado;
    
    @JsonProperty("prestador_asignado_id")
    private Long prestadorAsignadoId;

}
