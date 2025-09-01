package com.example.demo.repository;

import com.example.demo.entity.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
