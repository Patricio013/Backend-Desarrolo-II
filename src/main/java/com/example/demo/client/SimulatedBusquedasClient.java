// com.example.demo.client.SimulatedBusquedasClient.java
package com.example.demo.client;

import com.example.demo.dto.SolicitudCotizacionesPut;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulatedBusquedasClient implements BusquedasClient {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public boolean indexarSolicitudCotizaciones(SolicitudCotizacionesPut payload) {
        try {
            String json = om.writeValueAsString(payload);
            log.info("[SIM-BUSQUEDAS] POST /index/solicitudes body={}", json);
            return true;
        } catch (Exception e) {
            log.error("[SIM-BUSQUEDAS] Error serializando payload", e);
            return false;
        }
    }
}
