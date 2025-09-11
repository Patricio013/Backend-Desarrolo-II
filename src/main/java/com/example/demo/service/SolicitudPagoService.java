package com.example.demo.service;

import com.example.demo.client.PagoEnvioResponse;
import com.example.demo.client.PagosClient;
import com.example.demo.dto.SolicitudPagoCreateDTO;
import com.example.demo.dto.SolicitudPagoDTO;
import com.example.demo.entity.Notificaciones;
import com.example.demo.entity.SolicitudPago;
import com.example.demo.entity.enums.EstadoSolicitudPago;
import com.example.demo.repository.SolicitudPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SolicitudPagoService {

    private final SolicitudPagoRepository repo;
    private final PagosClient pagosClient;
    private final NotificacionesService notificacionesService; // ya lo tenés

    @Transactional
    public SolicitudPagoDTO crearYEnviar(SolicitudPagoCreateDTO in) {
        // Validación básica: al menos solicitudId u ordenId
        if (in.getSolicitudId() == null && in.getOrdenId() == null) {
            throw new IllegalArgumentException("Debe informarse solicitudId u ordenId");
        }

        // Map a entidad
        SolicitudPago sp = SolicitudPago.builder()
                .solicitudId(in.getSolicitudId())
                .ordenId(in.getOrdenId())
                .prestadorId(Objects.requireNonNull(in.getPrestadorId(), "prestadorId requerido"))
                .cotizacionId(in.getCotizacionId())
                .monto(in.getMonto())
                .concepto(Objects.requireNonNull(in.getConcepto(), "concepto requerido"))
                .vencimiento(in.getVencimiento())
                .estado(EstadoSolicitudPago.PENDIENTE)
                .build();

        sp = repo.save(sp);

        // Enviar al módulo de pagos
        PagoEnvioResponse resp = pagosClient.enviarSolicitudPago(toDTO(sp));

        if (resp.isAceptado()) {
            sp.setEstado(EstadoSolicitudPago.ENVIADA);
            sp.setExternoId(resp.getExternoId());
        } else {
            sp.setEstado(EstadoSolicitudPago.ERROR);
        }

        sp = repo.save(sp);

        // Notificación interna para otros módulos (tu entidad Notificaciones)
        String ref = sp.getCotizacionId() != null ? String.valueOf(sp.getCotizacionId())
                                                  : (sp.getSolicitudId() != null ? String.valueOf(sp.getSolicitudId())
                                                                                 : String.valueOf(sp.getId()));
        notificacionesService.crearNotificacion(
                Notificaciones.builder()
                        .cotizacionId(sp.getCotizacionId() != null ? sp.getCotizacionId() : sp.getId())
                        .titulo("Solicitud de pago " + sp.getEstado())
                        .mensaje("Pago por " + sp.getConcepto() + " (" + sp.getMonto()
                                + "), ref=" + ref + ", externoId=" + sp.getExternoId())
                        .leida(false)
                        .build()
        );

        return toDTO(sp);
    }

    public SolicitudPagoDTO toDTO(SolicitudPago sp) {
        return SolicitudPagoDTO.builder()
                .id(sp.getId())
                .solicitudId(sp.getSolicitudId())
                .ordenId(sp.getOrdenId())
                .prestadorId(sp.getPrestadorId())
                .cotizacionId(sp.getCotizacionId())
                .monto(sp.getMonto())
                .concepto(sp.getConcepto())
                .vencimiento(sp.getVencimiento())
                .estado(sp.getEstado())
                .externoId(sp.getExternoId())
                .createdAt(sp.getCreatedAt())
                .updatedAt(sp.getUpdatedAt())
                .build();
    }
}
