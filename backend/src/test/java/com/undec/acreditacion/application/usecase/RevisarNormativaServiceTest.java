package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.DocumentoObservadoDto;
import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.application.input.RevisarNormativaCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RevisarNormativaServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private SolicitudNormativaRepositoryPort solicitudRepository;
    private DocumentoNormativaRepositoryPort documentoRepository;
    private CarreraRepositoryPort carreraRepository;
    private RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private EmailNotificationPort emailNotificationPort;
    private RevisarNormativaService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        solicitudRepository = mock(SolicitudNormativaRepositoryPort.class);
        documentoRepository = mock(DocumentoNormativaRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        registroConvocatoriaRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        emailNotificationPort = mock(EmailNotificationPort.class);

        service = new RevisarNormativaService(
            procesoRepository,
            planTrabajoRepository,
            solicitudRepository,
            documentoRepository,
            carreraRepository,
            registroConvocatoriaRepository,
            emailNotificationPort
        );
    }

    @Test
    @DisplayName("Dictamen OBSERVAR pasa solicitud a OBSERVADA, marca documentos observados y notifica")
    void testRevisarNormativa_observar() {
        UUID carreraId = UUID.randomUUID();
        UUID revisorId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, revisorId);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(revisorId, "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();
        DocumentoNormativa doc = DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "p.pdf", "path", "application/pdf", 100, revisorId);
        sol.enviarDefinitivo(revisorId, LocalDate.now(), List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, revisorId),
            doc,
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "f.pdf", "p", "application/pdf", 100, revisorId)
        ), true, false);

        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any(SolicitudNormativaCarrera.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentoRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(documentoRepository.findBySolicitudNormativaId(sol.getId())).thenReturn(List.of(doc));

        Carrera carrera = Carrera.registrar(UUID.randomUUID(), "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero", 5, "desc", List.of());
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        RevisarNormativaCommand cmd = new RevisarNormativaCommand(
            procesoId, revisorId, DecisionRevisionNormativa.OBSERVAR,
            "El archivo cargado se encuentra ilegible en la página 3.",
            List.of(new DocumentoObservadoDto(doc.getId(), "Falta página 3 legibles"))
        );

        NormativaDetalleDto dto = service.ejecutar(cmd);

        assertNotNull(dto);
        assertEquals(EstadoSolicitudNormativa.OBSERVADA, dto.estado());
        assertTrue(doc.isObservado());
        assertEquals("Falta página 3 legibles", doc.getMotivoObservacion());

        verify(documentoRepository).save(doc);
        verify(emailNotificationPort).enviarObservaciones(eq(procesoId), anyString(), anyString(), anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Dictamen OBSERVAR con observaciones generales vacías construye resumen desde motivos de documentos y limpia observaciones previas")
    void testRevisarNormativa_observarConResumenAutomaticoYLimpiezaPrevia() {
        UUID carreraId = UUID.randomUUID();
        UUID revisorId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, revisorId);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(revisorId, "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();

        DocumentoNormativa doc1 = DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.CREACION_CARRERA, "PRG-001.pdf", "p1", "application/pdf", 100, revisorId);
        DocumentoNormativa doc2 = DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "ORD-002.pdf", "p2", "application/pdf", 100, revisorId);
        List<DocumentoNormativa> todosDocs = List.of(
            doc1,
            doc2,
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "f.pdf", "p", "application/pdf", 100, revisorId)
        );
        sol.enviarDefinitivo(revisorId, LocalDate.now(), todosDocs, true, false);
        // Simular que doc2 tenía una observación previa
        doc2.marcarObservado("observación anterior");

        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any(SolicitudNormativaCarrera.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentoRepository.findBySolicitudNormativaId(sol.getId())).thenReturn(todosDocs);

        Carrera carrera = Carrera.registrar(UUID.randomUUID(), "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero", 5, "desc", List.of());
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        RevisarNormativaCommand cmd = new RevisarNormativaCommand(
            procesoId, revisorId, DecisionRevisionNormativa.OBSERVAR,
            "",
            List.of(new DocumentoObservadoDto(doc1.getId(), "revisar algo"))
        );

        NormativaDetalleDto dto = service.ejecutar(cmd);

        assertEquals(EstadoSolicitudNormativa.OBSERVADA, dto.estado());
        assertEquals("revisar algo", dto.observacionesRevision());
        assertTrue(doc1.isObservado());
        assertEquals("revisar algo", doc1.getMotivoObservacion());
        assertFalse(doc2.isObservado());
        assertNull(doc2.getMotivoObservacion());
    }

    @Test
    @DisplayName("Dictamen APROBAR pasa solicitud a COMPLETADA y sincroniza ActividadPlanTrabajo a COMPLETADA")
    void testRevisarNormativa_aprobar() {
        UUID carreraId = UUID.randomUUID();
        UUID revisorId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, revisorId);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(revisorId, "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();
        sol.enviarDefinitivo(revisorId, LocalDate.now(), List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "c.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "d.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "e.pdf", "p", "application/pdf", 100, revisorId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "f.pdf", "p", "application/pdf", 100, revisorId)
        ), true, false);

        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any(SolicitudNormativaCarrera.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentoRepository.findBySolicitudNormativaId(sol.getId())).thenReturn(List.of());

        Carrera carrera = Carrera.registrar(UUID.randomUUID(), "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero", 5, "desc", List.of());
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        RevisarNormativaCommand cmd = new RevisarNormativaCommand(
            procesoId, revisorId, DecisionRevisionNormativa.APROBAR,
            "Documentación completa y correcta.", List.of()
        );

        NormativaDetalleDto dto = service.ejecutar(cmd);

        assertNotNull(dto);
        assertEquals(EstadoSolicitudNormativa.COMPLETADA, dto.estado());
        assertEquals(EstadoActividad.COMPLETADA, act2.getEstado());
        assertEquals(revisorId, act2.getCompletadaPor());
        assertNotNull(act2.getCompletadaEn());

        verify(planTrabajoRepository).saveActividad(act2);
        verify(emailNotificationPort).enviarAprobacion(eq(procesoId), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
