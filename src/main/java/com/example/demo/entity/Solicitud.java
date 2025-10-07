package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.demo.entity.enums.EstadoSolicitud;

@Entity
@Table(name = "solicitud")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitud {

    @Id
    private Long id; // este es el "solicitud_id" que viene del Core

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Long rubroId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private Long prestadorAsignadoId;

    // Preferencia horaria normalizada
    private LocalDate preferenciaDia;
    private LocalTime preferenciaDesde;
    private LocalTime preferenciaHasta;

    @Column(length = 20)
    private String preferenciaVentanaStr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.CREADA;

    @Column(name = "fue_cotizada", nullable = false)
    @Builder.Default
    private boolean fueCotizada = false;

    @Column(name = "es_critica", nullable = false)
    @Builder.Default
    private boolean esCritica = false;

    @Column(name = "cotizacion_round", nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private int cotizacionRound = 1;

    @Column(name = "cotizacion_round_started_at", nullable = false, columnDefinition = "timestamp default now()")
    private LocalDateTime cotizacionRoundStartedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoSolicitud.CREADA;
        }
        if (this.cotizacionRound <= 0) {
            this.cotizacionRound = 1;
        }
        if (this.cotizacionRoundStartedAt == null) {
            this.cotizacionRoundStartedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
