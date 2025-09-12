package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="Notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificaciones{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String titulo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private Long cotizacionId;

    @Column(nullable = false)
    private Boolean leida;

    @PrePersist
    void prePersist() {
        if (fecha == null) fecha = LocalDateTime.now();
        if (leida == null) leida = false;
    }
}