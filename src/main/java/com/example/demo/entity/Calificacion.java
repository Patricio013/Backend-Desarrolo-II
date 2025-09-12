package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "calificacion",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_calif_prest_solic_usuario",
                        columnNames = {"prestador_id", "solicitud_id", "usuario_id"})
        },
        indexes = {
                @Index(name = "ix_calif_prestador", columnList = "prestador_id"),
                @Index(name = "ix_calif_solicitud", columnList = "solicitud_id"),
                @Index(name = "ix_calif_usuario", columnList = "usuario_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Calificacion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Column(nullable = false) // De 1 a 5
    private Short puntuacion;

    @Lob
    private String comentario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}