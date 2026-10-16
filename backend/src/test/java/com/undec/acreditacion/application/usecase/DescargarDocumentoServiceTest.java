package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.output.DocumentoDescarga;
import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoDocumentoAcreditacion;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DescargarDocumentoServiceTest {

    private DocumentoAcreditacionRepositoryPort documentoRepository;
    private DocumentStoragePort documentStoragePort;
    private DescargarDocumentoService service;

    @BeforeEach
    void setUp() {
        documentoRepository = mock(DocumentoAcreditacionRepositoryPort.class);
        documentStoragePort = mock(DocumentStoragePort.class);
        service = new DescargarDocumentoService(documentoRepository, documentStoragePort);
    }

    @Test
    @DisplayName("Debe descargar documento y bytes desde MinIO")
    void debeDescargarDocumento() {
        UUID docId = UUID.randomUUID();
        DocumentoAcreditacion doc = DocumentoAcreditacion.crear(
                UUID.randomUUID(), UUID.randomUUID(), "acta.pdf",
                TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION, "application/pdf",
                100, "storage/path/acta.pdf", UUID.randomUUID()
        );
        byte[] expectedBytes = "%PDF-1.4 sample content".getBytes();

        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentStoragePort.downloadFile("storage/path/acta.pdf")).thenReturn(expectedBytes);

        DocumentoDescarga resultado = service.descargar(docId);

        assertNotNull(resultado);
        assertEquals("acta.pdf", resultado.nombreOriginal());
        assertEquals("application/pdf", resultado.mimeType());
        assertArrayEquals(expectedBytes, resultado.bytes());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el documento no existe")
    void debeLanzarExcepcionSiNoExiste() {
        UUID docId = UUID.randomUUID();
        when(documentoRepository.findById(docId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.descargar(docId));
    }
}
