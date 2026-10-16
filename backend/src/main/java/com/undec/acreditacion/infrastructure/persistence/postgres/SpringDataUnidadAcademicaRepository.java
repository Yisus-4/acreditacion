package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataUnidadAcademicaRepository extends JpaRepository<UnidadAcademicaJpaEntity, UUID> {

    List<UnidadAcademicaJpaEntity> findByInstitucionId(UUID institucionId);

    boolean existsByInstitucionIdAndNombre(UUID institucionId, String nombre);
}
