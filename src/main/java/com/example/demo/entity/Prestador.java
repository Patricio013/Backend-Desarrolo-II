package com.example.demo.entity;

import lombok.*;
import java.util.List;

import jakarta.persistence.*;

import java.util.ArrayList;

@Entity
@Table(name = "prestador")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Prestador {

    // ID interno autogenerado (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id")
    private Long internalId;

    // ID externo recibido en los mensajes
    @Column(name = "external_id", unique = true)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String direccion;

    @Column(nullable = false, length = 100)
    private String estado;

    @Column(nullable = false)
    private Double precioHora;

    @ManyToOne
    @JoinColumn(name = "zona_id")
    private Zona zona;

    @ManyToMany
    @JoinTable(
    name = "prestador_habilidad",
    joinColumns = @JoinColumn(name = "prestador_id"),
    inverseJoinColumns = @JoinColumn(name = "habilidad_id")
    )
    private final List<Habilidad> habilidades = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "prestador_calificacion",
        joinColumns = @JoinColumn(name = "prestador_id")
    )
    @Column(name = "puntuacion")
    @Builder.Default
    private List<Short> calificacion = new ArrayList<>();

    private Integer trabajosFinalizados;

    @ElementCollection
    @CollectionTable(
        name = "prestador_direccion",
        joinColumns = @JoinColumn(name = "prestador_id")
    )
    @Builder.Default
    private List<PrestadorDireccion> direcciones = new ArrayList<>();

    public Prestador(Long id) {
        this.id = id;
    }
}
