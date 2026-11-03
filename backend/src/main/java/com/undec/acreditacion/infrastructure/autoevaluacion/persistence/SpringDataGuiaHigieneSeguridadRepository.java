package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataGuiaHigieneSeguridadRepository extends JpaRepository<GuiaHigieneSeguridadJpaEntity, UUID> {

    @Query("SELECT g FROM GuiaHigieneSeguridadJpaEntity g " +
            "LEFT JOIN FETCH g.documentos d " +
            "WHERE g.esMaestro = true")
    Optional<GuiaHigieneSeguridadJpaEntity> findGuiaMaestraCompleta();

    @Query("SELECT g FROM GuiaHigieneSeguridadJpaEntity g " +
            "LEFT JOIN FETCH g.documentos d " +
            "WHERE g.procesoId = :procesoId")
    Optional<GuiaHigieneSeguridadJpaEntity> findByProcesoIdCompleta(@Param("procesoId") UUID procesoId);

    @Query("SELECT g FROM GuiaHigieneSeguridadJpaEntity g " +
            "LEFT JOIN FETCH g.documentos d " +
            "WHERE g.id = :id")
    Optional<GuiaHigieneSeguridadJpaEntity> findByIdCompleta(@Param("id") UUID id);
}
