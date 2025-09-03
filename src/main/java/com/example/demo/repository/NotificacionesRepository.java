package com.example.demo.repository;

import com.example.demo.entity.Notificaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionesRepository extends JpaRepository<Notificaciones, Long>{
    //List<Notificaciones> findByUsuarioId(Long usuario_id);
}