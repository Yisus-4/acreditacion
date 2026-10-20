package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.IniciarConvocatoriaCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IniciarConvocatoriaServiceTest {

    private ProcesoAcreditacionRepositoryPort procesoRepository;
    private RegistroConvocatoriaRepositoryPort registroRepository;
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private CarreraRepositoryPort carreraRepository;
    private IniciarConvocatoriaService service;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoAcreditacionRepositoryPort.class);
        registroRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);

        service = new IniciarConvocatoriaService(
                procesoRepository,
                registroRepository,
                planTrabajoRepository,
                carreraRepository
        );
    }

    private Carrera crearCarreraMock(UUID id, String nombre, String sigla, String nivel, boolean activo) {
        return Carrera.rehydrate(
                id,
                UUID.randomUUID(),
                nombre,
                sigla,
                nivel,
                "Computación",
                ModalidadCarrera.PRESENCIAL,
                EstadoCarrera.EN_FUNCIONAMIENTO,
                "Ingeniero en Sistemas",
                5,
                "Carrera de grado",
                activo,
                null,
                null,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Debe iniciar convocatoria con proceso INICIADO, RG-001 y PlanTrabajo ACTIVO con Actividad 1")
    void debeIniciarConvocatoriaExitosamente() {
        UUID carreraId = UUID.randomUUID();
        UUID creadoPor = UUID.randomUUID();
        Carrera carrera = crearCarreraMock(carreraId, "Ingeniería en Sistemas de Información", "ISI", "GRADO", true);

        int anio = LocalDate.now().getYear();
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));
        when(procesoRepository.findActivoByCarreraId(carreraId)).thenReturn(Optional.empty());
        when(procesoRepository.countByAnio(anio)).thenReturn(0L);

        when(procesoRepository.save(any(ProcesoAcreditacion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registroRepository.save(any(RegistroConvocatoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planTrabajoRepository.save(any(PlanTrabajo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IniciarConvocatoriaCommand command = new IniciarConvocatoriaCommand(
                carreraId,
                "RESFC-2026-145-APN-CONEAU#ME",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "Observaciones de prueba",
                creadoPor
        );

        ProcesoAcreditacion resultado = service.iniciar(command);

        assertNotNull(resultado);
        assertEquals(carreraId, resultado.getCarreraId());
        assertEquals(EstadoProcesoAcreditacion.INICIADO, resultado.getEstado());
        assertEquals(TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, resultado.getTipoProceso());
        assertTrue(resultado.getCodigo().startsWith("CONV-" + anio + "-ISI-"));

        // Verificar que se guardó el proceso
        verify(procesoRepository).save(any(ProcesoAcreditacion.class));

        // Verificar que se guardó el registro CONEAU RG-001
        ArgumentCaptor<RegistroConvocatoria> registroCaptor = ArgumentCaptor.forClass(RegistroConvocatoria.class);
        verify(registroRepository).save(registroCaptor.capture());
        RegistroConvocatoria registroGuardado = registroCaptor.getValue();
        assertEquals(resultado.getId(), registroGuardado.getProcesoId());
        assertEquals("RESFC-2026-145-APN-CONEAU#ME", registroGuardado.getNroResolucionConeau());
        assertEquals(LocalDate.now(), registroGuardado.getFechaInicio());
        assertEquals(LocalDate.now().plusMonths(6), registroGuardado.getFechaCierre());

        // Verificar que se guardó el Plan de Trabajo con la actividad inicial y normativa en PENDIENTE
        ArgumentCaptor<PlanTrabajo> planCaptor = ArgumentCaptor.forClass(PlanTrabajo.class);
        verify(planTrabajoRepository).save(planCaptor.capture());
        PlanTrabajo planGuardado = planCaptor.getValue();
        assertEquals(resultado.getId(), planGuardado.getProcesoId());
        assertEquals(EstadoPlanTrabajo.ACTIVO, planGuardado.getEstado());
        assertEquals(2, planGuardado.getActividades().size());
        assertEquals(EstadoActividad.PENDIENTE, planGuardado.getActividades().get(0).getEstado());
        assertEquals(EstadoActividad.PENDIENTE, planGuardado.getActividades().get(1).getEstado());
    }

    @Test
    @DisplayName("Debe rechazar si la carrera no existe")
    void debeRechazarSiCarreraNoExiste() {
        UUID carreraId = UUID.randomUUID();
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.empty());

        IniciarConvocatoriaCommand command = new IniciarConvocatoriaCommand(
                carreraId,
                "RESFC-2026-145",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                null,
                UUID.randomUUID()
        );

        assertThrows(ResourceNotFoundException.class, () -> service.iniciar(command));
    }

    @Test
    @DisplayName("Debe rechazar si la carrera no está activa")
    void debeRechazarSiCarreraInactiva() {
        UUID carreraId = UUID.randomUUID();
        Carrera carreraInactiva = crearCarreraMock(carreraId, "ISI", "ISI", "GRADO", false);
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carreraInactiva));

        IniciarConvocatoriaCommand command = new IniciarConvocatoriaCommand(
                carreraId,
                "RESFC-2026-145",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                null,
                UUID.randomUUID()
        );

        assertThrows(DomainValidationException.class, () -> service.iniciar(command));
    }

    @Test
    @DisplayName("Debe rechazar si ya existe un proceso activo para la carrera")
    void debeRechazarSiExisteProcesoActivo() {
        UUID carreraId = UUID.randomUUID();
        Carrera carrera = crearCarreraMock(carreraId, "ISI", "ISI", "GRADO", true);
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        ProcesoAcreditacion procesoExistente = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026-ISI-01", "Titulo", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        when(procesoRepository.findActivoByCarreraId(carreraId)).thenReturn(Optional.of(procesoExistente));

        IniciarConvocatoriaCommand command = new IniciarConvocatoriaCommand(
                carreraId,
                "RESFC-2026-145",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                null,
                UUID.randomUUID()
        );

        assertThrows(BusinessRuleConflictException.class, () -> service.iniciar(command));
    }

    @Test
    @DisplayName("Debe rechazar si la fecha de inicio es anterior a la fecha actual")
    void debeRechazarSiFechaInicioEsAnteriorAHoy() {
        UUID carreraId = UUID.randomUUID();
        DomainValidationException ex = assertThrows(DomainValidationException.class, () ->
                new IniciarConvocatoriaCommand(
                        carreraId,
                        "RESFC-2026-145",
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusMonths(6),
                        TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                        null,
                        UUID.randomUUID()
                )
        );
        assertEquals("La fecha de inicio no puede ser anterior a la fecha actual", ex.getMessage());
    }

    @Test
    @DisplayName("Debe rechazar si la fecha de cierre es anterior a la fecha actual")
    void debeRechazarSiFechaCierreEsAnteriorAHoy() {
        UUID carreraId = UUID.randomUUID();
        DomainValidationException ex = assertThrows(DomainValidationException.class, () ->
                new IniciarConvocatoriaCommand(
                        carreraId,
                        "RESFC-2026-145",
                        LocalDate.now(),
                        LocalDate.now().minusDays(1),
                        TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                        null,
                        UUID.randomUUID()
                )
        );
        assertEquals("La fecha de cierre no puede ser anterior a la fecha actual", ex.getMessage());
    }

    @Test
    @DisplayName("Debe asignar automáticamente la fecha actual si la fecha de inicio es nula")
    void debeAsignarFechaInicioActualSiEsNula() {
        UUID carreraId = UUID.randomUUID();
        UUID creadoPor = UUID.randomUUID();
        Carrera carrera = crearCarreraMock(carreraId, "Ingeniería en Sistemas de Información", "ISI", "GRADO", true);
        int anio = LocalDate.now().getYear();

        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));
        when(procesoRepository.findActivoByCarreraId(carreraId)).thenReturn(Optional.empty());
        when(procesoRepository.countByAnio(anio)).thenReturn(0L);

        when(procesoRepository.save(any(ProcesoAcreditacion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registroRepository.save(any(RegistroConvocatoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(planTrabajoRepository.save(any(PlanTrabajo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IniciarConvocatoriaCommand command = new IniciarConvocatoriaCommand(
                carreraId,
                "RESFC-2026-145-APN-CONEAU#ME",
                null,
                LocalDate.now().plusMonths(6),
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "Sin fecha de inicio especificada",
                creadoPor
        );

        assertEquals(LocalDate.now(), command.fechaInicio());

        ProcesoAcreditacion resultado = service.iniciar(command);
        assertNotNull(resultado);

        ArgumentCaptor<RegistroConvocatoria> registroCaptor = ArgumentCaptor.forClass(RegistroConvocatoria.class);
        verify(registroRepository).save(registroCaptor.capture());
        RegistroConvocatoria registroGuardado = registroCaptor.getValue();
        assertEquals(LocalDate.now(), registroGuardado.getFechaInicio());
    }
}
