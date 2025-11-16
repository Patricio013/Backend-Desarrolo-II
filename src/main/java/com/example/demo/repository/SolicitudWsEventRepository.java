package com.example.demo.repository;

import com.example.demo.entity.SolicitudWsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudWsEventRepository extends JpaRepository<SolicitudWsEvent, Long> {

    List<SolicitudWsEvent> findTop200ByOrderByCreatedAtDesc();
}
