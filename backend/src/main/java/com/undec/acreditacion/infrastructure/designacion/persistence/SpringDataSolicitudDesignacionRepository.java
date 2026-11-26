package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSolicitudDesignacionRepository extends JpaRepository<SolicitudDesignacionJpaEntity, UUID> {

    List<SolicitudDesignacionJpaEntity> findAllByOrderByCreadoEnDesc();

    List<SolicitudDesignacionJpaEntity> findByPeriodoAcademicoIdOrderByCreadoEnDesc(UUID periodoAcademicoId);

    List<SolicitudDesignacionJpaEntity> findByDepartamentoAcademicoIdOrderByCreadoEnDesc(UUID departamentoAcademicoId);

    List<SolicitudDesignacionJpaEntity> findByDepartamentoAcademicoIdAndPeriodoAcademicoIdOrderByCreadoEnDesc(
            UUID departamentoAcademicoId,
            UUID periodoAcademicoId
    );

    Optional<SolicitudDesignacionJpaEntity> findByDepartamentoAcademicoIdAndPeriodoAcademicoIdAndTipoSolicitud(
            UUID departamentoAcademicoId,
            UUID periodoAcademicoId,
            TipoSolicitudDesignacion tipoSolicitud
    );
}
