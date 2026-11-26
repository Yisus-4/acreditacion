package com.undec.acreditacion.infrastructure.designacion;

import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.designacion.dto.*;
import com.undec.acreditacion.infrastructure.designacion.persistence.*;
import com.undec.acreditacion.infrastructure.designacion.service.SolicitudDesignacionService;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudDesignacionFase3ServiceTest {

    @Mock private SpringDataSolicitudDesignacionRepository solicitudRepository;
    @Mock private SpringDataSolicitudDocenteDesignacionRepository designacionRepository;
    @Mock private SpringDataSolicitudAsignacionCurricularRepository asignacionRepository;
    @Mock private SpringDataSolicitudDesignacionCambioRepository cambioRepository;
    @Mock private SpringDataDepartamentoAcademicoRepository departamentoRepository;
    @Mock private SpringDataPeriodoAcademicoRepository periodoRepository;
    @Mock private SpringDataCargoDocenteRepository cargoRepository;
    @Mock private SpringDataDedicacionDocenteRepository dedicacionRepository;
    @Mock private SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository;
    @Mock private SpringDataCuerpoAcademicoMaestroRepository docenteRepository;
    @Mock private SpringDataCarreraRepository carreraRepository;
    @Mock private SpringDataPlanEstudioFt001Repository planRepository;
    @Mock private SpringDataActividadCurricularPlanRepository actividadRepository;
    @Mock private AuthenticatedUserProvider authenticatedUserProvider;

    private SolicitudDesignacionService service;

    private DepartamentoAcademicoJpaEntity deptoCyT;
    private PeriodoAcademicoJpaEntity periodo2025;
    private PeriodoAcademicoJpaEntity periodo2026;
    private CuerpoAcademicoMaestroJpaEntity docente;
    private CargoDocenteJpaEntity cargoAdjunto;
    private DedicacionDocenteJpaEntity dedExclusiva40;
    private DedicacionDocenteJpaEntity dedSemi20;
    private TipoDesignacionDocenteJpaEntity tipoOrdinario;

    @BeforeEach
    void setUp() {
        service = new SolicitudDesignacionService(
                solicitudRepository,
                designacionRepository,
                asignacionRepository,
                departamentoRepository,
                periodoRepository,
                cargoRepository,
                dedicacionRepository,
                tipoDesignacionRepository,
                docenteRepository,
                carreraRepository,
                planRepository,
                actividadRepository,
                authenticatedUserProvider,
                cambioRepository
        );

        deptoCyT = new DepartamentoAcademicoJpaEntity();
        deptoCyT.setId(UUID.randomUUID());
        deptoCyT.setCodigo("CYT");
        deptoCyT.setDenominacion("Departamento de Ciencias Básicas y Tecnológicas");

        periodo2025 = new PeriodoAcademicoJpaEntity();
        periodo2025.setId(UUID.randomUUID());
        periodo2025.setCodigo("2025");
        periodo2025.setDenominacion("Ciclo Lectivo 2025");
        periodo2025.setAnio(2025);
        periodo2025.setActivo(true);

        periodo2026 = new PeriodoAcademicoJpaEntity();
        periodo2026.setId(UUID.randomUUID());
        periodo2026.setCodigo("2026");
        periodo2026.setDenominacion("Ciclo Lectivo 2026");
        periodo2026.setAnio(2026);
        periodo2026.setActivo(true);

        docente = new CuerpoAcademicoMaestroJpaEntity();
        docente.setId(UUID.randomUUID());
        docente.setDni("30111222");
        docente.setLegajo("LEG-100");
        docente.setApellido("Gómez");
        docente.setNombre("Ana");
        docente.setActivo(true);
        docente.setDepartamentoAcademico(deptoCyT);

        cargoAdjunto = new CargoDocenteJpaEntity();
        cargoAdjunto.setId(UUID.randomUUID());
        cargoAdjunto.setCodigo("ADJUNTO");
        cargoAdjunto.setNombre("Profesor Adjunto");
        cargoAdjunto.setActivo(true);

        dedExclusiva40 = new DedicacionDocenteJpaEntity();
        dedExclusiva40.setId(UUID.randomUUID());
        dedExclusiva40.setCodigo("EXCLUSIVA");
        dedExclusiva40.setNombre("Exclusiva");
        dedExclusiva40.setHorasSemanales(40);
        dedExclusiva40.setActivo(true);

        dedSemi20 = new DedicacionDocenteJpaEntity();
        dedSemi20.setId(UUID.randomUUID());
        dedSemi20.setCodigo("PARCIAL");
        dedSemi20.setNombre("Parcial");
        dedSemi20.setHorasSemanales(20);
        dedSemi20.setActivo(true);

        tipoOrdinario = new TipoDesignacionDocenteJpaEntity();
        tipoOrdinario.setId(UUID.randomUUID());
        tipoOrdinario.setCodigo("REGULAR_RENTADO");
        tipoOrdinario.setNombre("Regular Rentado");
        tipoOrdinario.setActivo(true);
    }

    @Test
    void crearSolicitudConClonacion_replicaActivasFiltraRN09YRegistraOrigen() {
        SolicitudDesignacionJpaEntity sol2025 = new SolicitudDesignacionJpaEntity();
        sol2025.setId(UUID.randomUUID());
        sol2025.setDepartamentoAcademico(deptoCyT);
        sol2025.setPeriodoAcademico(periodo2025);
        sol2025.setTipoSolicitud(TipoSolicitudDesignacion.ANUAL);
        sol2025.setEstado(EstadoSolicitudDesignacion.APROBADA);

        SolicitudDocenteDesignacionJpaEntity itemActivo = new SolicitudDocenteDesignacionJpaEntity();
        itemActivo.setId(UUID.randomUUID());
        itemActivo.setSolicitud(sol2025);
        itemActivo.setDocente(docente);
        itemActivo.setCargoDocente(cargoAdjunto);
        itemActivo.setDedicacionDocente(dedExclusiva40);
        itemActivo.setTipoDesignacion(tipoOrdinario);
        itemActivo.setHorasDedicacion(40);
        itemActivo.setHorasClases(20);
        itemActivo.setHorasInvestigacion(20);
        itemActivo.setEstadoItem(EstadoItemDesignacion.NUEVO);

        SolicitudDocenteDesignacionJpaEntity itemExcluido = new SolicitudDocenteDesignacionJpaEntity();
        itemExcluido.setId(UUID.randomUUID());
        itemExcluido.setSolicitud(sol2025);
        itemExcluido.setDocente(docente);
        itemExcluido.setCargoDocente(cargoAdjunto);
        itemExcluido.setDedicacionDocente(dedSemi20);
        itemExcluido.setTipoDesignacion(tipoOrdinario);
        itemExcluido.setHorasDedicacion(20);
        itemExcluido.setEstadoItem(EstadoItemDesignacion.EXCLUIDO);

        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setId(UUID.randomUUID());
        carrera.setNombre("Ing. Sistemas");
        carrera.setSigla("IS");

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setCarrera(carrera);
        plan.setNombrePlan("Plan 2020");
        plan.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);

        ActividadCurricularPlanJpaEntity actActiva = new ActividadCurricularPlanJpaEntity();
        actActiva.setId(UUID.randomUUID());
        actActiva.setPlanEstudio(plan);
        actActiva.setNombre("Álgebra");
        actActiva.setEstadoDictado(EstadoDictadoActividad.ACTIVA);

        ActividadCurricularPlanJpaEntity actInactiva = new ActividadCurricularPlanJpaEntity();
        actInactiva.setId(UUID.randomUUID());
        actInactiva.setPlanEstudio(plan);
        actInactiva.setNombre("Materia Cerrada");
        actInactiva.setEstadoDictado(EstadoDictadoActividad.INACTIVA);

        ActividadCurricularPlanJpaEntity actSoloFinales = new ActividadCurricularPlanJpaEntity();
        actSoloFinales.setId(UUID.randomUUID());
        actSoloFinales.setPlanEstudio(plan);
        actSoloFinales.setNombre("Física Plan Viejo");
        actSoloFinales.setEstadoDictado(EstadoDictadoActividad.SOLO_EXAMENES_FINALES);

        SolicitudAsignacionCurricularJpaEntity asigOk = asig(itemActivo, carrera, plan, actActiva, ModalidadParticipacionDocente.DESARROLLO_CLASES);
        SolicitudAsignacionCurricularJpaEntity asigInactiva = asig(itemActivo, carrera, plan, actInactiva, ModalidadParticipacionDocente.DESARROLLO_CLASES);
        SolicitudAsignacionCurricularJpaEntity asigClasesEnSoloFinales = asig(itemActivo, carrera, plan, actSoloFinales, ModalidadParticipacionDocente.DESARROLLO_CLASES);

        List<SolicitudDocenteDesignacionJpaEntity> guardadas = new ArrayList<>();
        List<SolicitudAsignacionCurricularJpaEntity> asigsGuardadas = new ArrayList<>();

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(departamentoRepository.findById(deptoCyT.getId())).thenReturn(Optional.of(deptoCyT));
        when(periodoRepository.findById(periodo2026.getId())).thenReturn(Optional.of(periodo2026));
        when(solicitudRepository.findByDepartamentoAcademicoIdAndPeriodoAcademicoIdAndTipoSolicitud(
                deptoCyT.getId(), periodo2026.getId(), TipoSolicitudDesignacion.ANUAL)).thenReturn(Optional.empty());
        when(solicitudRepository.findCandidatasAnualesOrigenClonacion(
                eq(deptoCyT.getId()), eq(2026),
                eq(List.of(EstadoSolicitudDesignacion.APROBADA, EstadoSolicitudDesignacion.PRESENTADA_SGA))))
                .thenReturn(List.of(sol2025));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(sol2025.getId()))
                .thenReturn(List.of(itemActivo, itemExcluido));
        when(asignacionRepository.findByDesignacionIdInOrderByCreadoEnAsc(List.of(itemActivo.getId())))
                .thenReturn(List.of(asigOk, asigInactiva, asigClasesEnSoloFinales));
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenAnswer(inv -> guardadas);
        when(designacionRepository.save(any())).thenAnswer(inv -> {
            SolicitudDocenteDesignacionJpaEntity d = inv.getArgument(0);
            guardadas.add(d);
            return d;
        });
        when(asignacionRepository.save(any())).thenAnswer(inv -> {
            SolicitudAsignacionCurricularJpaEntity a = inv.getArgument(0);
            asigsGuardadas.add(a);
            return a;
        });
        when(designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(argThat(id -> id != null && !id.equals(sol2025.getId()))))
                .thenAnswer(inv -> guardadas);
        when(asignacionRepository.findByDesignacionIdInOrderByCreadoEnAsc(argThat(ids -> ids != null && !ids.contains(itemActivo.getId()))))
                .thenAnswer(inv -> asigsGuardadas);
        when(cambioRepository.findBySolicitudIdOrderByFechaCambioDesc(any())).thenReturn(List.of());

        SolicitudDesignacionDetalleDto creada = service.crearSolicitud(new CrearSolicitudDesignacionRequest(
                deptoCyT.getId(), periodo2026.getId(), TipoSolicitudDesignacion.ANUAL, "Clonada 2026", true, null));

        assertEquals(sol2025.getId(), creada.solicitudOrigenId());
        assertEquals(1, creada.designaciones().size(), "Solo debe clonar el renglón activo, no el EXCLUIDO");
        DesignacionDocenteItemDto clon = creada.designaciones().get(0);
        assertEquals(EstadoItemDesignacion.REPLICADO, clon.estadoItem());
        assertEquals(itemActivo.getId(), clon.designacionOrigenId());
        assertEquals(1, clon.asignaciones().size(), "Debe omitir INACTIVA y DESARROLLO_CLASES sobre SOLO_EXAMENES_FINALES");
        assertEquals("Álgebra", clon.asignaciones().get(0).actividadNombre());
    }

    @Test
    void modificarExcluirYReactivarItemReplicado_exigeMotivoAuditaYValidaTope50AlReactivar() {
        SolicitudDesignacionJpaEntity sol = new SolicitudDesignacionJpaEntity();
        sol.setId(UUID.randomUUID());
        sol.setDepartamentoAcademico(deptoCyT);
        sol.setPeriodoAcademico(periodo2026);
        sol.setTipoSolicitud(TipoSolicitudDesignacion.ANUAL);
        sol.setEstado(EstadoSolicitudDesignacion.BORRADOR);

        SolicitudDocenteDesignacionJpaEntity itemReplicado = new SolicitudDocenteDesignacionJpaEntity();
        itemReplicado.setId(UUID.randomUUID());
        itemReplicado.setSolicitud(sol);
        itemReplicado.setDocente(docente);
        itemReplicado.setCargoDocente(cargoAdjunto);
        itemReplicado.setDedicacionDocente(dedSemi20);
        itemReplicado.setTipoDesignacion(tipoOrdinario);
        itemReplicado.setHorasDedicacion(20);
        itemReplicado.setEstadoItem(EstadoItemDesignacion.REPLICADO);
        itemReplicado.setDesignacionOrigenId(UUID.randomUUID());

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(designacionRepository.findById(itemReplicado.getId())).thenReturn(Optional.of(itemReplicado));

        // 1) Modificar REPLICADO sin motivo -> 422
        assertThrows(DomainValidationException.class, () -> service.actualizarDistribucionHoraria(
                sol.getId(), itemReplicado.getId(),
                new GuardarDistribucionHorariaRequest(10, 10, 0, 0, 0, 0, "  ")));

        // 2) Excluir sin motivo -> 422
        assertThrows(DomainValidationException.class, () -> service.excluirDesignacion(
                sol.getId(), itemReplicado.getId(), new MotivoCambioRequest("")));

        // 3) Excluir con motivo -> pasa a EXCLUIDO y graba auditoría
        when(designacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of());
        when(asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(itemReplicado.getId())).thenReturn(List.of());

        DesignacionDocenteItemDto excluido = service.excluirDesignacion(
                sol.getId(), itemReplicado.getId(), new MotivoCambioRequest("Jubilación del docente"));
        assertEquals(EstadoItemDesignacion.EXCLUIDO, excluido.estadoItem());
        assertFalse(excluido.tieneAlertaHorasSinDistribuir());

        ArgumentCaptor<SolicitudDesignacionCambioJpaEntity> captor = ArgumentCaptor.forClass(SolicitudDesignacionCambioJpaEntity.class);
        verify(cambioRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(c -> c.getTipoEvento() == TipoEventoSolicitudDesignacion.EXCLUSION_DESIGNACION
                        && "Jubilación del docente".equals(c.getMotivo())));

        // 4) Intentar editar un ítem EXCLUIDO -> 422
        assertThrows(DomainValidationException.class, () -> service.actualizarDistribucionHoraria(
                sol.getId(), itemReplicado.getId(),
                new GuardarDistribucionHorariaRequest(5, 5, 0, 0, 0, 0, "Intento inválido")));

        // 5) Reactivar cuando ya tiene otro cargo de 40h (40 + 20 = 60 > 50) -> 422 (RN02)
        SolicitudDocenteDesignacionJpaEntity otro40 = new SolicitudDocenteDesignacionJpaEntity();
        otro40.setId(UUID.randomUUID());
        otro40.setHorasDedicacion(40);
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of(otro40));

        assertThrows(DomainValidationException.class, () -> service.reactivarDesignacion(
                sol.getId(), itemReplicado.getId(), new MotivoCambioRequest("Reincorporación")));

        // 6) Reactivar con 0h previas -> vuelve a MODIFICADO
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of());
        DesignacionDocenteItemDto reactivado = service.reactivarDesignacion(
                sol.getId(), itemReplicado.getId(), new MotivoCambioRequest("Reincorporación aprobada"));
        assertEquals(EstadoItemDesignacion.MODIFICADO, reactivado.estadoItem());
    }

    @Test
    void flujoSga_validaSolicitudesVaciasPermisosYObservacionesObligatorias() {
        SolicitudDesignacionJpaEntity sol = new SolicitudDesignacionJpaEntity();
        sol.setId(UUID.randomUUID());
        sol.setDepartamentoAcademico(deptoCyT);
        sol.setPeriodoAcademico(periodo2026);
        sol.setTipoSolicitud(TipoSolicitudDesignacion.ANUAL);
        sol.setEstado(EstadoSolicitudDesignacion.BORRADOR);

        User directorCyT = User.rehydrate(
                UUID.randomUUID(), "dir.cyt", "dir.cyt@undec.edu.ar", "hash", true, false,
                null, deptoCyT.getId(),
                Set.of(Role.system(UUID.randomUUID(), CodigosRol.DIRECTOR_DEPARTAMENTO, "Director Dpto", "Desc"))
        );
        User sgaUser = User.rehydrate(
                UUID.randomUUID(), "sga", "sga@undec.edu.ar", "hash", true, false,
                null, null,
                Set.of(Role.system(UUID.randomUUID(), CodigosRol.SECRETARIA_ACADEMICA, "SGA", "Desc"))
        );

        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cambioRepository.findBySolicitudIdOrderByFechaCambioDesc(sol.getId())).thenReturn(List.of());

        // 1) Presentar solicitud sin designaciones activas -> 422
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(directorCyT));
        when(designacionRepository.countBySolicitudIdAndEstadoItemNot(sol.getId(), EstadoItemDesignacion.EXCLUIDO))
                .thenReturn(0L);
        assertThrows(DomainValidationException.class, () -> service.presentarSolicitudASga(sol.getId()));

        // 2) Presentar con 1 designación activa -> PRESENTADA_SGA
        when(designacionRepository.countBySolicitudIdAndEstadoItemNot(sol.getId(), EstadoItemDesignacion.EXCLUIDO))
                .thenReturn(1L);
        when(designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(sol.getId())).thenReturn(List.of());
        SolicitudDesignacionDetalleDto presentada = service.presentarSolicitudASga(sol.getId());
        assertEquals(EstadoSolicitudDesignacion.PRESENTADA_SGA, presentada.estado());
        assertNotNull(presentada.fechaEmision());

        // 3) Director de Departamento NO puede aprobar ni observar -> 422
        assertThrows(DomainValidationException.class, () -> service.aprobarSolicitudSga(sol.getId(), new RevisionSgaRequest("OK")));
        assertThrows(DomainValidationException.class, () -> service.observarSolicitudSga(sol.getId(), new RevisionSgaRequest("Falta dato")));

        // 4) SGA observar sin motivo -> 422; con motivo -> OBSERVADA
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(sgaUser));
        assertThrows(DomainValidationException.class, () -> service.observarSolicitudSga(sol.getId(), new RevisionSgaRequest("   ")));

        SolicitudDesignacionDetalleDto observada = service.observarSolicitudSga(
                sol.getId(), new RevisionSgaRequest("Distribuir horas restantes de Gómez"));
        assertEquals(EstadoSolicitudDesignacion.OBSERVADA, observada.estado());
        assertTrue(observada.editable());

        // 5) Re-presentar y aprobar por SGA -> APROBADA
        service.presentarSolicitudASga(sol.getId());
        SolicitudDesignacionDetalleDto aprobada = service.aprobarSolicitudSga(
                sol.getId(), new RevisionSgaRequest("Conformidad SGA"));
        assertEquals(EstadoSolicitudDesignacion.APROBADA, aprobada.estado());
        assertFalse(aprobada.editable());
    }

    @Test
    void agregarDesignacion_rechazaCatalogoInactivo() {
        SolicitudDesignacionJpaEntity sol = new SolicitudDesignacionJpaEntity();
        sol.setId(UUID.randomUUID());
        sol.setDepartamentoAcademico(deptoCyT);
        sol.setPeriodoAcademico(periodo2026);
        sol.setEstado(EstadoSolicitudDesignacion.BORRADOR);

        cargoAdjunto.setActivo(false);
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(docenteRepository.findById(docente.getId())).thenReturn(Optional.of(docente));
        when(cargoRepository.findById(cargoAdjunto.getId())).thenReturn(Optional.of(cargoAdjunto));
        when(dedicacionRepository.findById(dedSemi20.getId())).thenReturn(Optional.of(dedSemi20));
        when(tipoDesignacionRepository.findById(tipoOrdinario.getId())).thenReturn(Optional.of(tipoOrdinario));

        assertThrows(DomainValidationException.class, () -> service.agregarDesignacion(
                sol.getId(),
                new GuardarDesignacionDocenteRequest(docente.getId(), cargoAdjunto.getId(), dedSemi20.getId(), tipoOrdinario.getId(), null, null)));
    }

    @Test
    void reactivarDesignacion_revalidaRN09EnAsignaturasRetenidas() {
        SolicitudDesignacionJpaEntity sol = new SolicitudDesignacionJpaEntity();
        sol.setId(UUID.randomUUID());
        sol.setDepartamentoAcademico(deptoCyT);
        sol.setPeriodoAcademico(periodo2026);
        sol.setEstado(EstadoSolicitudDesignacion.BORRADOR);

        SolicitudDocenteDesignacionJpaEntity itemExcluido = new SolicitudDocenteDesignacionJpaEntity();
        itemExcluido.setId(UUID.randomUUID());
        itemExcluido.setSolicitud(sol);
        itemExcluido.setDocente(docente);
        itemExcluido.setCargoDocente(cargoAdjunto);
        itemExcluido.setDedicacionDocente(dedSemi20);
        itemExcluido.setTipoDesignacion(tipoOrdinario);
        itemExcluido.setHorasDedicacion(20);
        itemExcluido.setEstadoItem(EstadoItemDesignacion.EXCLUIDO);
        itemExcluido.setDesignacionOrigenId(UUID.randomUUID());

        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setId(UUID.randomUUID());
        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setCarrera(carrera);
        ActividadCurricularPlanJpaEntity actCerrada = new ActividadCurricularPlanJpaEntity();
        actCerrada.setId(UUID.randomUUID());
        actCerrada.setPlanEstudio(plan);
        actCerrada.setNombre("Química Inactiva");
        actCerrada.setEstadoDictado(EstadoDictadoActividad.INACTIVA);

        SolicitudAsignacionCurricularJpaEntity asigInvalida = asig(
                itemExcluido, carrera, plan, actCerrada, ModalidadParticipacionDocente.DESARROLLO_CLASES);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(designacionRepository.findById(itemExcluido.getId())).thenReturn(Optional.of(itemExcluido));
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of());
        when(asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(itemExcluido.getId()))
                .thenReturn(List.of(asigInvalida));

        assertThrows(DomainValidationException.class, () -> service.reactivarDesignacion(
                sol.getId(), itemExcluido.getId(), new MotivoCambioRequest("Intento con asignatura inactiva")));
    }

    @Test
    void solicitudExtraordinaria_reemplazaCargoDeAnualAprobadaSinExceder50HorasYConsolida() {
        SolicitudDesignacionJpaEntity solAnualAprobada = new SolicitudDesignacionJpaEntity();
        solAnualAprobada.setId(UUID.randomUUID());
        solAnualAprobada.setDepartamentoAcademico(deptoCyT);
        solAnualAprobada.setPeriodoAcademico(periodo2026);
        solAnualAprobada.setTipoSolicitud(TipoSolicitudDesignacion.ANUAL);
        solAnualAprobada.setEstado(EstadoSolicitudDesignacion.APROBADA);

        SolicitudDocenteDesignacionJpaEntity cargoOriginal40 = new SolicitudDocenteDesignacionJpaEntity();
        cargoOriginal40.setId(UUID.randomUUID());
        cargoOriginal40.setSolicitud(solAnualAprobada);
        cargoOriginal40.setDocente(docente);
        cargoOriginal40.setCargoDocente(cargoAdjunto);
        cargoOriginal40.setDedicacionDocente(dedExclusiva40);
        cargoOriginal40.setTipoDesignacion(tipoOrdinario);
        cargoOriginal40.setHorasDedicacion(40);
        cargoOriginal40.setEstadoItem(EstadoItemDesignacion.NUEVO);

        SolicitudDesignacionJpaEntity solExtra = new SolicitudDesignacionJpaEntity();
        solExtra.setId(UUID.randomUUID());
        solExtra.setDepartamentoAcademico(deptoCyT);
        solExtra.setPeriodoAcademico(periodo2026);
        solExtra.setTipoSolicitud(TipoSolicitudDesignacion.EXTRAORDINARIA);
        solExtra.setEstado(EstadoSolicitudDesignacion.BORRADOR);
        solExtra.setSolicitudOrigenId(solAnualAprobada.getId());

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(solExtra.getId())).thenReturn(Optional.of(solExtra));
        when(designacionRepository.findById(cargoOriginal40.getId())).thenReturn(Optional.of(cargoOriginal40));
        when(docenteRepository.findById(docente.getId())).thenReturn(Optional.of(docente));
        when(cargoRepository.findById(cargoAdjunto.getId())).thenReturn(Optional.of(cargoAdjunto));
        when(dedicacionRepository.findById(dedExclusiva40.getId())).thenReturn(Optional.of(dedExclusiva40));
        when(tipoDesignacionRepository.findById(tipoOrdinario.getId())).thenReturn(Optional.of(tipoOrdinario));
        // Al pasar designacionReemplazadaId = cargoOriginal40.id, sus 40h previas se descuentan -> 0 + 40 = 40 <= 50
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of(cargoOriginal40));
        List<SolicitudDocenteDesignacionJpaEntity> extraItems = new ArrayList<>();
        when(designacionRepository.save(any())).thenAnswer(inv -> {
            SolicitudDocenteDesignacionJpaEntity saved = inv.getArgument(0);
            extraItems.add(saved);
            return saved;
        });
        when(asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(any())).thenReturn(List.of());

        DesignacionDocenteItemDto reemplazo = service.agregarDesignacion(
                solExtra.getId(),
                new GuardarDesignacionDocenteRequest(
                        docente.getId(), cargoAdjunto.getId(), dedExclusiva40.getId(), tipoOrdinario.getId(),
                        "MAP-99", "Cambio de carácter en extraordinaria", "Concurso ganado", cargoOriginal40.getId()
                )
        );

        assertEquals(cargoOriginal40.getId(), reemplazo.designacionOrigenId());
        assertEquals(40, reemplazo.totalHorasDocentePeriodo());

        // Verificar Vista Consolidada del Período: cargoOriginal40 queda superado por reemplazo -> solo 1 cargo vigente (40h)
        when(departamentoRepository.findById(deptoCyT.getId())).thenReturn(Optional.of(deptoCyT));
        when(periodoRepository.findById(periodo2026.getId())).thenReturn(Optional.of(periodo2026));
        when(solicitudRepository.findByDepartamentoAcademicoIdAndPeriodoAcademicoIdOrderByCreadoEnDesc(
                deptoCyT.getId(), periodo2026.getId())).thenReturn(List.of(solExtra, solAnualAprobada));
        when(designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(solExtra.getId())).thenReturn(extraItems);
        when(designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(solAnualAprobada.getId())).thenReturn(List.of(cargoOriginal40));
        when(asignacionRepository.findByDesignacionIdInOrderByCreadoEnAsc(any())).thenReturn(List.of());

        VistaConsolidadaPeriodoDto consolidado = service.obtenerVistaConsolidadaPeriodo(deptoCyT.getId(), periodo2026.getId());
        assertEquals(2, consolidado.totalSolicitudesIncluidas());
        assertEquals(1, consolidado.totalDesignacionesVigentes());
        assertEquals("MAP-99", consolidado.designacionesVigentes().get(0).nroDesignacionMapuche());
    }

    private static SolicitudAsignacionCurricularJpaEntity asig(
            SolicitudDocenteDesignacionJpaEntity desig,
            CarreraJpaEntity carrera,
            PlanEstudioFt001JpaEntity plan,
            ActividadCurricularPlanJpaEntity act,
            ModalidadParticipacionDocente modalidad
    ) {
        SolicitudAsignacionCurricularJpaEntity a = new SolicitudAsignacionCurricularJpaEntity();
        a.setId(UUID.randomUUID());
        a.setDesignacion(desig);
        a.setCarrera(carrera);
        a.setPlanEstudio(plan);
        a.setActividadCurricular(act);
        a.setModalidadParticipacion(modalidad);
        a.setHorasAsignadas(4);
        return a;
    }
}
