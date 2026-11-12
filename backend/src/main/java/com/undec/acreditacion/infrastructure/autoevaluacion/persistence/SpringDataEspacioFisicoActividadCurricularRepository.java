package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataEspacioFisicoActividadCurricularRepository extends JpaRepository<EspacioFisicoActividadCurricularJpaEntity, UUID> {

    List<EspacioFisicoActividadCurricularJpaEntity> findByEspacioFisicoId(UUID espacioFisicoId);

    List<EspacioFisicoActividadCurricularJpaEntity> findByPlanEstudioId(UUID planEstudioId);

    List<EspacioFisicoActividadCurricularJpaEntity> findByActividadCurricularId(UUID actividadCurricularId);

    List<EspacioFisicoActividadCurricularJpaEntity> findByCarreraId(UUID carreraId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByEspacioFisicoId(UUID espacioFisicoId);
}
