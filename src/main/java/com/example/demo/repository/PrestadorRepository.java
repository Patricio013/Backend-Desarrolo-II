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

    @Query(value = "select * from prestador p where p.external_id = :externalId", nativeQuery = true)
    java.util.Optional<Prestador> findByExternalId(@Param("externalId") Long externalId);

    @Query("select distinct p from Prestador p join p.habilidades h where h.rubro.id = :rubroId")
    List<Prestador> findByRubroId(@Param("rubroId") Long rubroId);

    @Query(value = "SELECT DISTINCT p.* FROM prestador p " +
            "JOIN prestador_habilidad ph ON ph.prestador_id = p.internal_id " +
            "JOIN habilidad h ON h.id = ph.habilidad_id " +
            "JOIN rubro r ON r.id = h.rubro_id " +
            "WHERE r.external_id = :rubroExternalId", nativeQuery = true)
    List<Prestador> findByRubroIdNative(@Param("rubroExternalId") Long rubroExternalId);

    @Query(value = """
        SELECT p.*
        FROM prestador p
        JOIN prestador_habilidad ph ON ph.prestador_id = p.internal_id
        JOIN habilidad h ON h.id = ph.habilidad_id
        JOIN rubro r ON r.id = h.rubro_id
        LEFT JOIN prestador_calificacion pc ON pc.prestador_id = p.internal_id
        WHERE r.external_id = :rubroExternalId
          AND UPPER(p.estado) = 'ACTIVO'
        GROUP BY p.internal_id
        ORDER BY
          (COALESCE(AVG(pc.puntuacion),0) / 5.0
            + 0.2 * LN(1 + COALESCE(p.trabajos_finalizados,0))) DESC,
          COALESCE(p.trabajos_finalizados,0) DESC,
          COALESCE(p.precio_hora, 1e12) ASC,
          p.apellido ASC,
          p.nombre ASC
        """, nativeQuery = true)
    List<Prestador> findTopByRubroRanked(@Param("rubroExternalId") Long rubroExternalId, Pageable pageable);

    @Query(value = """
      SELECT p.*
      FROM prestador p
      JOIN prestador_habilidad ph ON ph.prestador_id = p.internal_id
      JOIN habilidad h ON h.id = ph.habilidad_id
      JOIN rubro r ON r.id = h.rubro_id
      LEFT JOIN prestador_calificacion pc ON pc.prestador_id = p.internal_id
      WHERE r.external_id = :rubroExternalId
        AND UPPER(p.estado) = 'ACTIVO'
        AND p.internal_id NOT IN (
          SELECT c.prestador_id FROM cotizacion c
          JOIN solicitud s ON s.internal_id = c.solicitud_id
          WHERE s.external_id = :solicitudExternalId
        )
      GROUP BY p.internal_id
      ORDER BY
        (COALESCE(AVG(pc.puntuacion),0)/5.0 + 0.2*LN(1+COALESCE(p.trabajos_finalizados,0))) DESC,
        COALESCE(p.trabajos_finalizados,0) DESC,
        COALESCE(p.precio_hora,1e12) ASC,
        p.apellido ASC, p.nombre ASC
      """, nativeQuery = true)
    List<Prestador> findTopByRubroExcluyendoLosQueCotizaron(
        @Param("rubroExternalId") Long rubroExternalId,
        @Param("solicitudExternalId") Long solicitudExternalId,
        org.springframework.data.domain.Pageable pageable
    );

}
