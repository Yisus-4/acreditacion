package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProcesoCuerpoAcademicoRepository extends JpaRepository<ProcesoCuerpoAcademicoJpaEntity, UUID> {

    List<ProcesoCuerpoAcademicoJpaEntity> findByProcesoIdAndActivoTrue(UUID procesoId);

    List<ProcesoCuerpoAcademicoJpaEntity> findByProcesoId(UUID procesoId);

    Optional<ProcesoCuerpoAcademicoJpaEntity> findByProcesoIdAndOrigenMaestroId(UUID procesoId, UUID origenMaestroId);

    Optional<ProcesoCuerpoAcademicoJpaEntity> findByProcesoIdAndDni(UUID procesoId, String dni);
}
