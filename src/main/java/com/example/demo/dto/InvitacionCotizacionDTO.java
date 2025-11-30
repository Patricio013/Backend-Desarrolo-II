package com.example.demo.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionCotizacionDTO {
    private Long solicitudId;
    private Long usuarioId;
    private Long rubroId;           // corresponde a categoriaId de la Solicitud
    private Long habilidadId;       // habilidad solicitada (externa)
    private Long prestadorId;
    private Long cotizacionId;
    private String prestadorNombre; // nombre + apellido del prestador
    private LocalDate fecha;        // fecha preferida de ejecución
    private LocalTime horario;      // horario preferido
    private String direccionProvincia;
    private String direccionCiudad;
    private String direccionCalle;
    private String direccionNumero;
    private String direccionPiso;
    private String direccionDepto;
    private String direccionCodigoPostal;
    private String mensaje;         // texto de la invitación
    private boolean enviado;        // resultado de la simulación
    private LocalDateTime timestamp;// momento del envío simulado
}
