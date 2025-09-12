package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class RecibirCalificacionesDTO {
    private Long id;
    private List<Short> puntuaciones;
}
