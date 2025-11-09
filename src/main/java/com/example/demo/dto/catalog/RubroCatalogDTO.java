package com.example.demo.dto.catalog;

import com.example.demo.entity.Rubro;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RubroCatalogDTO {

    Long id;
    Long externalId;
    String nombre;

    public static RubroCatalogDTO fromEntity(Rubro rubro) {
        if (rubro == null) {
            return null;
        }
        return RubroCatalogDTO.builder()
                .id(rubro.getId())
                .externalId(rubro.getExternalId())
                .nombre(rubro.getNombre())
                .build();
    }
}
