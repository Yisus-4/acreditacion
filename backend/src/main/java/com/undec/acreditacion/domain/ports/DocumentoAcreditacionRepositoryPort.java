package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoAcreditacionRepositoryPort {

    DocumentoAcreditacion save(DocumentoAcreditacion doc);

    Optional<DocumentoAcreditacion> findById(UUID id);

    List<DocumentoAcreditacion> findByActividadId(UUID actividadId);

    default void deleteById(UUID id) {
    }
}
