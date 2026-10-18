package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProcesoAcreditacionRepository extends JpaRepository<ProcesoAcreditacionJpaEntity, UUID>, JpaSpecificationExecutor<ProcesoAcreditacionJpaEntity> {

    Optional<ProcesoAcreditacionJpaEntity> findByCarreraIdAndEstadoIn(UUID carreraId, Collection<String> estados);

    @Query("SELECT COUNT(p) FROM ProcesoAcreditacionJpaEntity p WHERE p.codigo LIKE CONCAT('CONV-', :anio, '-%')")
    long countByAnio(@Param("anio") int anio);

    List<ProcesoAcreditacionJpaEntity> findAllByOrderByCreadoEnDesc();
}
