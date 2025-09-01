package com.example.demo.client;

import com.example.demo.dto.InvitacionCotizacionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedCotizacionClient {

    private static final Logger log = LoggerFactory.getLogger(SimulatedCotizacionClient.class);

    public boolean enviarInvitacion(InvitacionCotizacionDTO dto) {
        // Simulación de llamada a sistema externo: log + retorno exitoso
        log.info("[Simulado] Invitando prestador {} a cotizar solicitud {} (rubro {}) - {}",
                dto.getPrestadorId(), dto.getSolicitudId(), dto.getRubroId(), dto.getMensaje());
        return true;
    }
}

