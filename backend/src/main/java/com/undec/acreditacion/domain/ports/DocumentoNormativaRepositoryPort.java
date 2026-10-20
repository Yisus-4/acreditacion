package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.DocumentoNormativa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoNormativaRepositoryPort {
    DocumentoNormativa save(DocumentoNormativa documento);
    Optional<DocumentoNormativa> findById(UUID id);
    List<DocumentoNormativa> findBySolicitudNormativaId(UUID solicitudId);
    List<DocumentoNormativa> findByProcesoId(UUID procesoId);
    void delete(DocumentoNormativa documento);
    void deleteById(UUID id);
}
