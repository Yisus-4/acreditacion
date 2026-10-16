package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.DescargarDocumentoUseCase;
import com.undec.acreditacion.application.output.DocumentoDescarga;
import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class DescargarDocumentoService implements DescargarDocumentoUseCase {

    private final DocumentoAcreditacionRepositoryPort documentoRepository;
    private final DocumentStoragePort documentStoragePort;

    public DescargarDocumentoService(
            DocumentoAcreditacionRepositoryPort documentoRepository,
            DocumentStoragePort documentStoragePort) {
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.documentStoragePort = Objects.requireNonNull(documentStoragePort);
    }

    @Override
    public DocumentoDescarga descargar(UUID documentoId) {
        DocumentoAcreditacion doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el documento con ID: " + documentoId));

        byte[] bytes = documentStoragePort.downloadFile(doc.getStoragePath());
        return new DocumentoDescarga(doc.getNombreOriginal(), doc.getMimeType(), bytes);
    }
}
