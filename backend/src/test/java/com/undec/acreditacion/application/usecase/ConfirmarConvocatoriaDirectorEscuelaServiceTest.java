package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConfirmarConvocatoriaDirectorEscuelaCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.persistence.postgres.ConvocatoriaCarreraPlanJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataConvocatoriaCarreraPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfirmarConvocatoriaDirectorEscuelaServiceTest {

    private ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private RegistroConvocatoriaRepositoryPort registroRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private CarreraRepositoryPort carreraRepository;
    private SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository;
    private SpringDataPlanEstudioFt001Repository planEstudioRepository;
    private ConfirmarConvocatoriaDirectorEscuelaService service;

    @BeforeEach
    void setUp() {
        convocatoriaRepository = mock(ConvocatoriaEscuelaRepositoryPort.class);
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        registroRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        convocatoriaCarreraPlanRepository = mock(SpringDataConvocatoriaCarreraPlanRepository.class);
        planEstudioRepository = mock(SpringDataPlanEstudioFt001Repository.class);

        service = new ConfirmarConvocatoriaDirectorEscuelaService(
                convocatoriaRepository, procesoRepository, registroRepository,
                planTrabajoRepository, carreraRepository,
                convocatoriaCarreraPlanRepository, planEstudioRepository
        );
    }

    @Test
    @DisplayName("Debe confirmar carreras y crear proceso, registro y plan de trabajo")
    void debeConfirmarYCrearProcesos() {
        UUID convId = UUID.randomUUID();
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID usuarioDe = UUID.randomUUID();

        ConvocatoriaCarrera item1 = ConvocatoriaCarrera.crear(convId, c1, true, true);
        Convocatoria conv = Convocatoria.crear(
                escuelaId,
                "CONV-2026-EI-01",
                "Convocatoria",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-123",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "Obs",
                UUID.randomUUID(),
                List.of(item1)
        );

        Carrera carrera = Carrera.registrar(escuelaId, "Ingeniería en Sistemas", "ISI", "Ingeniería", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Título", 5, "Desc", List.of());

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(carreraRepository.findById(c1)).thenReturn(Optional.of(carrera));
        when(procesoRepository.countByAnio(anyInt())).thenReturn(0L);
        when(procesoRepository.save(any(ProcesoAcreditacion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmarConvocatoriaDirectorEscuelaCommand cmd = new ConfirmarConvocatoriaDirectorEscuelaCommand(
                convId,
                List.of(c1),
                usuarioDe
        );

        Convocatoria confirmada = service.confirmar(cmd);

        assertEquals(EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA, confirmada.getEstado());
        assertEquals(usuarioDe, confirmada.getConfirmadoPorDe());
        assertNotNull(confirmada.getCarreras().get(0).getProcesoId());

        verify(procesoRepository).save(any(ProcesoAcreditacion.class));
        verify(registroRepository).save(any(RegistroConvocatoria.class));
        verify(planTrabajoRepository).save(any(PlanTrabajo.class));
        verify(convocatoriaRepository).save(any(Convocatoria.class));
    }

    @Test
    @DisplayName("Debe fallar si una carrera confirmada tiene lista de planes vacía")
    void debeFallarSiListaDePlanesVacia() {
        UUID convId = UUID.randomUUID();
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID usuarioDe = UUID.randomUUID();

        ConvocatoriaCarrera item1 = ConvocatoriaCarrera.crear(convId, c1, true, true);
        Convocatoria conv = Convocatoria.crear(
                escuelaId, "CONV-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-1", LocalDate.now(), LocalDate.now().plusDays(5), "Obs", UUID.randomUUID(),
                List.of(item1)
        );
        Carrera carrera = Carrera.registrar(escuelaId, "ISI", "ISI", "Ing", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "T", 5, "D", List.of());

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(carreraRepository.findById(c1)).thenReturn(Optional.of(carrera));

        ConfirmarConvocatoriaDirectorEscuelaCommand cmd = new ConfirmarConvocatoriaDirectorEscuelaCommand(
                convId,
                List.of(c1),
                usuarioDe,
                Map.of(c1, List.of())
        );

        assertThrows(DomainValidationException.class, () -> service.confirmar(cmd));
    }

    @Test
    @DisplayName("Debe fallar si un plan seleccionado no es maestro o no está vigente")
    void debeFallarSiPlanNoEsMaestroOVigente() {
        UUID convId = UUID.randomUUID();
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID usuarioDe = UUID.randomUUID();

        ConvocatoriaCarrera item1 = ConvocatoriaCarrera.crear(convId, c1, true, true);
        Convocatoria conv = Convocatoria.crear(
                escuelaId, "CONV-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-1", LocalDate.now(), LocalDate.now().plusDays(5), "Obs", UUID.randomUUID(),
                List.of(item1)
        );
        Carrera carrera = Carrera.registrar(escuelaId, "ISI", "ISI", "Ing", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "T", 5, "D", List.of());

        PlanEstudioFt001JpaEntity planNoMaestro = new PlanEstudioFt001JpaEntity();
        planNoMaestro.setId(p1);
        planNoMaestro.setEsMaestro(false);
        planNoMaestro.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(carreraRepository.findById(c1)).thenReturn(Optional.of(carrera));
        when(planEstudioRepository.findById(p1)).thenReturn(Optional.of(planNoMaestro));

        ConfirmarConvocatoriaDirectorEscuelaCommand cmd = new ConfirmarConvocatoriaDirectorEscuelaCommand(
                convId,
                List.of(c1),
                usuarioDe,
                Map.of(c1, List.of(p1))
        );

        assertThrows(DomainValidationException.class, () -> service.confirmar(cmd));
    }

    @Test
    @DisplayName("Debe persistir múltiples planes en convocatoria_carrera_plan con único proceso")
    void debePersistirMultiplesPlanesEnConvocatoriaCarreraPlan() {
        UUID convId = UUID.randomUUID();
        UUID escuelaId = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID usuarioDe = UUID.randomUUID();

        ConvocatoriaCarrera item1 = ConvocatoriaCarrera.crear(convId, c1, true, true);
        Convocatoria conv = Convocatoria.crear(
                escuelaId, "CONV-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-1", LocalDate.now(), LocalDate.now().plusDays(5), "Obs", UUID.randomUUID(),
                List.of(item1)
        );
        Carrera carrera = Carrera.registrar(escuelaId, "ISI", "ISI", "Ing", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "T", 5, "D", List.of());

        PlanEstudioFt001JpaEntity plan1 = new PlanEstudioFt001JpaEntity();
        plan1.setId(p1);
        plan1.setNombrePlan("Plan 2018");
        plan1.setEsMaestro(true);
        plan1.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);

        PlanEstudioFt001JpaEntity plan2 = new PlanEstudioFt001JpaEntity();
        plan2.setId(p2);
        plan2.setNombrePlan("Plan 2024");
        plan2.setEsMaestro(true);
        plan2.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(carreraRepository.findById(c1)).thenReturn(Optional.of(carrera));
        when(planEstudioRepository.findById(p1)).thenReturn(Optional.of(plan1));
        when(planEstudioRepository.findById(p2)).thenReturn(Optional.of(plan2));
        when(procesoRepository.countByAnio(anyInt())).thenReturn(0L);
        when(procesoRepository.save(any(ProcesoAcreditacion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(convocatoriaCarreraPlanRepository.findByConvocatoriaCarreraIdAndPlanEstudioMaestroId(any(), any()))
                .thenReturn(Optional.empty());

        ConfirmarConvocatoriaDirectorEscuelaCommand cmd = new ConfirmarConvocatoriaDirectorEscuelaCommand(
                convId,
                List.of(c1),
                usuarioDe,
                Map.of(c1, List.of(p1, p2))
        );

        Convocatoria result = service.confirmar(cmd);

        assertNotNull(result);
        assertEquals(EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA, result.getEstado());
        verify(procesoRepository, times(1)).save(any(ProcesoAcreditacion.class));
        verify(convocatoriaCarreraPlanRepository, times(2)).save(any(ConvocatoriaCarreraPlanJpaEntity.class));
    }
}
