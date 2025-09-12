package com.example.demo.entity;

import com.example.demo.entity.enums.EstadoSolicitudPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "solicitud_pago",
    indexes = {
        @Index(name = "ix_pago_solicitud", columnList = "solicitud_id"),
        @Index(name = "ix_pago_orden", columnList = "orden_id"),
        @Index(name = "ix_pago_prestador", columnList = "prestador_id"),
        @Index(name = "ix_pago_estado", columnList = "estado")
    }
)
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class SolicitudPago {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Al menos UNO de estos dos debe venir (según tu flujo de negocio):
    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "orden_id")
    private Long ordenId;

    @Column(name = "prestador_id", nullable = false)
    private Long prestadorId;

    @Column(name = "cotizacion_id") // opcional: por si querés atar el pago a una cotización ganadora
    private Long cotizacionId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String concepto; // “Servicio finalizado #123”, etc.

    private LocalDateTime vencimiento;   // opcional

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudPago estado;

    @Column(name = "externo_id", length = 100)
    private String externoId; // id que devuelva el módulo de pagos

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (estado == null) estado = EstadoSolicitudPago.PENDIENTE;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
