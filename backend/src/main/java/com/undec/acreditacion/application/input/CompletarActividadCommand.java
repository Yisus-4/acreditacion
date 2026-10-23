package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.AreaInformadaProceso;
import com.undec.acreditacion.domain.entities.DocumentacionRemitidaProceso;
import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoDocumentoAcreditacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Comando de la Actividad 1 (Informar sobre el proceso de acreditación).
 * Exige áreas informadas, documentación remitida y fecha de la reunión coordinada con la DC.
 * Observaciones y adjunto PDF son opcionales.
 */
public record CompletarActividadCommand(
        UUID actividadId,
        List<AreaInformadaProceso> areasInformadas,
        List<DocumentacionRemitidaProceso> documentacionRemitida,
        Instant fechaReunion,
        String observaciones,
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
        if (areasInformadas == null || areasInformadas.isEmpty()) {
            throw new DomainValidationException("Debe indicar al menos un área informada");
        }
        if (documentacionRemitida == null || documentacionRemitida.isEmpty()) {
            throw new DomainValidationException("Debe indicar al menos un documento remitido");
        }
        if (fechaReunion == null) {
            throw new DomainValidationException("La fecha de la reunión coordinada con la Dirección de Carrera es obligatoria");
        }
        if (usuarioAeaId == null) {
            throw new DomainValidationException("El ID del usuario de AEA es obligatorio");
        }
        areasInformadas = List.copyOf(areasInformadas);
        documentacionRemitida = List.copyOf(documentacionRemitida);
        observaciones = (observaciones == null || observaciones.isBlank()) ? null : observaciones.trim();

        boolean tieneAdjunto = archivoBytes != null && archivoBytes.length > 0;
        if (tieneAdjunto) {
            if (archivoBytes.length > DocumentoAcreditacion.MAX_TAMANIO_BYTES) {
                throw new DomainValidationException("El archivo debe pesar entre 1 byte y 20 MB");
            }
            if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                throw new DomainValidationException("El nombre del archivo es obligatorio");
            }
            if (mimeType == null || !DocumentoAcreditacion.MIME_PDF.equalsIgnoreCase(mimeType.trim())) {
                throw new DomainValidationException("Solo se admiten documentos en formato PDF");
            }
            nombreArchivo = nombreArchivo.trim();
            if (tipoDocumento == null) {
                tipoDocumento = TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION;
            }
        } else {
            archivoBytes = null;
            nombreArchivo = null;
            mimeType = null;
        }
    }

    public boolean tieneAdjunto() {
        return archivoBytes != null && archivoBytes.length > 0;
    }
}
