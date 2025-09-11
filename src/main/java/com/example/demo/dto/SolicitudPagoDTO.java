package com.example.demo.dto;

import com.example.demo.entity.enums.EstadoSolicitudPago;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class SolicitudPagoDTO {
    private Long id;
    private Long solicitudId;
    private Long ordenId;
    private Long prestadorId;
    private Long cotizacionId;

    private BigDecimal monto;
    private String concepto;

    private LocalDateTime vencimiento;
    private EstadoSolicitudPago estado;

    private String externoId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

