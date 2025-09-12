package com.example.demo.repository;

import com.example.demo.entity.Solicitud;
import com.example.demo.entity.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByUsuarioId(Long usuarioId);
    List<Solicitud> findByEstado(EstadoSolicitud estado);
    List<Solicitud> findByServicioId(Long servicioId);
    List<Solicitud> findByCategoriaId(Long categoriaId);
    List<Solicitud> findByPrestadorAsignadoId(Long prestadorAsignadoId);
    Optional <Solicitud> findById(Long id);
}
