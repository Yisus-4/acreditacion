package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.ModalidadParticipacionDocente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataSolicitudAsignacionCurricularRepository extends JpaRepository<SolicitudAsignacionCurricularJpaEntity, UUID> {

    List<SolicitudAsignacionCurricularJpaEntity> findByDesignacionIdOrderByCreadoEnAsc(UUID designacionId);

    List<SolicitudAsignacionCurricularJpaEntity> findByDesignacionIdInOrderByCreadoEnAsc(Collection<UUID> designacionIds);

    boolean existsByDesignacionIdAndActividadCurricularIdAndModalidadParticipacion(
            UUID designacionId,
            UUID actividadCurricularId,
            ModalidadParticipacionDocente modalidadParticipacion
    );
}
