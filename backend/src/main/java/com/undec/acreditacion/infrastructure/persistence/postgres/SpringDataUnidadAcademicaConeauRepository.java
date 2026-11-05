package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataUnidadAcademicaConeauRepository extends JpaRepository<UnidadAcademicaConeauJpaEntity, UUID> {

    List<UnidadAcademicaConeauJpaEntity> findByInstitucionId(UUID institucionId);

    List<UnidadAcademicaConeauJpaEntity> findByInstitucionIdAndActivaTrue(UUID institucionId);

    boolean existsByInstitucionIdAndDenominacionIgnoreCase(UUID institucionId, String denominacion);
}
