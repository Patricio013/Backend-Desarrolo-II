// com.example.demo.service.CotizacionService.java
package com.example.demo.service;

import com.example.demo.client.CoreCotizacionesClient;
import com.example.demo.client.BusquedasClient;       // <-- comentá si no lo tenés
import com.example.demo.dto.CotizacionesSubmit;
import com.example.demo.client.SimulatedSolicitudesClient;
import com.example.demo.dto.SolicitudCotizacionesPut; // <-- comentá si no lo tenés
import com.example.demo.entity.Cotizacion;
//import com.example.demo.entity.Notificaciones;        // <-- comentá si no lo usás
import com.example.demo.repository.CotizacionRepository;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final PrestadorRepository prestadorRepository;
    private final SolicitudRepository solicitudRepository;
    private final CoreCotizacionesClient coreClient;
    private final SimulatedSolicitudesClient solicitudesClient;
    private final BusquedasClient busquedasClient;
    //private final NotificacionesService notificacionesService;


    @Transactional
    public void recibirCotizacion(CotizacionesSubmit in) {

        var prestador = prestadorRepository.findById(in.getPrestadorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Prestador no encontrado: " + in.getPrestadorId()));

        var solicitud = solicitudRepository.findById(in.getSolicitudId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada: " + in.getSolicitudId()));

        var existente = cotizacionRepository
                .findByPrestador_IdAndSolicitud_Id(in.getPrestadorId(), in.getSolicitudId());

        Cotizacion cotizacion;
        boolean created;
        if (existente.isPresent()) {
            cotizacion = existente.get();
            cotizacion.setValor(in.getMonto().doubleValue());
            created = false;
        } else {
            cotizacion = Cotizacion.builder()
                    .prestador(prestador)
                    .solicitud(solicitud)
                    .valor(in.getMonto().doubleValue())
                    .build();
            created = true;
        }
        cotizacion = cotizacionRepository.save(cotizacion);

        // (1) Enviar al Core con JSON mínimo {idsolicitud,idprestador,monto}
        coreClient.enviarCotizacion(
                CotizacionesSubmit.builder()
                        .solicitudId(solicitud.getId())
                        .prestadorId(prestador.getId())
                        .monto(in.getMonto())
                        .build()
        );

        // ===== Si aún no tenés los clientes abajo, podés comentar todo este bloque =====
        // (2) Armar payload con TODAS las cotizaciones de la solicitud
        List<SolicitudCotizacionesPut.Item> items = cotizacionRepository.findBySolicitud_Id(solicitud.getId())
                .stream()
                .map(c -> SolicitudCotizacionesPut.Item.builder()
                        .idprestador(c.getPrestador().getId())
                        .monto(BigDecimal.valueOf(c.getValor()))
                        .build())
                .collect(Collectors.toList());

        SolicitudCotizacionesPut payload = SolicitudCotizacionesPut.builder()
                .idsolicitud(solicitud.getId())
                .cotizaciones(items)
                .build();

        // (3) PUT a Solicitudes
        solicitudesClient.putCotizaciones(payload);

        // (4) Enviar a módulo de Búsquedas
        busquedasClient.indexarSolicitudCotizaciones(payload);

        // (5) Notificación interna (opcional)
        //notificacionesService.crearNotificacion(
        //Notificaciones.builder()
        //    .cotizacionId(cotizacion.getId())
        //    .titulo(created ? "Cotización recibida" : "Cotización actualizada")
        //    .mensaje("idsolicitud=" + solicitud.getId()
        //            + ", idprestador=" + prestador.getId()
        //            + ", monto=" + in.getMonto())
        //    .fecha(java.time.LocalDateTime.now())  // <-- acá el cambio
        //    .leida(false)
        //    .build()
        //);
        // ===============================================================================
    }
}
