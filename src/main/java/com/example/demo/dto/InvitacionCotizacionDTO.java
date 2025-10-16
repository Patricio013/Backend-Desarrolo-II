package com.example.demo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionCotizacionDTO {
    private Long solicitudId;
    private Long rubroId;           // corresponde a categoriaId de la Solicitud
    private Long habilidadId;       // habilidad solicitada (externa)
    private Long prestadorId;
    private Long cotizacionId;
    private String prestadorNombre; // nombre + apellido del prestador
    private String mensaje;         // texto de la invitación
    private boolean enviado;        // resultado de la simulación
    private LocalDateTime timestamp;// momento del envío simulado
}
