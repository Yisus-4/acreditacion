package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataConvenioInstitucionalRepository extends JpaRepository<ConvenioInstitucionalJpaEntity, UUID> {

    List<ConvenioInstitucionalJpaEntity> findByVigenteOrderByContraparteAsc(Boolean vigente);

    List<ConvenioInstitucionalJpaEntity> findAllByOrderByFechaCelebracionDesc();

    @Query("SELECT c FROM ConvenioInstitucionalJpaEntity c WHERE " +
           "LOWER(c.contraparte) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.resolucionRectoral) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.expediente) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.objeto) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY c.fechaCelebracion DESC")
    List<ConvenioInstitucionalJpaEntity> buscarPorTexto(@Param("query") String query);
}
