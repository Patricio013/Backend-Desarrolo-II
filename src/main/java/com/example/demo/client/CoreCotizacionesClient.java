// com.example.demo.client.CoreCotizacionesClient.java
package com.example.demo.client;

import com.example.demo.dto.CotizacionesSubmit;

public interface CoreCotizacionesClient {
    boolean enviarCotizacion(CotizacionesSubmit dto);
}
