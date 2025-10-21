package com.example.demo.service;

import com.example.demo.entity.Solicitud; // TODO: ajusta al paquete real
import com.example.demo.repository.SolicitudRepository; // TODO
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class SolicitudesServiceTest {

    @MockBean
    private SolicitudRepository solicitudRepository;

    @MockBean
    private AmqpTemplate amqpTemplate; // mock de Rabbit

    // TODO: inyecta tu service real
    @org.springframework.beans.factory.annotation.Autowired
    private SolicitudesService solicitudesService;

    private Solicitud stub;

    @BeforeEach
    void setUp() {
        stub = new Solicitud();
        // TODO: setea campos mínimos válidos
        // stub.setId(1L);
        // stub.setTitulo("Demo");
        // stub.setFechaCreacion(OffsetDateTime.now());
    }

    @Test
    void crear_ok_persiste_y_publica_evento() {
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            // TODO: asignar ID simulado si hace falta
            return s;
        });

        var result = solicitudesService.crear(stub); // TODO: método real del service

        assertThat(result).isNotNull();
        verify(solicitudRepository, times(1)).save(any(Solicitud.class));
        verify(amqpTemplate, times(1)).convertAndSend(anyString(), any()); // según tu implementación

        // Capturar payload si querés validar
        ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);
        verify(amqpTemplate).convertAndSend(anyString(), cap.capture());
        assertThat(cap.getValue()).isNotNull();
    }

    @Test
    void obtenerPorId_ok() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(stub));
        var found = solicitudesService.obtenerPorId(1L); // TODO: método real
        assertThat(found).isPresent();
    }
}
