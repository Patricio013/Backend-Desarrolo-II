package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SolicitudPagoCreateDTO {

    // Enviá solicitudId O ordenId (al menos uno)
    private Long solicitudId;
    private Long ordenId;

    @NotNull
    private Long prestadorId;

    private Long cotizacionId; // opcional

    @NotNull @Digits(integer = 16, fraction = 2) @Positive
    private BigDecimal monto;

    @NotBlank
    private String concepto;

    private LocalDateTime vencimiento; // opcional
}
