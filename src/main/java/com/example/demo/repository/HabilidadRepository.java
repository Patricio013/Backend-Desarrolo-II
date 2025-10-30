package com.example.demo.repository;

import com.example.demo.entity.Habilidad;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HabilidadRepository extends JpaRepository<Habilidad, Long> {

    @Query("SELECT h FROM Habilidad h WHERE LOWER(h.nombre)=LOWER(:nombre) AND h.rubro.externalId=:rubroExternalId")
    Optional<Habilidad> findByNombreAndRubro(@Param("nombre") String nombre, @Param("rubroExternalId") Long rubroExternalId);

    @Query(value = "select * from habilidad h where h.external_id = :externalId", nativeQuery = true)
    Optional<Habilidad> findByExternalId(@Param("externalId") Long externalId);
}

