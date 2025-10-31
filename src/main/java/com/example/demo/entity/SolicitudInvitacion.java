package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "solicitud_invitacion",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_invitacion_solicitud_prestador_round",
            columnNames = {"solicitud_id", "prestador_id", "round"}
        )
    },
    indexes = {
        @Index(name = "ix_invitacion_solicitud", columnList = "solicitud_id"),
        @Index(name = "ix_invitacion_round", columnList = "round")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudInvitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Column(nullable = false)
    private int round;

    @Column(name = "enviado_at", nullable = false, columnDefinition = "timestamp default now()")
    private LocalDateTime enviadoAt;

    @Column(name = "rechazada", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean rechazada = false;

    @Column(name = "rechazada_at")
    private LocalDateTime rechazadaAt;

    @Column(name = "rechazo_motivo", columnDefinition = "TEXT")
    private String rechazoMotivo;

    @Column(name = "cotizacion_id_externo")
    private Long cotizacionIdExterno;
}
