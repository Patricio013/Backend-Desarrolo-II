package com.example.demo.repository;

import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByUsuarioId(Long usuarioId);
    List<Solicitud> findByEstado(EstadoSolicitud estado);
    List<Solicitud> findByPrestadorAsignadoId(Long prestadorAsignadoId);
    @Query("""
        SELECT s FROM Solicitud s
        WHERE s.prestadorAsignadoId = :prestadorId
        AND s.preferenciaDia = :dia
        AND (s.preferenciaDesde <= :hasta AND s.preferenciaHasta >= :desde)
        AND (s.estado = 'ASIGNADA' OR s.estado = 'EN_PROGRESO')
    """)
    List<Solicitud> findAsignadasEnDiaYFranja(Long prestadorId, LocalDate dia,
                                            LocalTime desde, LocalTime hasta);

}
