package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SpringDataEspacioFisicoRepository extends JpaRepository<EspacioFisicoJpaEntity, UUID> {

    List<EspacioFisicoJpaEntity> findByInmuebleIdAndActivoTrue(UUID inmuebleId);

    List<EspacioFisicoJpaEntity> findByInmuebleIdAndEspacioPadreIsNullAndActivoTrue(UUID inmuebleId);

    List<EspacioFisicoJpaEntity> findByEspacioPadreIdAndActivoTrue(UUID espacioPadreId);

    long countByEsMaestroTrue();

    List<EspacioFisicoJpaEntity> findByEsMaestroTrue();

    List<EspacioFisicoJpaEntity> findByProcesoId(UUID procesoId);

    java.util.Optional<EspacioFisicoJpaEntity> findByProcesoIdAndOrigenMaestroId(UUID procesoId, UUID origenMaestroId);

    @Query("SELECT e FROM EspacioFisicoJpaEntity e " +
           "JOIN e.inmueble i " +
           "LEFT JOIN i.sede s " +
           "WHERE e.activo = true " +
           "AND (:tipoEspacioId IS NULL OR e.tipoEspacio.id = :tipoEspacioId) " +
           "AND (:m2Min IS NULL OR e.superficieM2 >= :m2Min) " +
           "AND (:m2Max IS NULL OR e.superficieM2 <= :m2Max) " +
           "AND (:sedeId IS NULL OR s.id = :sedeId)")
    List<EspacioFisicoJpaEntity> consultarPorFiltros(
            @Param("tipoEspacioId") UUID tipoEspacioId,
            @Param("m2Min") BigDecimal m2Min,
            @Param("m2Max") BigDecimal m2Max,
            @Param("sedeId") UUID sedeId
    );

    @Query("SELECT e FROM EspacioFisicoJpaEntity e " +
           "JOIN e.inmueble i " +
           "LEFT JOIN i.sede s " +
           "WHERE e.activo = true " +
           "AND i.activo = true " +
           "AND i.esMaestro = true " +
           "AND e.esMaestro = true " +
           "AND (:tipoEspacioId IS NULL OR e.tipoEspacio.id = :tipoEspacioId) " +
           "AND (:m2Min IS NULL OR e.superficieM2 >= :m2Min) " +
           "AND (:m2Max IS NULL OR e.superficieM2 <= :m2Max) " +
           "AND (:sedeId IS NULL OR s.id = :sedeId)")
    List<EspacioFisicoJpaEntity> consultarMaestrosPorFiltros(
            @Param("tipoEspacioId") UUID tipoEspacioId,
            @Param("m2Min") BigDecimal m2Min,
            @Param("m2Max") BigDecimal m2Max,
            @Param("sedeId") UUID sedeId
    );

    @Query("SELECT e FROM EspacioFisicoJpaEntity e " +
           "JOIN e.inmueble i " +
           "LEFT JOIN i.sede s " +
           "WHERE e.activo = true " +
           "AND i.activo = true " +
           "AND i.procesoId = :procesoId " +
           "AND e.procesoId = :procesoId " +
           "AND (:tipoEspacioId IS NULL OR e.tipoEspacio.id = :tipoEspacioId) " +
           "AND (:m2Min IS NULL OR e.superficieM2 >= :m2Min) " +
           "AND (:m2Max IS NULL OR e.superficieM2 <= :m2Max) " +
           "AND (:sedeId IS NULL OR s.id = :sedeId)")
    List<EspacioFisicoJpaEntity> consultarPorProcesoYFiltros(
            @Param("procesoId") UUID procesoId,
            @Param("tipoEspacioId") UUID tipoEspacioId,
            @Param("m2Min") BigDecimal m2Min,
            @Param("m2Max") BigDecimal m2Max,
            @Param("sedeId") UUID sedeId
    );
}
