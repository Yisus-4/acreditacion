package com.undec.acreditacion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DocumentosFaltantesException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort;
import com.undec.acreditacion.infrastructure.web.dto.RevisarNormativaRequest;
import com.undec.acreditacion.infrastructure.web.dto.SolicitarNormativaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NormativaControllerIT {

    private MockMvc mockMvc;
    private SolicitarNormativaUseCase solicitarNormativaUseCase;
    private ObtenerNormativaDetalleUseCase obtenerNormativaDetalleUseCase;
    private GuardarDocumentoBorradorUseCase guardarDocumentoBorradorUseCase;
    private EliminarDocumentoBorradorUseCase eliminarDocumentoBorradorUseCase;
    private EnviarNormativaDefinitivaUseCase enviarNormativaDefinitivaUseCase;
    private RevisarNormativaUseCase revisarNormativaUseCase;
    private ExportarHojaInfoExcelUseCase exportarHojaInfoExcelUseCase;
    private DocumentoNormativaRepositoryPort documentoRepository;
    private DocumentStoragePort storagePort;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    private UUID procesoId;
    private UUID solicitudId;
    private UUID actividadId;

    @BeforeEach
    void setUp() {
        solicitarNormativaUseCase = mock(SolicitarNormativaUseCase.class);
        obtenerNormativaDetalleUseCase = mock(ObtenerNormativaDetalleUseCase.class);
        guardarDocumentoBorradorUseCase = mock(GuardarDocumentoBorradorUseCase.class);
        eliminarDocumentoBorradorUseCase = mock(EliminarDocumentoBorradorUseCase.class);
        enviarNormativaDefinitivaUseCase = mock(EnviarNormativaDefinitivaUseCase.class);
        revisarNormativaUseCase = mock(RevisarNormativaUseCase.class);
        exportarHojaInfoExcelUseCase = mock(ExportarHojaInfoExcelUseCase.class);
        documentoRepository = mock(DocumentoNormativaRepositoryPort.class);
        storagePort = mock(DocumentStoragePort.class);
        userRepository = mock(UserRepository.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        NormativaController controller = new NormativaController(
                solicitarNormativaUseCase,
                obtenerNormativaDetalleUseCase,
                guardarDocumentoBorradorUseCase,
                eliminarDocumentoBorradorUseCase,
                enviarNormativaDefinitivaUseCase,
                revisarNormativaUseCase,
                exportarHojaInfoExcelUseCase,
                documentoRepository,
                storagePort,
                userRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ConvocatoriaExceptionHandler())
                .build();

        procesoId = UUID.randomUUID();
        solicitudId = UUID.randomUUID();
        actividadId = UUID.randomUUID();
    }

    private NormativaDetalleDto crearDetalleDto(EstadoSolicitudNormativa estado, List<DocumentoNormativaDto> docs) {
        BadgePlazoDto badge = new BadgePlazoDto("EN_TERMINO", 10, "A tiempo", "badge-success");
        return new NormativaDetalleDto(
                solicitudId,
                procesoId,
                actividadId,
                estado,
                Instant.now(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(10),
                10,
                null,
                null,
                CumplimientoPlazo.EN_TERMINO,
                "Observaciones iniciales",
                null,
                null,
                null,
                badge,
                docs != null ? docs : Collections.emptyList(),
                true,
                false
        );
    }

    @Test
    @DisplayName("POST /api/v1/procesos/{id}/normativa debe crear solicitud y retornar 201 Created")
    void debeSolicitarNormativaExitosamente() throws Exception {
        when(solicitarNormativaUseCase.ejecutar(any(SolicitarNormativaCommand.class)))
                .thenReturn(crearDetalleDto(EstadoSolicitudNormativa.SOLICITADA, Collections.emptyList()));

        SolicitarNormativaRequest request = new SolicitarNormativaRequest("Apertura de normativa", 10);

        mockMvc.perform(post("/api/v1/procesos/" + procesoId + "/normativa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(solicitudId.toString()))
                .andExpect(jsonPath("$.estado").value("SOLICITADA"))
                .andExpect(jsonPath("$.documentosRequeridos").value(5));

        verify(solicitarNormativaUseCase).ejecutar(any(SolicitarNormativaCommand.class));
    }

    @Test
    @DisplayName("GET /api/v1/procesos/{id}/normativa debe retornar 200 OK con detalle y badges")
    void debeObtenerDetalleNormativa() throws Exception {
        when(obtenerNormativaDetalleUseCase.ejecutar(procesoId))
                .thenReturn(crearDetalleDto(EstadoSolicitudNormativa.EN_CARGA_BORRADOR, Collections.emptyList()));

        mockMvc.perform(get("/api/v1/procesos/" + procesoId + "/normativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitudId.toString()))
                .andExpect(jsonPath("$.badgePlazo.etiqueta").value("A tiempo"));
    }

    @Test
    @DisplayName("POST /api/v1/procesos/{id}/normativa/documentos con multipart PDF debe responder 201 Created")
    void debeCargarDocumentoBorrador() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "ordenanza_plan.pdf",
                "application/pdf",
                "%PDF-1.4 contenido del archivo".getBytes()
        );

        UUID docId = UUID.randomUUID();
        DocumentoNormativaDto docDto = new DocumentoNormativaDto(
                docId,
                solicitudId,
                procesoId,
                TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
                CarpetaMinioDestino.NORMATIVA,
                "ordenanza_plan.pdf",
                "storage/ordenanza_plan.pdf",
                "application/pdf",
                (long) file.getBytes().length,
                true,
                false,
                null,
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );

        when(guardarDocumentoBorradorUseCase.ejecutar(any())).thenReturn(docDto);

        mockMvc.perform(multipart("/api/v1/procesos/" + procesoId + "/normativa/documentos")
                        .file(file)
                        .param("tipoDocumento", "ORDENANZA_PLAN_ESTUDIOS"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.nombreOriginal").value("ordenanza_plan.pdf"))
                .andExpect(jsonPath("$.tipoDocumento").value("ORDENANZA_PLAN_ESTUDIOS"));

        verify(guardarDocumentoBorradorUseCase).ejecutar(any());
    }

    @Test
    @DisplayName("DELETE /api/v1/procesos/{procesoId}/normativa/documentos/{documentoId} responde 204 No Content")
    void debeEliminarDocumentoBorrador() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/procesos/" + procesoId + "/normativa/documentos/" + docId))
                .andExpect(status().isNoContent());

        verify(eliminarDocumentoBorradorUseCase).ejecutar(any());
    }

    @Test
    @DisplayName("POST /api/v1/procesos/{id}/normativa/enviar cuando faltan documentos responde 400 Bad Request")
    void debeFallarEnvioSiFaltanDocumentos() throws Exception {
        doThrow(new DocumentosFaltantesException(List.of(TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS)))
                .when(enviarNormativaDefinitivaUseCase).ejecutar(any());

        mockMvc.perform(post("/api/v1/procesos/" + procesoId + "/normativa/enviar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.faltantes[0]").value("ORDENANZA_PLAN_ESTUDIOS"));
    }

    @Test
    @DisplayName("POST /api/v1/procesos/{id}/normativa/enviar completo responde 200 OK con estado ENVIADA_A_REVISION")
    void debeEnviarNormativaDefinitivaExitosamente() throws Exception {
        when(enviarNormativaDefinitivaUseCase.ejecutar(any()))
                .thenReturn(crearDetalleDto(EstadoSolicitudNormativa.ENVIADA_A_REVISION, Collections.emptyList()));

        mockMvc.perform(post("/api/v1/procesos/" + procesoId + "/normativa/enviar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADA_A_REVISION"));

        verify(enviarNormativaDefinitivaUseCase).ejecutar(any());
    }

    @Test
    @DisplayName("POST /api/v1/procesos/{id}/normativa/revisar responde 200 OK con estado COMPLETADA")
    void debeRevisarYAprobarNormativa() throws Exception {
        when(revisarNormativaUseCase.ejecutar(any()))
                .thenReturn(crearDetalleDto(EstadoSolicitudNormativa.COMPLETADA, Collections.emptyList()));

        RevisarNormativaRequest request = new RevisarNormativaRequest("APROBADO", "Todo en orden conforme normativa", Collections.emptyList());

        mockMvc.perform(post("/api/v1/procesos/" + procesoId + "/normativa/revisar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        verify(revisarNormativaUseCase).ejecutar(any());
    }

    @Test
    @DisplayName("GET /api/v1/planes-trabajo/{id}/hoja-info responde 200 OK con Content-Type XLSX")
    void debeExportarHojaInfoExcel() throws Exception {
        UUID planId = UUID.randomUUID();
        byte[] mockExcel = new byte[]{1, 2, 3, 4};
        when(exportarHojaInfoExcelUseCase.ejecutar(planId)).thenReturn(mockExcel);

        mockMvc.perform(get("/api/v1/planes-trabajo/" + planId + "/hoja-info"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Hoja_INFO_PlanTrabajo_" + planId + ".xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(mockExcel));

        verify(exportarHojaInfoExcelUseCase).ejecutar(planId);
    }

    @Test
    @DisplayName("GET /api/v1/procesos/{id}/normativa/documentos/{docId}/descargar redirige 302 si existe presigned URL")
    void debeRedirigirPresignedDownloadUrl() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(
                solicitudId,
                procesoId,
                TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
                "ordenanza_plan.pdf",
                "storage/path/res.pdf",
                "application/pdf",
                1024L,
                UUID.randomUUID()
        );

        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storagePort.getPresignedDownloadUrl(doc.getStoragePath(), 3600))
                .thenReturn("https://minio.undec.edu.ar/presigned-url");

        mockMvc.perform(get("/api/v1/procesos/" + procesoId + "/normativa/documentos/" + docId + "/descargar"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://minio.undec.edu.ar/presigned-url"));
    }
}
