package com.example.demo.entity.enums;

public enum EstadoSolicitudPago {
    PENDIENTE,   // creada en nuestro módulo, aún no enviada
    ENVIADA,     // enviada al módulo de pagos
    APROBADA,    // confirmado por módulo de pagos (callback u operación manual)
    RECHAZADA,   // rechazado por módulo de pagos
    ERROR        // error al enviar
}
