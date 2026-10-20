package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ElaborarPlanTrabajoCommand;
import com.undec.acreditacion.application.input.ItemActividadPlanCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;
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

class ElaborarPlanTrabajoServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private RegistroConvocatoriaRepositoryPort registroRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private ElaborarPlanTrabajoService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        registroRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        service = new ElaborarPlanTrabajoService(procesoRepository, registroRepository, planTrabajoRepository);
    }

    @Test
    @DisplayName("Debe elaborar plan de trabajo completando RG-001, completando ACT-02 y guardando cronograma")
    void debeElaborarPlanTrabajoExitosamente() {
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID usuarioAeaId = UUID.randomUUID();

        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026-ISI-01", "Acreditación ISI 2026",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, usuarioAeaId
        );
        RegistroConvocatoria registro = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-145", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 11, 30), "Obs"
        );
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(registroRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(registro));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));
        when(planTrabajoRepository.save(any(PlanTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate fechaLimite = LocalDate.of(2026, 12, 10);
        List<ItemActividadPlanCommand> actividades = List.of(
                new ItemActividadPlanCommand(
                        3,
                        "ACT-03-DOC",
                        "Relevamiento cuerpo docente y CVs",
                        HojaPlanTrabajo.INFO,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 5, 1),
                        "Director Carrera",
                        PrioridadActividad.ALTA
                ),
                new ItemActividadPlanCommand(
                        4,
                        "ACT-04-FICHAS",
                        "Carga de fichas docentes",
                        HojaPlanTrabajo.CONEAU,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 7, 1),
                        "Comisión Autoevaluación",
                        PrioridadActividad.MEDIA
                )
        );

        ElaborarPlanTrabajoCommand command = new ElaborarPlanTrabajoCommand(
                procesoId,
                "RM 1254/2018",
                FaseEvaluacion.PRIMERA_FASE,
                fechaLimite,
                actividades,
                usuarioAeaId
        );

        PlanTrabajo resultado = service.elaborar(command);

        assertNotNull(resultado);
        verify(registroRepository).save(registro);
        assertEquals("RM 1254/2018", registro.getRmEstandares());
        assertEquals(fechaLimite, registro.getFechaLimitePresentacionConeau());

        // Debe haber completado Actividad 2
        ActividadPlanTrabajo act2 = resultado.getActividades().stream()
                .filter(a -> ActividadPlanTrabajo.CODIGO_ACT_02.equals(a.getCodigoActividad()))
                .findFirst()
                .orElseThrow();
        assertEquals(EstadoActividad.COMPLETADA, act2.getEstado());
        assertEquals(usuarioAeaId, act2.getCompletadaPor());

        // Verificamos que tenga las actividades de cronograma + actividad inicial + actividad 2 + 1.3 preservada
        assertEquals(5, resultado.getActividades().size());
        verify(planTrabajoRepository).save(plan);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el proceso no existe")
    void debeRechazarSiProcesoNoExiste() {
        UUID procesoId = UUID.randomUUID();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.empty());

        ElaborarPlanTrabajoCommand command = new ElaborarPlanTrabajoCommand(
                procesoId, "RM 1254", FaseEvaluacion.PRIMERA_FASE, LocalDate.now().plusMonths(6), List.of(), UUID.randomUUID()
        );

        assertThrows(ResourceNotFoundException.class, () -> service.elaborar(command));
    }

    @Test
    @DisplayName("Debe lanzar DomainValidationException si el proceso está cancelado o finalizado")
    void debeRechazarSiProcesoCancelado() {
        UUID procesoId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "COD", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        proceso.cancelar();
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        ElaborarPlanTrabajoCommand command = new ElaborarPlanTrabajoCommand(
                procesoId, "RM 1254", FaseEvaluacion.PRIMERA_FASE, LocalDate.now().plusMonths(6), List.of(), UUID.randomUUID()
        );

        assertThrows(DomainValidationException.class, () -> service.elaborar(command));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el registro de convocatoria no existe")
    void debeLanzarExcepcionSiRegistroConvocatoriaNoExiste() {
        UUID procesoId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "COD", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(registroRepository.findByProcesoId(procesoId)).thenReturn(Optional.empty());

        ElaborarPlanTrabajoCommand command = new ElaborarPlanTrabajoCommand(
                procesoId, "RM 1254", FaseEvaluacion.PRIMERA_FASE, LocalDate.now().plusMonths(6), List.of(), UUID.randomUUID()
        );

        assertThrows(ResourceNotFoundException.class, () -> service.elaborar(command));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el plan de trabajo no existe")
    void debeLanzarExcepcionSiPlanTrabajoNoExiste() {
        UUID procesoId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "COD", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        RegistroConvocatoria registro = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-145", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 11, 30), "Obs"
        );
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(registroRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(registro));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.empty());

        ElaborarPlanTrabajoCommand command = new ElaborarPlanTrabajoCommand(
                procesoId, "RM 1254", FaseEvaluacion.PRIMERA_FASE, LocalDate.now().plusMonths(6), List.of(), UUID.randomUUID()
        );

        assertThrows(ResourceNotFoundException.class, () -> service.elaborar(command));
    }
}
