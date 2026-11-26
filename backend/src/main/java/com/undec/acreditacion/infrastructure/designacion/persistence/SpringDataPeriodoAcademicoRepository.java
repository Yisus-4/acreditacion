package com.undec.acreditacion.infrastructure.designacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPeriodoAcademicoRepository extends JpaRepository<PeriodoAcademicoJpaEntity, UUID> {

    List<PeriodoAcademicoJpaEntity> findAllByOrderByAnioDescCodigoDesc();

    List<PeriodoAcademicoJpaEntity> findByActivoTrueOrderByAnioDescCodigoDesc();

    Optional<PeriodoAcademicoJpaEntity> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
