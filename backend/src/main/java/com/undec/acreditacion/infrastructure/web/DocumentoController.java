package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.DescargarDocumentoUseCase;
import com.undec.acreditacion.application.output.DocumentoDescarga;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DescargarDocumentoUseCase descargarDocumentoUseCase;

    public DocumentoController(DescargarDocumentoUseCase descargarDocumentoUseCase) {
        this.descargarDocumentoUseCase = descargarDocumentoUseCase;
    }

    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable UUID id) {
        DocumentoDescarga documento = descargarDocumentoUseCase.descargar(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documento.nombreOriginal() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(documento.bytes().length))
                .body(documento.bytes());
    }
}
