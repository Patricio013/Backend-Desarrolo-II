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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Builder.Default
    @OneToMany(mappedBy = "prestador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Calificacion> calificaciones = new ArrayList<>();

    public Prestador(Long id) {
        this.id = id;
    }
}