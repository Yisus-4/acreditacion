package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.NormativaValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoNormativaTest {

    @Test
    @DisplayName("Crear documento borrador con datos válidos")
    void testCrearDocumentoBorradorValido() {
        UUID solicitudId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();

        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(
            solicitudId,
            procesoId,
            TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan_2020.pdf",
            "path/to/Plan_2020.pdf",
            "application/pdf",
            1024 * 50,
            subidoPor
        );

        assertNotNull(doc.getId());
        assertEquals(solicitudId, doc.getSolicitudNormativaId());
        assertEquals(procesoId, doc.getProcesoId());
        assertEquals(TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, doc.getTipoDocumento());
        assertEquals(CarpetaMinioDestino.NORMATIVA, doc.getCarpetaDestino());
        assertEquals("Plan_2020.pdf", doc.getNombreOriginal());
        assertTrue(doc.isEsBorrador());
        assertFalse(doc.isObservado());
        assertNull(doc.getMotivoObservacion());
    }

    @Test
    @DisplayName("Validación de tamaño máximo 20MB y tamaño mínimo 1 byte")
    void testValidacionTamanio() {
        UUID solicitudId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();

        // 0 bytes debe fallar
        assertThrows(NormativaValidationException.class, () -> DocumentoNormativa.crearBorrador(
            solicitudId, procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan.pdf", "path", "application/pdf", 0, subidoPor
        ));

        // Mayor a 20 MB (20971520 bytes) debe fallar
        assertThrows(NormativaValidationException.class, () -> DocumentoNormativa.crearBorrador(
            solicitudId, procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan.pdf", "path", "application/pdf", 20971521, subidoPor
        ));
    }

    @Test
    @DisplayName("Validación de tipo MIME estrictamente application/pdf")
    void testValidacionMime() {
        UUID solicitudId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();

        assertThrows(NormativaValidationException.class, () -> DocumentoNormativa.crearBorrador(
            solicitudId, procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan.doc", "path", "application/msword", 1024, subidoPor
        ));
    }

    @Test
    @DisplayName("Marcar como definitivo congela el documento y cambia esBorrador a false")
    void testMarcarComoDefinitivo() {
        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(
            UUID.randomUUID(), UUID.randomUUID(), TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan.pdf", "path", "application/pdf", 1024, UUID.randomUUID()
        );

        assertTrue(doc.isEsBorrador());
        doc.marcarComoDefinitivo();
        assertFalse(doc.isEsBorrador());
    }

    @Test
    @DisplayName("Marcar observado requiere motivo no vacío y limpiarObservacion lo reinicia")
    void testObservarYLimpiarObservacion() {
        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(
            UUID.randomUUID(), UUID.randomUUID(), TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan.pdf", "path", "application/pdf", 1024, UUID.randomUUID()
        );

        assertThrows(NormativaValidationException.class, () -> doc.marcarObservado(null));
        assertThrows(NormativaValidationException.class, () -> doc.marcarObservado("   "));

        doc.marcarObservado("Falta anexo curricular.");
        assertTrue(doc.isObservado());
        assertEquals("Falta anexo curricular.", doc.getMotivoObservacion());

        doc.limpiarObservacion();
        assertFalse(doc.isObservado());
        assertNull(doc.getMotivoObservacion());
    }
}
