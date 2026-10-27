package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCatalogoMiembroComisionRepository extends JpaRepository<CatalogoMiembroComisionJpaEntity, UUID> {

    Optional<CatalogoMiembroComisionJpaEntity> findByDocumento(String documento);

    Optional<CatalogoMiembroComisionJpaEntity> findByCuitPasaporte(String cuitPasaporte);

    @Query("SELECT c FROM CatalogoMiembroComisionJpaEntity c WHERE " +
            "LOWER(c.apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(c.nombreCompleto) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(c.documento) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(c.cuitPasaporte) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(c.correoElectronico) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
            "ORDER BY c.apellido ASC, c.nombreCompleto ASC")
    List<CatalogoMiembroComisionJpaEntity> buscar(@Param("busqueda") String busqueda);
}
