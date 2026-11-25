package com.undec.acreditacion.infrastructure.estandares.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataActividadCurricularEvaluacion22Repository
        extends JpaRepository<ActividadCurricularEvaluacion22JpaEntity, UUID> {

    List<ActividadCurricularEvaluacion22JpaEntity> findByActividadCurricularId(UUID actividadCurricularId);

    List<ActividadCurricularEvaluacion22JpaEntity> findByActividadCurricularIdIn(Collection<UUID> actividadCurricularIds);

    void deleteByActividadCurricularId(UUID actividadCurricularId);
}
