package com.example.demo.repository;

import com.example.demo.entity.Rubro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RubroRepository extends JpaRepository<Rubro, Long> {

    Rubro findByNombre(String nombre);

    @Query(value = "select * from rubro r where r.external_id = :externalId", nativeQuery = true)
    java.util.Optional<Rubro> findByExternalId(@Param("externalId") Long externalId);
}

