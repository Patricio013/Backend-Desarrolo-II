package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudAsignarDTO {
    @NotNull
    private Long solicitudId;

    @NotNull
    private Long prestadorId;

    // Si se envía, debe coincidir con el total cotizado. Si es null, se toma el total cotizado.
    @Positive
    @Digits(integer = 16, fraction = 2)
    private BigDecimal monto;

    private String concepto; // opcional: si no viene, se genera uno por defecto

    private LocalDateTime vencimiento; // opcional
}
