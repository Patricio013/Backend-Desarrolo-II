// com.example.demo.dto.CotizacionSubmitRequest.java
package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CotizacionesSubmit {
    @NotNull private Long solicitudId;
    @NotNull private Long prestadorId;
    @NotNull @Positive @Digits(integer = 16, fraction = 2)
    private BigDecimal monto; // ARS
}