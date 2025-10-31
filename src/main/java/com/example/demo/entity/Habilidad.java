package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "habilidad")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Habilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID externo recibido en los mensajes
    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rubro_id", nullable = false)
    private Rubro rubro;


}
