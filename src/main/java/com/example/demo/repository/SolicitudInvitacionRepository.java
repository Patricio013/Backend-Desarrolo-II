package com.example.demo.repository;

import com.example.demo.entity.SolicitudInvitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudInvitacionRepository extends JpaRepository<SolicitudInvitacion, Long> {

    List<SolicitudInvitacion> findBySolicitud_IdAndRound(Long solicitudId, int round);

    boolean existsBySolicitud_IdAndPrestador_IdAndRound(Long solicitudId, Long prestadorId, int round);

    @Query("select si.prestador.id from SolicitudInvitacion si where si.solicitud.id = :solicitudId and si.round = :round")
    List<Long> findPrestadorIdsBySolicitudAndRound(@Param("solicitudId") Long solicitudId, @Param("round") int round);
}

