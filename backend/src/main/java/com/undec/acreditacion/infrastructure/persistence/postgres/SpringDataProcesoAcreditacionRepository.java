package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProcesoAcreditacionRepository extends JpaRepository<ProcesoAcreditacionJpaEntity, UUID> {

    Optional<ProcesoAcreditacionJpaEntity> findByCarreraIdAndEstadoIn(UUID carreraId, Collection<String> estados);

    @Query("SELECT COUNT(p) FROM ProcesoAcreditacionJpaEntity p WHERE p.codigo LIKE CONCAT('CONV-', :anio, '-%')")
    long countByAnio(@Param("anio") int anio);

    @Query("SELECT p FROM ProcesoAcreditacionJpaEntity p WHERE " +
            "(:carreraId IS NULL OR p.carreraId = :carreraId) AND " +
            "(:estado IS NULL OR p.estado = :estado) AND " +
            "(:tipoProceso IS NULL OR p.tipoProceso = :tipoProceso) AND " +
            "(:anio IS NULL OR p.codigo LIKE CONCAT('CONV-', :anio, '-%')) " +
            "ORDER BY p.creadoEn DESC")
    List<ProcesoAcreditacionJpaEntity> findAllByFiltro(
            @Param("carreraId") UUID carreraId,
            @Param("estado") String estado,
            @Param("tipoProceso") String tipoProceso,
            @Param("anio") Integer anio
    );
}
