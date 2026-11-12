package com.undec.acreditacion.infrastructure.datoscentrales.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataCuerpoAcademicoActividadMaestroRepository extends JpaRepository<CuerpoAcademicoActividadMaestroJpaEntity, UUID> {

    List<CuerpoAcademicoActividadMaestroJpaEntity> findByDocenteId(UUID docenteId);

    List<CuerpoAcademicoActividadMaestroJpaEntity> findByPlanEstudioId(UUID planEstudioId);

    List<CuerpoAcademicoActividadMaestroJpaEntity> findByActividadCurricularId(UUID actividadCurricularId);

    List<CuerpoAcademicoActividadMaestroJpaEntity> findByCarreraId(UUID carreraId);
}
