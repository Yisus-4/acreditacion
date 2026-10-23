package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.CompletarActividadCommand;
import com.undec.acreditacion.application.input.CompletarActividadInformarProcesoUseCase;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    private ActividadPlanTrabajo actividadCompletada(Instant fecha, String observaciones) {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());
        actividad.registrarComunicacionYCompletar(
                List.of(AreaInformadaProceso.DIRECCION_CARRERA, AreaInformadaProceso.SECRETARIAS),
                List.of(DocumentacionRemitidaProceso.RM_ESTANDARES),
                fecha,
                observaciones,
                UUID.randomUUID()
        );
        return actividad;
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar sin adjunto ni síntesis debe responder 200 con áreas y documentación")
    void debeCompletarSinAdjunto() throws Exception {
        UUID actividadId = UUID.randomUUID();
        ActividadPlanTrabajo actividad = actividadCompletada(Instant.parse("2026-03-10T03:00:00Z"), null);

        when(completarActividadUseCase.completar(any())).thenReturn(actividad);
        when(documentoRepository.findByActividadId(actividad.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(multipart("/api/actividades/{id}/completar", actividadId)
                        .param("areasInformadas", "DIRECCION_CARRERA", "SECRETARIAS")
                        .param("documentacionRemitida", "RM_ESTANDARES")
                        .param("fechaReunion", "10-03-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.areasInformadas[0]").value("DIRECCION_CARRERA"))
                .andExpect(jsonPath("$.areasInformadas[1]").value("SECRETARIAS"))
                .andExpect(jsonPath("$.documentacionRemitida[0]").value("RM_ESTANDARES"));

        ArgumentCaptor<CompletarActividadCommand> captor = ArgumentCaptor.forClass(CompletarActividadCommand.class);
        verify(completarActividadUseCase).completar(captor.capture());
        CompletarActividadCommand cmd = captor.getValue();
        assertEquals(List.of(AreaInformadaProceso.DIRECCION_CARRERA, AreaInformadaProceso.SECRETARIAS), cmd.areasInformadas());
        assertEquals(List.of(DocumentacionRemitidaProceso.RM_ESTANDARES), cmd.documentacionRemitida());
        assertFalse(cmd.tieneAdjunto());
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar con adjunto PDF opcional y observaciones")
    void debeCompletarConAdjuntoPdf() throws Exception {
        UUID actividadId = UUID.randomUUID();
        ActividadPlanTrabajo actividad = actividadCompletada(Instant.parse("2026-03-10T10:00:00Z"), "Reunión realizada");

        when(completarActividadUseCase.completar(any())).thenReturn(actividad);
        when(documentoRepository.findByActividadId(actividad.getId())).thenReturn(Collections.emptyList());

        MockMultipartFile file = new MockMultipartFile("archivo", "Comunicacion.pdf", "application/pdf",
                "%PDF-1.4 test stream".getBytes());

        mockMvc.perform(multipart("/api/actividades/{id}/completar", actividadId)
                        .file(file)
                        .param("areasInformadas", "DIRECCION_CARRERA")
                        .param("documentacionRemitida", "RM_ESTANDARES")
                        .param("fechaReunion", "2026-03-10T10:00:00Z")
                        .param("observaciones", "Reunión realizada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observaciones").value("Reunión realizada"));

        ArgumentCaptor<CompletarActividadCommand> captor = ArgumentCaptor.forClass(CompletarActividadCommand.class);
        verify(completarActividadUseCase).completar(captor.capture());
        assertTrue(captor.getValue().tieneAdjunto());
        assertEquals(TipoDocumentoAcreditacion.ACTA_REUNION_COORDINACION, captor.getValue().tipoDocumento());
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar debe soportar formatos datetime-local, argentino, 12h y simple")
    void debeSoportarMultiplesFormatosDeFecha() throws Exception {
        UUID actividadId = UUID.randomUUID();
        ActividadPlanTrabajo actividad = actividadCompletada(Instant.parse("2026-03-10T13:00:00Z"), null);

        when(completarActividadUseCase.completar(any())).thenReturn(actividad);
        when(documentoRepository.findByActividadId(any())).thenReturn(Collections.emptyList());

        String[] fechasValidas = {
                "2026-03-10T10:00",
                "2026-03-10T10:00:00",
                "2026-03-10T10:00:00-03:00",
                "10/03/2026 10:00",
                "10-03-2026 10:00",
                "10/03/2026 10:00:00",
                "10/03/2026",
                "10-03-2026",
                "2026-03-10",
                "03/10/2026 10:00 AM",
                "10/03/2026 10:00 AM"
        };

        for (String fecha : fechasValidas) {
            mockMvc.perform(multipart("/api/actividades/{id}/completar", actividadId)
                            .param("areasInformadas", "DIRECCION_CARRERA")
                            .param("documentacionRemitida", "RM_ESTANDARES")
                            .param("fechaReunion", fecha))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar debe rechazar fecha en formato ininteligible")
    void debeRechazarFechaInvalida() throws Exception {
        mockMvc.perform(multipart("/api/actividades/{id}/completar", UUID.randomUUID())
                        .param("areasInformadas", "DIRECCION_CARRERA")
                        .param("documentacionRemitida", "RM_ESTANDARES")
                        .param("fechaReunion", "fecha-invalida-xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar debe rechazar si no se informa ningún área")
    void debeRechazarSinAreas() throws Exception {
        mockMvc.perform(multipart("/api/actividades/{id}/completar", UUID.randomUUID())
                        .param("documentacionRemitida", "RM_ESTANDARES")
                        .param("fechaReunion", "10-03-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(completarActividadUseCase);
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar debe rechazar códigos de área o documentación inexistentes")
    void debeRechazarCodigosInvalidos() throws Exception {
        mockMvc.perform(multipart("/api/actividades/{id}/completar", UUID.randomUUID())
                        .param("areasInformadas", "RECTORADO_INVENTADO")
                        .param("documentacionRemitida", "RM_ESTANDARES")
                        .param("fechaReunion", "10-03-2026"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(completarActividadUseCase);
    }

    @Test
    @DisplayName("POST /api/actividades/{id}/completar debe rechazar si el adjunto no es PDF")
    void debeRechazarArchivoNoPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile("archivo", "documento.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "fake word bytes".getBytes());

        mockMvc.perform(multipart("/api/actividades/{id}/completar", UUID.randomUUID())
                        .file(file)
                        .param("areasInformadas", "DIRECCION_CARRERA")
                        .param("documentacionRemitida", "RM_ESTANDARES")
                        .param("fechaReunion", "2026-03-10T10:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
