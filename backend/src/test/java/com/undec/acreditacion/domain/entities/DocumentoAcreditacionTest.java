package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoAcreditacionTest {

    @Test
    @DisplayName("Debe crear un DocumentoAcreditacion con metadatos válidos")
    void debeCrearDocumentoValido() {
        UUID procesoId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();
        String nombre = "Acta_Reunion.pdf";
        String storagePath = "convocatorias/2026/acta.pdf";
        long size = 1024 * 500; // 500 KB

        DocumentoAcreditacion doc = DocumentoAcreditacion.crear(
                procesoId,
                actividadId,
                nombre,
                TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                "application/pdf",
                size,
                storagePath,
                subidoPor
        );

        assertNotNull(doc.getId());
        assertEquals(procesoId, doc.getProcesoId());
        assertEquals(actividadId, doc.getActividadId());
        assertEquals(nombre, doc.getNombreOriginal());
        assertEquals(TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION, doc.getTipoDocumento());
        assertEquals("application/pdf", doc.getMimeType());
        assertEquals(size, doc.getTamanioBytes());
        assertEquals(storagePath, doc.getStoragePath());
        assertEquals(subidoPor, doc.getSubidoPor());
        assertNotNull(doc.getCreadoEn());
    }

    @Test
    @DisplayName("Debe rechazar archivos cuyo tipo MIME no sea application/pdf")
    void debeRechazarMimeNoPdf() {
        UUID procesoId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();

        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(
                        procesoId,
                        null,
                        "archivo.docx",
                        TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        1024,
                        "path",
                        subidoPor
                )
        );
    }

    @Test
    @DisplayName("Debe rechazar archivos vacíos o que excedan 20 MB")
    void debeRechazarTamanioInvalido() {
        UUID procesoId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();

        // 0 bytes
        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(
                        procesoId,
                        null,
                        "acta.pdf",
                        TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                        "application/pdf",
                        0,
                        "path",
                        subidoPor
                )
        );

        // > 20 MB (20 * 1024 * 1024 + 1)
        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(
                        procesoId,
                        null,
                        "acta.pdf",
                        TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION,
                        "application/pdf",
                        20971521,
                        "path",
                        subidoPor
                )
        );
    }

    @Test
    @DisplayName("Debe rechazar campos obligatorios nulos")
    void debeRechazarCamposNulos() {
        UUID procesoId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();

        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(null, null, "a.pdf", TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION, "application/pdf", 100, "path", subidoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(procesoId, null, "", TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION, "application/pdf", 100, "path", subidoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(procesoId, null, "a.pdf", null, "application/pdf", 100, "path", subidoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(procesoId, null, "a.pdf", TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION, "application/pdf", 100, null, subidoPor)
        );

        assertThrows(DomainValidationException.class, () ->
                DocumentoAcreditacion.crear(procesoId, null, "a.pdf", TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION, "application/pdf", 100, "path", null)
        );
    }
}
