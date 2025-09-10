package com.example.demo.dto;
import java.util.List;

import com.example.demo.entity.Habilidad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestadorDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String direccion;
    private String estado;
    private Double precioHora;
    private Long zonaId;
    private List<Habilidad> habilidades;
    private List<Short> calificacion;
    private Integer trabajosFinalizados;
}
