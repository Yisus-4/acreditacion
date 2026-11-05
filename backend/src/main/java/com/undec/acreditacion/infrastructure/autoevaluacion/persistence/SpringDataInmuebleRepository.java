package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataInmuebleRepository extends JpaRepository<InmuebleJpaEntity, UUID> {

    @Query("SELECT DISTINCT i FROM InmuebleJpaEntity i " +
            "LEFT JOIN FETCH i.provincia " +
            "LEFT JOIN FETCH i.departamentoGeografico " +
            "LEFT JOIN FETCH i.localidad " +
            "LEFT JOIN FETCH i.carreras " +
            "WHERE i.activo = true " +
            "ORDER BY i.numeroOrden ASC NULLS LAST, i.nombre ASC")
    List<InmuebleJpaEntity> findAllActivosConRelaciones();

    @Query("SELECT DISTINCT i FROM InmuebleJpaEntity i " +
            "LEFT JOIN FETCH i.provincia " +
            "LEFT JOIN FETCH i.departamentoGeografico " +
            "LEFT JOIN FETCH i.localidad " +
            "LEFT JOIN FETCH i.carreras " +
            "WHERE i.esMaestro = true AND i.activo = true " +
            "ORDER BY i.numeroOrden ASC NULLS LAST, i.nombre ASC")
    List<InmuebleJpaEntity> findMaestrosActivos();

    @Query("SELECT DISTINCT i FROM InmuebleJpaEntity i " +
            "LEFT JOIN FETCH i.provincia " +
            "LEFT JOIN FETCH i.departamentoGeografico " +
            "LEFT JOIN FETCH i.localidad " +
            "LEFT JOIN FETCH i.carreras " +
            "WHERE i.procesoId = :procesoId AND i.activo = true " +
            "ORDER BY i.numeroOrden ASC NULLS LAST, i.nombre ASC")
    List<InmuebleJpaEntity> findByProcesoIdActivos(@Param("procesoId") UUID procesoId);

    @Query("SELECT DISTINCT i FROM InmuebleJpaEntity i " +
            "LEFT JOIN FETCH i.provincia " +
            "LEFT JOIN FETCH i.departamentoGeografico " +
            "LEFT JOIN FETCH i.localidad " +
            "LEFT JOIN FETCH i.carreras " +
            "LEFT JOIN FETCH i.espacios e " +
            "LEFT JOIN FETCH e.tipoEspacio " +
            "LEFT JOIN FETCH i.documentos " +
            "WHERE i.id = :id AND i.activo = true")
    Optional<InmuebleJpaEntity> findByIdCompleto(@Param("id") UUID id);
}
