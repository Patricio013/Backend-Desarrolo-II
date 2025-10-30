package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zona")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID externo recibido en los mensajes
    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(nullable = false, length = 100)
    private String nombre;
}
