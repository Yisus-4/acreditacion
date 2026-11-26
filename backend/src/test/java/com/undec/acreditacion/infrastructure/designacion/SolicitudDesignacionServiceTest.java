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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudDesignacionServiceTest {

    @Mock private SpringDataSolicitudDesignacionRepository solicitudRepository;
    @Mock private SpringDataSolicitudDocenteDesignacionRepository designacionRepository;
    @Mock private SpringDataSolicitudAsignacionCurricularRepository asignacionRepository;
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
    private DepartamentoAcademicoJpaEntity deptoSociales;
    private PeriodoAcademicoJpaEntity periodo2026;
    private CuerpoAcademicoMaestroJpaEntity docente;
    private CargoDocenteJpaEntity cargoAdjunto;
    private DedicacionDocenteJpaEntity dedExclusiva40;
    private DedicacionDocenteJpaEntity dedSemi20;
    private DedicacionDocenteJpaEntity dedSimple10;
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
                authenticatedUserProvider
        );

        deptoCyT = new DepartamentoAcademicoJpaEntity();
        deptoCyT.setId(UUID.randomUUID());
        deptoCyT.setCodigo("CYT");
        deptoCyT.setDenominacion("Departamento de Ciencias Básicas y Tecnológicas");

        deptoSociales = new DepartamentoAcademicoJpaEntity();
        deptoSociales.setId(UUID.randomUUID());
        deptoSociales.setCodigo("SOC");
        deptoSociales.setDenominacion("Departamento de Ciencias Sociales");

        periodo2026 = new PeriodoAcademicoJpaEntity();
        periodo2026.setId(UUID.randomUUID());
        periodo2026.setCodigo("2026-ANUAL");
        periodo2026.setDenominacion("Ciclo Lectivo 2026");
        periodo2026.setAnio(2026);
        periodo2026.setActivo(true);

        docente = new CuerpoAcademicoMaestroJpaEntity();
        docente.setId(UUID.randomUUID());
        docente.setDni("30111222");
        docente.setLegajo("LEG-100");
        docente.setApellido("Gómez");
        docente.setNombre("Ana");
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
        dedSemi20.setCodigo("SEMIEXCLUSIVA");
        dedSemi20.setNombre("Semiexclusiva");
        dedSemi20.setHorasSemanales(20);
        dedSemi20.setActivo(true);

        dedSimple10 = new DedicacionDocenteJpaEntity();
        dedSimple10.setId(UUID.randomUUID());
        dedSimple10.setCodigo("SIMPLE");
        dedSimple10.setNombre("Simple");
        dedSimple10.setHorasSemanales(10);
        dedSimple10.setActivo(true);

        tipoOrdinario = new TipoDesignacionDocenteJpaEntity();
        tipoOrdinario.setId(UUID.randomUUID());
        tipoOrdinario.setCodigo("ORDINARIO");
        tipoOrdinario.setNombre("Ordinario");
        tipoOrdinario.setActivo(true);
    }

    @Test
    void crearSolicitudAnual_rechazaDuplicadaEnMismoDepartamentoYPeriodo() {
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(departamentoRepository.findById(deptoCyT.getId())).thenReturn(Optional.of(deptoCyT));
        when(periodoRepository.findById(periodo2026.getId())).thenReturn(Optional.of(periodo2026));
        when(solicitudRepository.findByDepartamentoAcademicoIdAndPeriodoAcademicoIdAndTipoSolicitud(
                deptoCyT.getId(), periodo2026.getId(), TipoSolicitudDesignacion.ANUAL))
                .thenReturn(Optional.of(solicitudBorrador(deptoCyT)));

        assertThrows(DomainValidationException.class, () -> service.crearSolicitud(
                new CrearSolicitudDesignacionRequest(deptoCyT.getId(), periodo2026.getId(), TipoSolicitudDesignacion.ANUAL, null)));
    }

    @Test
    void agregarDesignacion_permiteMultiplesCargosHasta50HorasYRechazaSiSupera50() {
        SolicitudDesignacionJpaEntity sol = solicitudBorrador(deptoCyT);
        SolicitudDocenteDesignacionJpaEntity existente40 = designacion(sol, docente, dedExclusiva40);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(docenteRepository.findById(docente.getId())).thenReturn(Optional.of(docente));
        when(cargoRepository.findById(cargoAdjunto.getId())).thenReturn(Optional.of(cargoAdjunto));
        when(tipoDesignacionRepository.findById(tipoOrdinario.getId())).thenReturn(Optional.of(tipoOrdinario));
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of(existente40));

        // 40 + 20 = 60 > 50 -> rechaza RN02
        when(dedicacionRepository.findById(dedSemi20.getId())).thenReturn(Optional.of(dedSemi20));
        DomainValidationException ex = assertThrows(DomainValidationException.class, () ->
                service.agregarDesignacion(sol.getId(), new GuardarDesignacionDocenteRequest(
                        docente.getId(), cargoAdjunto.getId(), dedSemi20.getId(), tipoOrdinario.getId(), "MAP-2", null)));
        assertTrue(ex.getMessage().contains("50"));

        // 40 + 10 = 50 -> permitido RN02
        when(dedicacionRepository.findById(dedSimple10.getId())).thenReturn(Optional.of(dedSimple10));
        when(designacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(any())).thenReturn(List.of());

        DesignacionDocenteItemDto item = service.agregarDesignacion(sol.getId(), new GuardarDesignacionDocenteRequest(
                docente.getId(), cargoAdjunto.getId(), dedSimple10.getId(), tipoOrdinario.getId(), "MAP-3", "Segundo cargo"));

        assertEquals(10, item.horasDedicacion());
        assertEquals(50, item.totalHorasDocentePeriodo());
        assertTrue(item.tieneAlertaHorasSinDistribuir());
        assertEquals(10, item.horasSinDistribuir());
    }

    @Test
    void actualizarDesignacion_restaHorasPreviasDelMismoRenglonParaTope50() {
        SolicitudDesignacionJpaEntity sol = solicitudBorrador(deptoCyT);
        SolicitudDocenteDesignacionJpaEntity itemSimple10 = designacion(sol, docente, dedSimple10);
        SolicitudDocenteDesignacionJpaEntity otroSimple10 = designacion(sol, docente, dedSimple10);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(designacionRepository.findById(itemSimple10.getId())).thenReturn(Optional.of(itemSimple10));
        when(cargoRepository.findById(cargoAdjunto.getId())).thenReturn(Optional.of(cargoAdjunto));
        when(dedicacionRepository.findById(dedExclusiva40.getId())).thenReturn(Optional.of(dedExclusiva40));
        when(tipoDesignacionRepository.findById(tipoOrdinario.getId())).thenReturn(Optional.of(tipoOrdinario));
        // Tiene dos cargos de 10h; si el primero sube a 40h -> 10 (otro) + 40 (editado) = 50h -> OK
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of(itemSimple10, otroSimple10));
        when(designacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(itemSimple10.getId())).thenReturn(List.of());

        DesignacionDocenteItemDto updated = service.actualizarDesignacion(
                sol.getId(),
                itemSimple10.getId(),
                new GuardarDesignacionDocenteRequest(docente.getId(), cargoAdjunto.getId(), dedExclusiva40.getId(), tipoOrdinario.getId(), "MAP-1", null)
        );

        assertEquals(40, updated.horasDedicacion());
        assertEquals(50, updated.totalHorasDocentePeriodo());
    }

    @Test
    void actualizarDistribucionHoraria_validaTopeNegativosYAlertaHorasSinDistribuir() {
        SolicitudDesignacionJpaEntity sol = solicitudBorrador(deptoCyT);
        SolicitudDocenteDesignacionJpaEntity item20 = designacion(sol, docente, dedSemi20);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(designacionRepository.findById(item20.getId())).thenReturn(Optional.of(item20));

        // Negativo -> 422
        assertThrows(DomainValidationException.class, () -> service.actualizarDistribucionHoraria(
                sol.getId(), item20.getId(), new GuardarDistribucionHorariaRequest(-1, 0, 0, 0, 0, 0)));

        // Suma 21 > 20 -> 422 (RN06)
        assertThrows(DomainValidationException.class, () -> service.actualizarDistribucionHoraria(
                sol.getId(), item20.getId(), new GuardarDistribucionHorariaRequest(10, 5, 2, 2, 1, 1)));

        // Suma 14 < 20 -> OK con alerta RF04.5 (6 hs sin distribuir)
        when(designacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(designacionRepository.findActivasByDocenteAndPeriodo(docente.getId(), periodo2026.getId()))
                .thenReturn(List.of(item20));
        when(asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(item20.getId())).thenReturn(List.of());

        DesignacionDocenteItemDto res = service.actualizarDistribucionHoraria(
                sol.getId(), item20.getId(), new GuardarDistribucionHorariaRequest(8, 4, 2, 0, 0, 0));

        assertEquals(14, res.totalHorasDistribuidas());
        assertEquals(6, res.horasSinDistribuir());
        assertTrue(res.tieneAlertaHorasSinDistribuir());
    }

    @Test
    void agregarAsignacionCurricular_respetaRN09YEvitaDuplicados() {
        SolicitudDesignacionJpaEntity sol = solicitudBorrador(deptoCyT);
        SolicitudDocenteDesignacionJpaEntity item = designacion(sol, docente, dedSemi20);

        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setId(UUID.randomUUID());
        carrera.setNombre("Ingeniería en Sistemas");
        carrera.setSigla("IS");

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setCarrera(carrera);
        plan.setNombrePlan("Plan 2015");
        plan.setEstadoVigencia(EstadoVigenciaPlan.NO_VIGENTE);

        ActividadCurricularPlanJpaEntity actInactiva = actividad(plan, "MAT-0", EstadoDictadoActividad.INACTIVA);
        ActividadCurricularPlanJpaEntity actSoloFinales = actividad(plan, "MAT-1", EstadoDictadoActividad.SOLO_EXAMENES_FINALES);
        ActividadCurricularPlanJpaEntity actActiva = actividad(plan, "MAT-2", EstadoDictadoActividad.ACTIVA);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(solicitudRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(designacionRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(carreraRepository.findById(carrera.getId())).thenReturn(Optional.of(carrera));
        when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

        // INACTIVA -> 422 siempre
        when(actividadRepository.findById(actInactiva.getId())).thenReturn(Optional.of(actInactiva));
        assertThrows(DomainValidationException.class, () -> service.agregarAsignacionCurricular(
                sol.getId(), item.getId(), new AgregarAsignacionCurricularRequest(
                        carrera.getId(), plan.getId(), actInactiva.getId(), ModalidadParticipacionDocente.EVALUACION_FINALES, 2, null)));

        // SOLO_EXAMENES_FINALES + DESARROLLO_CLASES -> 422
        when(actividadRepository.findById(actSoloFinales.getId())).thenReturn(Optional.of(actSoloFinales));
        assertThrows(DomainValidationException.class, () -> service.agregarAsignacionCurricular(
                sol.getId(), item.getId(), new AgregarAsignacionCurricularRequest(
                        carrera.getId(), plan.getId(), actSoloFinales.getId(), ModalidadParticipacionDocente.DESARROLLO_CLASES, 4, null)));

        // SOLO_EXAMENES_FINALES + EVALUACION_FINALES -> OK
        when(asignacionRepository.existsByDesignacionIdAndActividadCurricularIdAndModalidadParticipacion(
                item.getId(), actSoloFinales.getId(), ModalidadParticipacionDocente.EVALUACION_FINALES))
                .thenReturn(false);
        when(asignacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AsignacionCurricularPeriodoDto okFinales = service.agregarAsignacionCurricular(
                sol.getId(), item.getId(), new AgregarAsignacionCurricularRequest(
                        carrera.getId(), plan.getId(), actSoloFinales.getId(), ModalidadParticipacionDocente.EVALUACION_FINALES, 2, "Mesa examen"));
        assertEquals(ModalidadParticipacionDocente.EVALUACION_FINALES, okFinales.modalidadParticipacion());
        assertEquals(EstadoVigenciaPlan.NO_VIGENTE, okFinales.planEstadoVigencia());

        // ACTIVA + DESARROLLO_CLASES duplicada -> 422
        when(actividadRepository.findById(actActiva.getId())).thenReturn(Optional.of(actActiva));
        when(asignacionRepository.existsByDesignacionIdAndActividadCurricularIdAndModalidadParticipacion(
                item.getId(), actActiva.getId(), ModalidadParticipacionDocente.DESARROLLO_CLASES))
                .thenReturn(true);
        assertThrows(DomainValidationException.class, () -> service.agregarAsignacionCurricular(
                sol.getId(), item.getId(), new AgregarAsignacionCurricularRequest(
                        carrera.getId(), plan.getId(), actActiva.getId(), ModalidadParticipacionDocente.DESARROLLO_CLASES, 6, null)));
    }

    @Test
    void directorDepartamento_noPuedeAccederNiEditarSolicitudDeOtroDepartamentoNiSolicitudPresentada() {
        User directorCyT = User.rehydrate(
                UUID.randomUUID(), "dir.cyt", "dir.cyt@undec.edu.ar", "hash", true, false,
                null, deptoCyT.getId(),
                Set.of(Role.system(UUID.randomUUID(), CodigosRol.DIRECTOR_DEPARTAMENTO, "Director Dpto", "Director de Departamento"))
        );
        SolicitudDesignacionJpaEntity solSociales = solicitudBorrador(deptoSociales);
        SolicitudDesignacionJpaEntity solCyTPresentada = solicitudBorrador(deptoCyT);
        solCyTPresentada.setEstado(EstadoSolicitudDesignacion.PRESENTADA_SGA);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(directorCyT));
        when(solicitudRepository.findById(solSociales.getId())).thenReturn(Optional.of(solSociales));
        when(solicitudRepository.findById(solCyTPresentada.getId())).thenReturn(Optional.of(solCyTPresentada));

        // Lectura de otro departamento -> bloqueada (RNF01)
        assertThrows(DomainValidationException.class, () -> service.obtenerDetalleSolicitud(solSociales.getId()));

        // Edición en solicitud PRESENTADA_SGA -> bloqueada
        assertThrows(DomainValidationException.class, () -> service.agregarDesignacion(
                solCyTPresentada.getId(),
                new GuardarDesignacionDocenteRequest(docente.getId(), cargoAdjunto.getId(), dedSimple10.getId(), tipoOrdinario.getId(), null, null)));

        // Director de CyT no puede designar un docente de Sociales en su propia solicitud (RNF01)
        SolicitudDesignacionJpaEntity solCyTBorrador = solicitudBorrador(deptoCyT);
        CuerpoAcademicoMaestroJpaEntity docenteSociales = new CuerpoAcademicoMaestroJpaEntity();
        docenteSociales.setId(UUID.randomUUID());
        docenteSociales.setDni("30999888");
        docenteSociales.setApellido("Pérez");
        docenteSociales.setNombre("Luis");
        docenteSociales.setDepartamentoAcademico(deptoSociales);

        when(solicitudRepository.findById(solCyTBorrador.getId())).thenReturn(Optional.of(solCyTBorrador));
        when(docenteRepository.findById(docenteSociales.getId())).thenReturn(Optional.of(docenteSociales));

        DomainValidationException exOtroDepto = assertThrows(DomainValidationException.class, () -> service.agregarDesignacion(
                solCyTBorrador.getId(),
                new GuardarDesignacionDocenteRequest(docenteSociales.getId(), cargoAdjunto.getId(), dedSimple10.getId(), tipoOrdinario.getId(), null, null)));
        assertTrue(exOtroDepto.getMessage().contains("Departamento Académico"));
    }

    private SolicitudDesignacionJpaEntity solicitudBorrador(DepartamentoAcademicoJpaEntity depto) {
        SolicitudDesignacionJpaEntity s = new SolicitudDesignacionJpaEntity();
        s.setId(UUID.randomUUID());
        s.setDepartamentoAcademico(depto);
        s.setPeriodoAcademico(periodo2026);
        s.setTipoSolicitud(TipoSolicitudDesignacion.ANUAL);
        s.setEstado(EstadoSolicitudDesignacion.BORRADOR);
        return s;
    }

    private SolicitudDocenteDesignacionJpaEntity designacion(
            SolicitudDesignacionJpaEntity sol,
            CuerpoAcademicoMaestroJpaEntity doc,
            DedicacionDocenteJpaEntity ded) {
        SolicitudDocenteDesignacionJpaEntity d = new SolicitudDocenteDesignacionJpaEntity();
        d.setId(UUID.randomUUID());
        d.setSolicitud(sol);
        d.setDocente(doc);
        d.setCargoDocente(cargoAdjunto);
        d.setDedicacionDocente(ded);
        d.setTipoDesignacion(tipoOrdinario);
        d.setHorasDedicacion(ded.getHorasSemanales());
        d.setEstadoItem(EstadoItemDesignacion.NUEVO);
        return d;
    }

    private ActividadCurricularPlanJpaEntity actividad(
            PlanEstudioFt001JpaEntity plan,
            String codigo,
            EstadoDictadoActividad estado) {
        ActividadCurricularPlanJpaEntity a = new ActividadCurricularPlanJpaEntity();
        a.setId(UUID.randomUUID());
        a.setPlanEstudio(plan);
        a.setCodigo(codigo);
        a.setNombre("Materia " + codigo);
        a.setEstadoDictado(estado);
        a.setHorasSemanales(4);
        return a;
    }
}
