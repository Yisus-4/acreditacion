package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.DocumentoNormativaDto;
import com.undec.acreditacion.application.input.GuardarDocumentoBorradorCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.EstadoInvalidoException;
import com.undec.acreditacion.domain.exceptions.NormativaValidationException;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GuardarDocumentoBorradorServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private SolicitudNormativaRepositoryPort solicitudRepository;
    private DocumentoNormativaRepositoryPort documentoRepository;
    private DocumentStoragePort documentStoragePort;
    private CarreraRepositoryPort carreraRepository;
    private RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private GuardarDocumentoBorradorService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        solicitudRepository = mock(SolicitudNormativaRepositoryPort.class);
        documentoRepository = mock(DocumentoNormativaRepositoryPort.class);
        documentStoragePort = mock(DocumentStoragePort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        registroConvocatoriaRepository = mock(RegistroConvocatoriaRepositoryPort.class);

        service = new GuardarDocumentoBorradorService(
            procesoRepository,
            planTrabajoRepository,
            solicitudRepository,
            documentoRepository,
            documentStoragePort,
            carreraRepository,
            registroConvocatoriaRepository
        );
    }

    @Test
    @DisplayName("Rechazar archivo que no comience con los magic bytes %PDF-1.")
    void testGuardarDocumento_rechazaMagicBytesInvalidos() {
        UUID procesoId = UUID.randomUUID();
        byte[] contenidoInvalido = "ESTO NO ES UN PDF".getBytes(StandardCharsets.UTF_8);

        GuardarDocumentoBorradorCommand cmd = new GuardarDocumentoBorradorCommand(
            procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "documento.pdf", contenidoInvalido, "application/pdf", UUID.randomUUID()
        );

        assertThrows(NormativaValidationException.class, () -> service.ejecutar(cmd));
    }

    @Test
    @DisplayName("Subida exitosa de PDF válido pasa a EN_CARGA_BORRADOR, sube a MinIO y persiste")
    void testGuardarDocumento_exito() {
        UUID carreraId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "ISI Acreditación", TipoProcesoAcreditacion.NUEVA, subidoPor);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act1 = ActividadPlanTrabajo.crearActividadInicial(planId);
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act1, act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.now());
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any(SolicitudNormativaCarrera.class))).thenAnswer(inv -> inv.getArgument(0));

        Carrera carrera = Carrera.registrar(UUID.randomUUID(), "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero", 5, "desc", List.of());
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        RegistroConvocatoria conv = RegistroConvocatoria.crear(procesoId, "RES-1234-2026", LocalDate.now(), LocalDate.now().plusMonths(3), "obs");
        when(registroConvocatoriaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(conv));

        byte[] pdfValido = "%PDF-1.4 contenido de prueba del pdf".getBytes(StandardCharsets.UTF_8);
        when(documentStoragePort.uploadFile(any(), any(), any())).thenReturn("storage/path/file.pdf");
        when(documentoRepository.findBySolicitudNormativaId(sol.getId())).thenReturn(List.of());
        when(documentoRepository.save(any(DocumentoNormativa.class))).thenAnswer(inv -> inv.getArgument(0));

        GuardarDocumentoBorradorCommand cmd = new GuardarDocumentoBorradorCommand(
            procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan_2020.pdf", pdfValido, "application/pdf", subidoPor
        );

        DocumentoNormativaDto dto = service.ejecutar(cmd);

        assertNotNull(dto);
        assertEquals(TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, dto.tipoDocumento());
        assertTrue(dto.esBorrador());
        assertEquals("Plan_2020.pdf", dto.nombreOriginal());
        assertEquals(EstadoSolicitudNormativa.EN_CARGA_BORRADOR, sol.getEstado());

        verify(documentStoragePort).uploadFile(eq(pdfValido), anyString(), eq("application/pdf"));
        verify(documentoRepository).save(any(DocumentoNormativa.class));
    }

    @Test
    @DisplayName("Rechazar subida si la solicitud está en ENVIADA_A_REVISION")
    void testGuardarDocumento_rechazaEnRevision() {
        UUID carreraId = UUID.randomUUID();
        UUID subidoPor = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV-2026-ISI-01", "ISI Acreditación", TipoProcesoAcreditacion.NUEVA, subidoPor);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(UUID.randomUUID(), "obs", 5, LocalDate.now());
        // Forzar a revisión
        sol.enviarDefinitivo(UUID.randomUUID(), LocalDate.now(), List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, subidoPor),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "b.pdf", "p", "application/pdf", 100, subidoPor),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "c.pdf", "p", "application/pdf", 100, subidoPor),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, subidoPor),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, subidoPor)
        ), true, false);

        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));

        byte[] pdfValido = "%PDF-1.4 valido".getBytes(StandardCharsets.UTF_8);
        GuardarDocumentoBorradorCommand cmd = new GuardarDocumentoBorradorCommand(
            procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
            "Plan.pdf", pdfValido, "application/pdf", subidoPor
        );

        assertThrows(EstadoInvalidoException.class, () -> service.ejecutar(cmd));
    }
}
