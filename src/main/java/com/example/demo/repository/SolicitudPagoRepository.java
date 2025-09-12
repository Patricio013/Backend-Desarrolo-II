package com.example.demo.repository;

import com.example.demo.entity.SolicitudPago;
import com.example.demo.entity.enums.EstadoSolicitudPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudPagoRepository extends JpaRepository<SolicitudPago, Long> {
    List<SolicitudPago> findTop50ByOrderByCreatedAtDesc();
    List<SolicitudPago> findByEstadoOrderByCreatedAtAsc(EstadoSolicitudPago estado);
}
