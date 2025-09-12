package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "cotizacion",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cotiz_prest_solic",
                        columnNames = {"prestador_id", "solicitud_id"}
                )
        },
        indexes = {
                @Index(name = "ix_cotiz_prestador", columnList = "prestador_id"),
                @Index(name = "ix_cotiz_solicitud", columnList = "solicitud_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(nullable = false)
    private Double valor;
}
