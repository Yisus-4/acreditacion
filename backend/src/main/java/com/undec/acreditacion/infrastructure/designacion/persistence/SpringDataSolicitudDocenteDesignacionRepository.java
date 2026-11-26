package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.EstadoItemDesignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataSolicitudDocenteDesignacionRepository extends JpaRepository<SolicitudDocenteDesignacionJpaEntity, UUID> {

    List<SolicitudDocenteDesignacionJpaEntity> findBySolicitudIdOrderByCreadoEnAsc(UUID solicitudId);

    long countBySolicitudIdAndEstadoItemNot(UUID solicitudId, EstadoItemDesignacion estadoItem);

    @Query("""
            SELECT d FROM SolicitudDocenteDesignacionJpaEntity d
            WHERE d.docente.id = :docenteId
              AND d.solicitud.periodoAcademico.id = :periodoId
              AND d.estadoItem <> com.undec.acreditacion.domain.entities.EstadoItemDesignacion.EXCLUIDO
              AND NOT EXISTS (
                  SELECT 1 FROM SolicitudDocenteDesignacionJpaEntity r
                  WHERE r.designacionOrigenId = d.id
                    AND r.solicitud.periodoAcademico.id = :periodoId
                    AND r.estadoItem <> com.undec.acreditacion.domain.entities.EstadoItemDesignacion.EXCLUIDO
              )
            """)
    List<SolicitudDocenteDesignacionJpaEntity> findActivasByDocenteAndPeriodo(
            @Param("docenteId") UUID docenteId,
            @Param("periodoId") UUID periodoId
    );
}
