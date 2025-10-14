package com.example.demo.repository;

import com.example.demo.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ZonaRepository extends JpaRepository<Zona, Long> {
    @Query(value = "select * from zona z where z.external_id = :externalId", nativeQuery = true)
    java.util.Optional<Zona> findByExternalId(@Param("externalId") Long externalId);
}

