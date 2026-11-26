package com.undec.acreditacion.infrastructure.designacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDepartamentoAcademicoRepository extends JpaRepository<DepartamentoAcademicoJpaEntity, UUID> {

    List<DepartamentoAcademicoJpaEntity> findAllByOrderByDenominacionAsc();

    List<DepartamentoAcademicoJpaEntity> findByActivoTrueOrderByDenominacionAsc();

    Optional<DepartamentoAcademicoJpaEntity> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
