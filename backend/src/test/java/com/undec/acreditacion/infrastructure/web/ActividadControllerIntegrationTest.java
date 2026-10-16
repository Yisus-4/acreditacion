package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.CompletarActividadInformarProcesoUseCase;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActividadControllerIntegrationTest {

    private MockMvc mockMvc;
    private CompletarActividadInformarProcesoUseCase completarActividadUseCase;
    private DocumentoAcreditacionRepositoryPort documentoRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        completarActividadUseCase = mock(CompletarActividadInformarProcesoUseCase.class);
        documentoRepository = mock(DocumentoAcreditacionRepositoryPort.class);
        userRepository = mock(UserRepository.class);

        ActividadController controller = new ActividadController(
                completarActividadUseCase,
                documentoRepository,
                userRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ConvocatoriaExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar con multipart PDF debe responder 200 OK con actividad COMPLETADA")
    void debeCompletarActividadConMultipartPdf() throws Exception {
        UUID actividadId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(planId);
        actividad.registrarReunionYCompletar(
                Instant.parse("2026-03-10T10:00:00Z"),
                "Reunión de coordinación formal completada",
                usuarioId
        );

        when(completarActividadUseCase.completar(any())).thenReturn(actividad);
        when(documentoRepository.findByActividadId(actividad.getId())).thenReturn(Collections.emptyList());

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "Acta_Reunion.pdf",
                "application/pdf",
                "%PDF-1.4 test stream".getBytes()
        );

        mockMvc.perform(multipart("/api/actividades/{id}/completar", actividadId)
                        .file(file)
                        .param("fechaReunion", "2026-03-10T10:00:00Z")
                        .param("sintesisAcuerdos", "Reunión de coordinación formal completada")
                        .param("tipoDocumento", "ACTA_REUNION_COORDINACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.sintesisAcuerdos").value("Reunión de coordinación formal completada"));
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar debe rechazar si el archivo no es PDF")
    void debeRechazarArchivoNoPdf() throws Exception {
        UUID actividadId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "documento.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "fake word bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/actividades/{id}/completar", actividadId)
                        .file(file)
                        .param("fechaReunion", "2026-03-10T10:00:00Z")
                        .param("sintesisAcuerdos", "Reunión de coordinación formal completada")
                        .param("tipoDocumento", "ACTA_REUNION_COORDINACION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
