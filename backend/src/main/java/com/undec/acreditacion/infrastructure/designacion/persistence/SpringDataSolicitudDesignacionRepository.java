package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.EstadoSolicitudDesignacion;
import com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    @Query("""
            SELECT s FROM SolicitudDesignacionJpaEntity s
            WHERE s.departamentoAcademico.id = :departamentoId
              AND s.tipoSolicitud = com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion.ANUAL
              AND s.periodoAcademico.anio < :anioDestino
              AND s.estado IN :estadosPermitidos
            ORDER BY s.periodoAcademico.anio DESC,
                     CASE WHEN s.estado = com.undec.acreditacion.domain.entities.EstadoSolicitudDesignacion.APROBADA THEN 0 ELSE 1 END ASC,
                     s.creadoEn DESC
            """)
    List<SolicitudDesignacionJpaEntity> findCandidatasAnualesOrigenClonacion(
            @Param("departamentoId") UUID departamentoId,
            @Param("anioDestino") Integer anioDestino,
            @Param("estadosPermitidos") Collection<EstadoSolicitudDesignacion> estadosPermitidos
    );
}
