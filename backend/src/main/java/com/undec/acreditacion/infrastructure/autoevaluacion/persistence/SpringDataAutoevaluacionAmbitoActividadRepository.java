package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAutoevaluacionAmbitoActividadRepository
        extends JpaRepository<AutoevaluacionAmbitoActividadJpaEntity, UUID> {

    List<AutoevaluacionAmbitoActividadJpaEntity> findByAutoevaluacionAmbitoId(UUID autoevaluacionAmbitoId);

    List<AutoevaluacionAmbitoActividadJpaEntity> findByAutoevaluacionAmbitoIdIn(Collection<UUID> autoevaluacionAmbitoIds);

    @Query("""
            SELECT aaa FROM AutoevaluacionAmbitoActividadJpaEntity aaa
            JOIN FETCH aaa.autoevaluacionAmbito aca
            JOIN FETCH aca.ambito ap
            WHERE aaa.actividadCurricular.id = :actividadId
              AND aca.procesoId = :procesoId
              AND aca.utilizadoPorCarrera = true
              AND ap.activo = true
              AND ap.esMaestro = false
              AND ap.procesoId = :procesoId
            """)
    List<AutoevaluacionAmbitoActividadJpaEntity> findActiveUsagesByActividadAndProceso(
            @Param("actividadId") UUID actividadId,
            @Param("procesoId") UUID procesoId
    );
}
