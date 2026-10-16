package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataCarreraRepository extends JpaRepository<CarreraJpaEntity, UUID> {

    @Query("SELECT c FROM CarreraJpaEntity c LEFT JOIN FETCH c.sedes WHERE c.unidadAcademica.id = :unidadAcademicaId")
    List<CarreraJpaEntity> findByUnidadAcademicaId(@Param("unidadAcademicaId") UUID unidadAcademicaId);

    @Query("SELECT c FROM CarreraJpaEntity c LEFT JOIN FETCH c.sedes")
    List<CarreraJpaEntity> findAllWithSedes();

    boolean existsByUnidadAcademicaIdAndNombre(UUID unidadAcademicaId, String nombre);

    long countByUnidadAcademicaId(UUID unidadAcademicaId);

    @Query("SELECT COUNT(c) > 0 FROM CarreraJpaEntity c JOIN c.sedes s WHERE s.id = :sedeId")
    boolean existsBySedeId(@Param("sedeId") UUID sedeId);
}
