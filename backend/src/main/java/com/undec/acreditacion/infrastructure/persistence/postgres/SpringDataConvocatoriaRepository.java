package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpringDataConvocatoriaRepository extends JpaRepository<ConvocatoriaJpaEntity, UUID> {
    @Query("SELECT COUNT(c) FROM ConvocatoriaJpaEntity c WHERE EXTRACT(YEAR FROM c.fechaInicio) = :anio")
    long countByAnio(@Param("anio") int anio);
}
