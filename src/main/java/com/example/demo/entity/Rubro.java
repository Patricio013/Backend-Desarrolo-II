package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rubro")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Rubro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID externo recibido en los mensajes
    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(nullable = false, length = 100)
    private String nombre;

}
