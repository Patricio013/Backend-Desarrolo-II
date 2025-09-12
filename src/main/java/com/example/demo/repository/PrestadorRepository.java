package com.example.demo.repository;

import com.example.demo.entity.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;


import java.util.List;

@Repository
public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    @Query("select distinct p from Prestador p join p.habilidades h where h.rubro.id = :rubroId")
    List<Prestador> findByRubroId(@Param("rubroId") Long rubroId);

    @Query(value = "SELECT DISTINCT p.* FROM prestador p " +
            "JOIN prestador_habilidad ph ON ph.prestador_id = p.id " +
            "JOIN habilidad h ON h.id = ph.habilidad_id " +
            "WHERE h.rubro_id = :rubroId", nativeQuery = true)
    List<Prestador> findByRubroIdNative(@Param("rubroId") Long rubroId);

    @Query(value = """
        SELECT p.*
        FROM prestador p
        JOIN prestador_habilidad ph ON ph.prestador_id = p.id
        JOIN habilidad h ON h.id = ph.habilidad_id
        LEFT JOIN prestador_calificacion pc ON pc.prestador_id = p.id
        WHERE h.rubro_id = :rubroId
          AND UPPER(p.estado) = 'ACTIVO'
        GROUP BY p.id
        ORDER BY
          (COALESCE(AVG(pc.puntuacion),0) / 5.0
            + 0.2 * LN(1 + COALESCE(p.trabajos_finalizados,0))) DESC,
          COALESCE(p.trabajos_finalizados,0) DESC,
          COALESCE(p.precio_hora, 1e12) ASC,
          p.apellido ASC,
          p.nombre ASC
        """, nativeQuery = true)
    List<Prestador> findTopByRubroRanked(@Param("rubroId") Long rubroId, Pageable pageable);

    @Query(value = """
      SELECT p.*
      FROM prestador p
      JOIN prestador_habilidad ph ON ph.prestador_id = p.id
      JOIN habilidad h ON h.id = ph.habilidad_id
      LEFT JOIN prestador_calificacion pc ON pc.prestador_id = p.id
      WHERE h.rubro_id = :rubroId
        AND UPPER(p.estado) = 'ACTIVO'
        AND p.id NOT IN (SELECT c.prestador_id FROM cotizacion c WHERE c.solicitud_id = :solicitudId)
      GROUP BY p.id
      ORDER BY
        (COALESCE(AVG(pc.puntuacion),0)/5.0 + 0.2*LN(1+COALESCE(p.trabajos_finalizados,0))) DESC,
        COALESCE(p.trabajos_finalizados,0) DESC,
        COALESCE(p.precio_hora,1e12) ASC,
        p.apellido ASC, p.nombre ASC
      """, nativeQuery = true)
    List<Prestador> findTopByRubroExcluyendoLosQueCotizaron(
        @Param("rubroId") Long rubroId,
        @Param("solicitudId") Long solicitudId,
        org.springframework.data.domain.Pageable pageable
    );

}
