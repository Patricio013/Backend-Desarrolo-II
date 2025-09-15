package com.example.demo.repository;

import com.example.demo.entity.Cotizacion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

  @Query("select c.prestador.id from Cotizacion c where c.solicitud.id = :solicitudId")
  List<Long> findPrestadorIdsQueCotizaron(@Param("solicitudId") Long solicitudId);

  boolean existsByPrestador_IdAndSolicitud_Id(Long prestadorId, Long solicitudId);

  Optional<Cotizacion> findByPrestador_IdAndSolicitud_Id(Long prestadorId, Long solicitudId);

  List<Cotizacion> findBySolicitud_Id(Long solicitudId);
}
