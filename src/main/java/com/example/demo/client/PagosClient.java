package com.example.demo.client;

import com.example.demo.dto.SolicitudPagoDTO;

public interface PagosClient {
    PagoEnvioResponse enviarSolicitudPago(SolicitudPagoDTO pago);
}
