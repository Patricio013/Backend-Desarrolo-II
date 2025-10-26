package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestadorDireccionDTO {
    private String state;
    private String city;
    private String street;
    private String number;
    private String floor;
    private String apartment;
}

