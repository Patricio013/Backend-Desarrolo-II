package com.example.demo.client;

import java.util.*;

import org.springframework.stereotype.Component;

import com.example.demo.dto.CotizacionesSubmit;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SimulatedCoreCotizacionesClient implements CoreCotizacionesClient {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public boolean enviarCotizacion(CotizacionesSubmit dto) {
        Map<String, Object> payload = Map.of(
            "idsolicitud", dto.getSolicitudId(),
            "idprestador", dto.getPrestadorId(),
            "monto", dto.getMonto()
        );
        try {
            String json = om.writeValueAsString(payload);
            log.info("[SIM-CORE] POST /core/cotizaciones payload={}", json);
            return true; // simulación OK
        } catch (Exception e) {
            log.error("[SIM-CORE] Error serializando payload", e);
            return false;
        }
    }
}
