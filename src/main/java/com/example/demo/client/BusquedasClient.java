// com.example.demo.client.BusquedasClient.java
package com.example.demo.client;

import com.example.demo.dto.SolicitudCotizacionesPut;

public interface BusquedasClient {
    boolean indexarSolicitudCotizaciones(SolicitudCotizacionesPut payload);
}
