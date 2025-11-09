package com.example.demo.dto.catalog;

import com.example.demo.entity.Zona;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ZonaCatalogDTO {

    Long id;
    Long externalId;
    String nombre;

    public static ZonaCatalogDTO fromEntity(Zona zona) {
        if (zona == null) {
            return null;
        }
        return ZonaCatalogDTO.builder()
                .id(zona.getId())
                .externalId(zona.getExternalId())
                .nombre(zona.getNombre())
                .build();
    }
}
