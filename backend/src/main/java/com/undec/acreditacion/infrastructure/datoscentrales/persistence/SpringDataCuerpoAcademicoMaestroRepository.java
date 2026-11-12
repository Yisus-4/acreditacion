package com.undec.acreditacion.infrastructure.datoscentrales.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCuerpoAcademicoMaestroRepository extends JpaRepository<CuerpoAcademicoMaestroJpaEntity, UUID> {

    List<CuerpoAcademicoMaestroJpaEntity> findByActivoTrue();

    Optional<CuerpoAcademicoMaestroJpaEntity> findByDni(String dni);

    boolean existsByDni(String dni);

    long countByActivoTrue();

    @Query("SELECT d FROM CuerpoAcademicoMaestroJpaEntity d WHERE d.activo = true AND (:query IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.apellido) LIKE LOWER(CONCAT('%', :query, '%')) OR d.dni LIKE CONCAT('%', :query, '%'))")
    List<CuerpoAcademicoMaestroJpaEntity> buscarPorTexto(@Param("query") String query);

    @Query("SELECT DISTINCT d FROM CuerpoAcademicoMaestroJpaEntity d JOIN d.asignaciones a WHERE a.carreraId = :carreraId AND d.activo = true")
    List<CuerpoAcademicoMaestroJpaEntity> findByCarreraId(@Param("carreraId") UUID carreraId);
}
