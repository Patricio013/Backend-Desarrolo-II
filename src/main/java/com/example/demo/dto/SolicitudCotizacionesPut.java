// com.example.demo.dto.SolicitudCotizacionesPut.java
package com.example.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudCotizacionesPut {
    // claves EXACTAS pedidas
    private Long idsolicitud;
    private List<Item> cotizaciones;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private Long idprestador;
        private BigDecimal monto;
    }
}
