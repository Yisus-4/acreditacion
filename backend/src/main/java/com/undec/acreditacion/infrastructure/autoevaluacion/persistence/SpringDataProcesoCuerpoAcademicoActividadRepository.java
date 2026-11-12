package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProcesoCuerpoAcademicoActividadRepository extends JpaRepository<ProcesoCuerpoAcademicoActividadJpaEntity, UUID> {

    List<ProcesoCuerpoAcademicoActividadJpaEntity> findByProcesoId(UUID procesoId);

    List<ProcesoCuerpoAcademicoActividadJpaEntity> findByProcesoIdAndDocenteId(UUID procesoId, UUID docenteId);

    List<ProcesoCuerpoAcademicoActividadJpaEntity> findByPlanEstudioId(UUID planEstudioId);

    Optional<ProcesoCuerpoAcademicoActividadJpaEntity> findByProcesoIdAndDocenteIdAndPlanEstudioIdAndActividadCurricularId(
            UUID procesoId, UUID docenteId, UUID planEstudioId, UUID actividadCurricularId);
}
