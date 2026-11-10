package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProgramaAnaliticoAsignaturaRepository extends JpaRepository<ProgramaAnaliticoAsignaturaJpaEntity, UUID> {

    List<ProgramaAnaliticoAsignaturaJpaEntity> findByProcesoIdOrderByNombreAsignaturaAsc(UUID procesoId);

    Optional<ProgramaAnaliticoAsignaturaJpaEntity> findByProcesoIdAndNombreAsignatura(UUID procesoId, String nombreAsignatura);

    @Query("SELECT p FROM ProgramaAnaliticoAsignaturaJpaEntity p WHERE p.actividadCurricular.id = :actividadCurricularId")
    Optional<ProgramaAnaliticoAsignaturaJpaEntity> findByActividadCurricularId(@Param("actividadCurricularId") UUID actividadCurricularId);

    @Query("SELECT p FROM ProgramaAnaliticoAsignaturaJpaEntity p WHERE p.actividadCurricular.planEstudio.id = :planEstudioId ORDER BY p.nombreAsignatura ASC")
    List<ProgramaAnaliticoAsignaturaJpaEntity> findByPlanEstudioId(@Param("planEstudioId") UUID planEstudioId);

    long countByProcesoId(UUID procesoId);

    long countByProcesoIdAndEstado(UUID procesoId, String estado);
}
