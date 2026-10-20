package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EnviarNormativaDefinitivaCommand;
import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DocumentosFaltantesException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EnviarNormativaDefinitivaServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private SolicitudNormativaRepositoryPort solicitudRepository;
    private DocumentoNormativaRepositoryPort documentoRepository;
    private CarreraRepositoryPort carreraRepository;
    private RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private EmailNotificationPort emailNotificationPort;
    private EnviarNormativaDefinitivaService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        solicitudRepository = mock(SolicitudNormativaRepositoryPort.class);
        documentoRepository = mock(DocumentoNormativaRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        registroConvocatoriaRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        emailNotificationPort = mock(EmailNotificationPort.class);

        service = new EnviarNormativaDefinitivaService(
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
    @DisplayName("Lanzar DocumentosFaltantesException si la matriz obligatoria está incompleta")
    void testEnviarDefinitivo_incompletaLanzaExcepcion() {
        UUID carreraId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, usuarioId);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(usuarioId, "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));

        when(documentoRepository.findBySolicitudNormativaId(sol.getId())).thenReturn(List.of());

        EnviarNormativaDefinitivaCommand cmd = new EnviarNormativaDefinitivaCommand(procesoId, usuarioId);
        assertThrows(DocumentosFaltantesException.class, () -> service.ejecutar(cmd));
    }

    @Test
    @DisplayName("Envío exitoso transiciona a ENVIADA_A_REVISION, congela documentos y notifica a AEA")
    void testEnviarDefinitivo_exito() {
        UUID carreraId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(carreraId, "CONV", "Titulo", TipoProcesoAcreditacion.NUEVA, usuarioId);
        UUID procesoId = proceso.getId();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID planId = UUID.randomUUID();
        ActividadPlanTrabajo act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(planId);
        PlanTrabajo plan = PlanTrabajo.rehydrate(planId, procesoId, "PT-001", TipoPlanTrabajo.PT_001_ACREDITACION, EstadoPlanTrabajo.ACTIVO, null, null, List.of(act2));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        SolicitudNormativaCarrera sol = SolicitudNormativaCarrera.iniciar(procesoId, act2.getId());
        sol.solicitar(usuarioId, "obs", 5, LocalDate.now());
        sol.iniciarCargaBorrador();
        when(solicitudRepository.findByActividadId(act2.getId())).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any(SolicitudNormativaCarrera.class))).thenAnswer(inv -> inv.getArgument(0));

        List<DocumentoNormativa> docs = List.of(
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.CREACION_CARRERA, "a.pdf", "p", "application/pdf", 100, usuarioId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS, "b.pdf", "p", "application/pdf", 100, usuarioId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA, "c.pdf", "p", "application/pdf", 100, usuarioId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR, "d.pdf", "p", "application/pdf", 100, usuarioId),
            DocumentoNormativa.crearBorrador(sol.getId(), procesoId, TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA, "e.pdf", "p", "application/pdf", 100, usuarioId)
        );
        when(documentoRepository.findBySolicitudNormativaId(sol.getId())).thenReturn(docs);

        Carrera carrera = Carrera.registrar(UUID.randomUUID(), "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero", 5, "desc", List.of());
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        EnviarNormativaDefinitivaCommand cmd = new EnviarNormativaDefinitivaCommand(procesoId, usuarioId);
        NormativaDetalleDto dto = service.ejecutar(cmd);

        assertNotNull(dto);
        assertEquals(EstadoSolicitudNormativa.ENVIADA_A_REVISION, dto.estado());
        assertEquals(CumplimientoPlazo.EN_TERMINO, dto.cumplimientoPlazo());

        for (DocumentoNormativa d : docs) {
            assertFalse(d.isEsBorrador());
            verify(documentoRepository).save(d);
        }

        verify(emailNotificationPort).enviarRemisionRevision(
            eq(procesoId),
            eq("acreditacion@undec.edu.ar"),
            eq("Área de Evaluación y Acreditación"),
            eq("Ingeniería en Sistemas"),
            anyString(),
            eq(CumplimientoPlazo.EN_TERMINO),
            eq(5)
        );
    }
}
