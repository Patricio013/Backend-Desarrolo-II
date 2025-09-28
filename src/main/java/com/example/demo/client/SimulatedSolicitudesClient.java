package com.example.demo.client;

import com.example.demo.dto.SolicitudCotizacionesPut;
import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.SolicitudRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulatedSolicitudesClient {

    private final ObjectMapper om = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SimulatedSolicitudesClient.class);
    private final SolicitudRepository solicitudRepository;

    public SimulatedSolicitudesClient(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    public List<Solicitud> obtenerSolicitudesCreadas() {
        log.info("[Simulado] Consultando módulo externo por solicitudes en estado CREADA");
        return solicitudRepository.findByEstado(EstadoSolicitud.CREADA);
    }

    public boolean putCotizaciones(SolicitudCotizacionesPut payload) {
        try {
            String json = om.writeValueAsString(payload);
            log.info("[SIM-SOLICITUDES] PUT /solicitudes/{}/cotizaciones body={}",
                    payload.getIdsolicitud(), json);
            return true;
        } catch (Exception e) {
            log.error("[SIM-SOLICITUDES] Error serializando payload", e);
            return false;
        }
    }
}

