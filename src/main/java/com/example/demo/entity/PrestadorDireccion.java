package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestadorDireccion {

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "number", length = 30)
    private String number;

    @Column(name = "floor", length = 30)
    private String floor;

    @Column(name = "apartment", length = 30)
    private String apartment;
}

