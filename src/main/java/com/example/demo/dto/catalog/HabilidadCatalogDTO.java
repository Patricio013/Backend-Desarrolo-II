package com.example.demo.dto.catalog;

import com.example.demo.entity.Habilidad;
import com.example.demo.entity.Rubro;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HabilidadCatalogDTO {

    Long id;
    Long externalId;
    String nombre;
    RubroSummary rubro;

    public static HabilidadCatalogDTO fromEntity(Habilidad habilidad) {
        if (habilidad == null) {
            return null;
        }
        return HabilidadCatalogDTO.builder()
                .id(habilidad.getId())
                .externalId(habilidad.getExternalId())
                .nombre(habilidad.getNombre())
                .rubro(RubroSummary.from(habilidad.getRubro()))
                .build();
    }

    @Value
    @Builder
    public static class RubroSummary {
        Long id;
        Long externalId;
        String nombre;

        static RubroSummary from(Rubro rubro) {
            if (rubro == null) {
                return null;
            }
            return RubroSummary.builder()
                    .id(rubro.getId())
                    .externalId(rubro.getExternalId())
                    .nombre(rubro.getNombre())
                    .build();
        }
    }
}
