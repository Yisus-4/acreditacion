package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProgramaAnaliticoAsignaturaRepository extends JpaRepository<ProgramaAnaliticoAsignaturaJpaEntity, UUID> {

    List<ProgramaAnaliticoAsignaturaJpaEntity> findByProcesoIdOrderByNombreAsignaturaAsc(UUID procesoId);

    Optional<ProgramaAnaliticoAsignaturaJpaEntity> findByProcesoIdAndNombreAsignatura(UUID procesoId, String nombreAsignatura);

    long countByProcesoId(UUID procesoId);

    long countByProcesoIdAndEstado(UUID procesoId, String estado);
}
