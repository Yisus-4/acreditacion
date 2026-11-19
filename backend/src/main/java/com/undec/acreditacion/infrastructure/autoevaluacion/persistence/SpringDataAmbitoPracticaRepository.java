package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAmbitoPracticaRepository extends JpaRepository<AmbitoPracticaJpaEntity, UUID> {

    List<AmbitoPracticaJpaEntity> findByTipoCategoriaAndActivoTrueOrderByNumeroOrdenAsc(String tipoCategoria);

    List<AmbitoPracticaJpaEntity> findByActivoTrueOrderByNumeroOrdenAsc();

    long countByActivoTrue();

    List<AmbitoPracticaJpaEntity> findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc();

    List<AmbitoPracticaJpaEntity> findByEsMaestroTrueAndTipoCategoriaAndActivoTrueOrderByNumeroOrdenAsc(String tipoCategoria);

    long countByEsMaestroTrueAndActivoTrue();

    @Query("SELECT a FROM AmbitoPracticaJpaEntity a WHERE a.esMaestro = true AND a.activo = true AND (" +
           "LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.tipoAmbito) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.carrerasQueLoUsan) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY a.numeroOrden ASC")
    List<AmbitoPracticaJpaEntity> buscarMaestrosPorTexto(@Param("query") String query);

    List<AmbitoPracticaJpaEntity> findByProcesoIdAndActivoTrueOrderByNumeroOrdenAsc(UUID procesoId);

    List<AmbitoPracticaJpaEntity> findByProcesoIdAndTipoCategoriaAndActivoTrueOrderByNumeroOrdenAsc(UUID procesoId, String tipoCategoria);

    List<AmbitoPracticaJpaEntity> findByProcesoId(UUID procesoId);

    Optional<AmbitoPracticaJpaEntity> findByProcesoIdAndOrigenMaestroId(UUID procesoId, UUID origenMaestroId);

    @Query("SELECT a FROM AmbitoPracticaJpaEntity a WHERE a.procesoId = :procesoId AND a.activo = true AND (" +
           "LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.tipoAmbito) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.carrerasQueLoUsan) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY a.numeroOrden ASC")
    List<AmbitoPracticaJpaEntity> buscarPorProcesoYTexto(@Param("procesoId") UUID procesoId, @Param("query") String query);

    @Query("SELECT a FROM AmbitoPracticaJpaEntity a WHERE a.activo = true AND (" +
           "LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.tipoAmbito) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.carrerasQueLoUsan) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY a.numeroOrden ASC")
    List<AmbitoPracticaJpaEntity> buscarPorTexto(@Param("query") String query);
}
