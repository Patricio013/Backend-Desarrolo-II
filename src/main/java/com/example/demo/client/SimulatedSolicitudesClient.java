package com.example.demo.client;

import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import com.example.demo.repository.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulatedSolicitudesClient {

    private static final Logger log = LoggerFactory.getLogger(SimulatedSolicitudesClient.class);
    private final SolicitudRepository solicitudRepository;

    public SimulatedSolicitudesClient(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    public List<Solicitud> obtenerSolicitudesCreadas() {
        log.info("[Simulado] Consultando módulo externo por solicitudes en estado CREADA");
        return solicitudRepository.findByEstado(EstadoSolicitud.CREADA);
    }
}

