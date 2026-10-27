package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.CatalogoMiembroComision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogoMiembroComisionRepositoryPort {

    Optional<CatalogoMiembroComision> findById(UUID id);

    Optional<CatalogoMiembroComision> findByDocumento(String documento);

    Optional<CatalogoMiembroComision> findByCuitPasaporte(String cuitPasaporte);

    List<CatalogoMiembroComision> buscar(String busqueda);

    CatalogoMiembroComision save(CatalogoMiembroComision miembro);
}
