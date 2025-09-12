package com.example.demo.client;

import com.example.demo.dto.SolicitudPagoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SimulatedPagosClient implements PagosClient {

    @Override
    public PagoEnvioResponse enviarSolicitudPago(SolicitudPagoDTO pago) {
        log.info("[SIM-PAGOS] Enviando solicitud de pago: {}", pago);
        // Simulación OK
        return PagoEnvioResponse.builder()
                .aceptado(true)
                .externoId("PAY-" + UUID.randomUUID())
                .mensaje("OK")
                .build();
    }
}
