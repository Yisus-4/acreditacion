package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.DescargarDocumentoUseCase;
import com.undec.acreditacion.application.output.DocumentoDescarga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentoControllerIntegrationTest {

    private MockMvc mockMvc;
    private DescargarDocumentoUseCase descargarDocumentoUseCase;

    @BeforeEach
    void setUp() {
        descargarDocumentoUseCase = mock(DescargarDocumentoUseCase.class);
        DocumentoController controller = new DocumentoController(descargarDocumentoUseCase);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ConvocatoriaExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/documentos/{id}/descargar debe retornar PDF con headers apropiados")
    void debeDescargarPdf() throws Exception {
        UUID docId = UUID.randomUUID();
        byte[] pdfBytes = "%PDF-1.4 sample content".getBytes();
        DocumentoDescarga descarga = new DocumentoDescarga("Acta.pdf", "application/pdf", pdfBytes);

        when(descargarDocumentoUseCase.descargar(docId)).thenReturn(descarga);

        mockMvc.perform(get("/api/documentos/{id}/descargar", docId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Acta.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }
}
