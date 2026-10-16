package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoDocumentoAcreditacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

public record CompletarActividadCommand(
        UUID actividadId,
        Instant fechaReunion,
        String sintesisAcuerdos,
        TipoDocumentoAcreditacion tipoDocumento,
        byte[] archivoBytes,
        String nombreArchivo,
        String mimeType,
        UUID usuarioAeaId
) {
    public CompletarActividadCommand {
        if (actividadId == null) {
            throw new DomainValidationException("El ID de la actividad es obligatorio");
        }
        if (fechaReunion == null) {
            throw new DomainValidationException("La fecha de reunión de coordinación es obligatoria");
        }
        if (sintesisAcuerdos == null || sintesisAcuerdos.trim().length() < 10) {
            throw new DomainValidationException("La síntesis de acuerdos debe tener al menos 10 caracteres");
        }
        if (tipoDocumento == null) {
            throw new DomainValidationException("El tipo de documento es obligatorio");
        }
        if (archivoBytes == null || archivoBytes.length == 0) {
            throw new DomainValidationException("El archivo probatorio es obligatorio");
        }
        if (archivoBytes.length > DocumentoAcreditacion.MAX_TAMANIO_BYTES) {
            throw new DomainValidationException("El archivo debe pesar entre 1 byte y 20 MB");
        }
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
            throw new DomainValidationException("El nombre del archivo es obligatorio");
        }
        if (mimeType == null || !DocumentoAcreditacion.MIME_PDF.equalsIgnoreCase(mimeType.trim())) {
            throw new DomainValidationException("Solo se admiten documentos en formato PDF");
        }
        if (usuarioAeaId == null) {
            throw new DomainValidationException("El ID del usuario de AEA es obligatorio");
        }
        sintesisAcuerdos = sintesisAcuerdos.trim();
        nombreArchivo = nombreArchivo.trim();
    }
}
