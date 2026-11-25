package com.undec.acreditacion.infrastructure.estandares.service;

import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ProgramaAnaliticoAsignaturaJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataProgramaAnaliticoAsignaturaRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.SincronizacionBidireccionalPlanService;
import com.undec.acreditacion.infrastructure.estandares.dto.CumplimientoCargaHorariaPlanDto;
import com.undec.acreditacion.infrastructure.estandares.dto.EstandarDetalleDto;
import com.undec.acreditacion.infrastructure.estandares.dto.FichaEstandarActividadDto;
import com.undec.acreditacion.infrastructure.estandares.dto.GuardarFichaEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUserRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UserJpaEntity;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActividadEstandarFichaServiceTest {

    @Mock private SpringDataPlanEstudioFt001Repository planRepository;
    @Mock private SpringDataActividadCurricularPlanRepository asignaturaRepository;
    @Mock private SpringDataEstandarEscalaEvaluacionRepository escalaRepository;
    @Mock private SpringDataEstandarGrupo22Repository grupoRepository;
    @Mock private SpringDataEstandarItem22Repository itemRepository;
    @Mock private SpringDataEstandarAreaCargaHorariaRepository areaRepository;
    @Mock private SpringDataActividadCurricularEvaluacion22Repository evaluacion22Repository;
    @Mock private SpringDataActividadCurricularCargaHorariaAreaRepository cargaAreaRepository;
    @Mock private SpringDataActividadCurricularCargaHorariaResumenRepository cargaResumenRepository;
    @Mock private EstandarService estandarService;
    @Mock private SpringDataUserRepository userRepository;
    @Mock private SpringDataProgramaAnaliticoAsignaturaRepository programaRepository;
    @Mock private SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;
    @Mock private AuthenticatedUserProvider authenticatedUserProvider;

    private ActividadEstandarFichaService service;

    @BeforeEach
    void setUp() {
        service = new ActividadEstandarFichaService(
                planRepository,
                asignaturaRepository,
                escalaRepository,
                grupoRepository,
                itemRepository,
                areaRepository,
                evaluacion22Repository,
                cargaAreaRepository,
                cargaResumenRepository,
                estandarService,
                userRepository,
                programaRepository,
                sincronizacionBidireccionalPlanService,
                authenticatedUserProvider
        );
    }

    @Test
    @DisplayName("Guarda y evalúa completitud COMPETENCIAS (MATRIZ_MULTIPLE) ignorando grupos/áreas deshabilitados")
    void guardaYCompletaMatrizCompetenciasConGruposYAreasParciales() {
        Fixture f = crearFixture(TipoSeccionEstandar22.COMPETENCIAS, TipoEvaluacionEstandar22.MATRIZ_MULTIPLE, 80);
        UUID docenteId = UUID.randomUUID();
        f.asig.setDocenteResponsableId(docenteId);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(docenteId, "DOCENTE")));
        when(cargaResumenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(estandarService.toDetalleDto(f.estandar)).thenReturn(f.detalleDto);

        GuardarFichaEstandarRequest req = new GuardarFichaEstandarRequest(
                List.of(new FichaEstandarActividadDto.Respuesta22Dto(f.itemHabilitado.getId(), f.escala1.getId())),
                List.of(
                        new FichaEstandarActividadDto.CargaAreaDto(f.areaHabilitada.getId(), 80, 0, 0, 0),
                        new FichaEstandarActividadDto.CargaAreaDto(f.areaPracticaHabilitada.getId(), 0, 0, 20, 0)
                ),
                new FichaEstandarActividadDto.ResumenCargaDto("Laboratorio de informática", 5, 0, 2, 0)
        );

        FichaEstandarActividadDto out = service.guardarFicha(f.plan.getId(), f.asig.getId(), req);

        verify(sincronizacionBidireccionalPlanService).propagarFichaEstandarAsignatura(f.plan, f.asig);
        assertEquals(f.asig.getId(), out.actividadCurricularId());
        assertTrue(out.puedeEditar());

        // Reject disabled item / disabled area
        GuardarFichaEstandarRequest reqItemDeshabilitado = new GuardarFichaEstandarRequest(
                List.of(new FichaEstandarActividadDto.Respuesta22Dto(f.itemDeshabilitado.getId(), f.escala1.getId())),
                req.cargasPorArea(),
                req.resumenCarga()
        );
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(f.plan.getId(), f.asig.getId(), reqItemDeshabilitado));

        GuardarFichaEstandarRequest reqAreaDeshabilitada = new GuardarFichaEstandarRequest(
                req.respuestas22(),
                List.of(new FichaEstandarActividadDto.CargaAreaDto(f.areaDeshabilitada.getId(), 80, 0, 0, 0)),
                req.resumenCarga()
        );
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(f.plan.getId(), f.asig.getId(), reqAreaDeshabilitada));
    }

    @Test
    @DisplayName("Validaciones 422 para ESCALA_UNICA, MATRIZ vacía y reglas de carga horaria Sección 3")
    void validaReglasSeccion22YSeccion3() {
        // ESCALA_UNICA missing enabled item
        Fixture fEjes = crearFixture(TipoSeccionEstandar22.EJES, TipoEvaluacionEstandar22.ESCALA_UNICA, 75);
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(UUID.randomUUID(), "AEA")));

        GuardarFichaEstandarRequest sinRespuestas = new GuardarFichaEstandarRequest(
                List.of(),
                List.of(new FichaEstandarActividadDto.CargaAreaDto(fEjes.areaHabilitada.getId(), 75, 0, 0, 0)),
                new FichaEstandarActividadDto.ResumenCargaDto(null, 5, 0, 0, 0)
        );
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(fEjes.plan.getId(), fEjes.asig.getId(), sinRespuestas));

        // MATRIZ_MULTIPLE empty
        Fixture fComp = crearFixture(TipoSeccionEstandar22.COMPETENCIAS, TipoEvaluacionEstandar22.MATRIZ_MULTIPLE, 75);
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(fComp.plan.getId(), fComp.asig.getId(), sinRespuestas));

        List<FichaEstandarActividadDto.Respuesta22Dto> evalOk = List.of(
                new FichaEstandarActividadDto.Respuesta22Dto(fComp.itemHabilitado.getId(), fComp.escala1.getId()));

        // Sum 3.1.1 != horasReloj (60 != 75)
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(fComp.plan.getId(), fComp.asig.getId(), new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(new FichaEstandarActividadDto.CargaAreaDto(fComp.areaHabilitada.getId(), 60, 0, 0, 0)),
                        new FichaEstandarActividadDto.ResumenCargaDto(null, 4, 0, 0, 0)
                )));

        // horasPractica > horasTotal
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(fComp.plan.getId(), fComp.asig.getId(), new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(
                                new FichaEstandarActividadDto.CargaAreaDto(fComp.areaHabilitada.getId(), 75, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(fComp.areaPracticaHabilitada.getId(), 0, 0, 80, 0)
                        ),
                        new FichaEstandarActividadDto.ResumenCargaDto("Taller", 5, 0, 2, 0)
                )));

        // Missing ambitos 3.1.3 when 3.1.2 > 0
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(fComp.plan.getId(), fComp.asig.getId(), new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(
                                new FichaEstandarActividadDto.CargaAreaDto(fComp.areaHabilitada.getId(), 75, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(fComp.areaPracticaHabilitada.getId(), 0, 0, 10, 0)
                        ),
                        new FichaEstandarActividadDto.ResumenCargaDto("   ", 5, 0, 1, 0)
                )));

        // Semanal total <= 0 or semanal practica > semanal total
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(fComp.plan.getId(), fComp.asig.getId(), new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(new FichaEstandarActividadDto.CargaAreaDto(fComp.areaHabilitada.getId(), 75, 0, 0, 0)),
                        new FichaEstandarActividadDto.ResumenCargaDto(null, 0, 0, 0, 0)
                )));
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(fComp.plan.getId(), fComp.asig.getId(), new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(new FichaEstandarActividadDto.CargaAreaDto(fComp.areaHabilitada.getId(), 75, 0, 0, 0)),
                        new FichaEstandarActividadDto.ResumenCargaDto(null, 4, 0, 5, 0)
                )));
    }

    @Test
    @DisplayName("Permisos docente y designarDocenteResponsable con sincronización a Programa Analítico y clones")
    void permisosDocenteYDesignacionSincronizada() {
        Fixture f = crearFixture(TipoSeccionEstandar22.COMPETENCIAS, TipoEvaluacionEstandar22.MATRIZ_MULTIPLE, 60);
        UUID docenteAsignadoId = UUID.randomUUID();
        UUID otroDocenteId = UUID.randomUUID();
        f.asig.setDocenteResponsableId(docenteAsignadoId);

        when(estandarService.toDetalleDto(f.estandar)).thenReturn(f.detalleDto);
        when(cargaResumenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Another teacher cannot save -> 403
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(otroDocenteId, "DOCENTE")));
        GuardarFichaEstandarRequest reqOk = new GuardarFichaEstandarRequest(
                List.of(new FichaEstandarActividadDto.Respuesta22Dto(f.itemHabilitado.getId(), f.escala1.getId())),
                List.of(new FichaEstandarActividadDto.CargaAreaDto(f.areaHabilitada.getId(), 60, 0, 0, 0)),
                new FichaEstandarActividadDto.ResumenCargaDto(null, 4, 0, 0, 0)
        );
        ResponseStatusException ex403 = assertThrows(ResponseStatusException.class,
                () -> service.guardarFicha(f.plan.getId(), f.asig.getId(), reqOk));
        assertEquals(HttpStatus.FORBIDDEN, ex403.getStatusCode());

        // Assigned teacher succeeds
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(docenteAsignadoId, "DOCENTE")));
        assertDoesNotThrow(() -> service.guardarFicha(f.plan.getId(), f.asig.getId(), reqOk));

        // Gestor designates teacher and syncs ProgramaAnalitico + bidirectional clones
        UserJpaEntity nuevoDoc = new UserJpaEntity(otroDocenteId, "ing.perez", "perez@undec.edu.ar", "h", true, false, Instant.now());
        ProgramaAnaliticoAsignaturaJpaEntity prog = new ProgramaAnaliticoAsignaturaJpaEntity();
        prog.setId(UUID.randomUUID());

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(UUID.randomUUID(), "DIRECTOR_CARRERA")));
        when(userRepository.findById(otroDocenteId)).thenReturn(Optional.of(nuevoDoc));
        when(asignaturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(programaRepository.findByActividadCurricularId(f.asig.getId())).thenReturn(Optional.of(prog));

        ActividadCurricularPlanJpaEntity updated = service.designarDocenteResponsable(
                f.plan.getId(), f.asig.getId(), otroDocenteId, null);

        assertEquals(otroDocenteId, updated.getDocenteResponsableId());
        assertEquals("ing.perez", updated.getDocenteResponsableNombre());
        assertEquals(otroDocenteId, prog.getDocenteDesignadoId());
        assertEquals("ing.perez", prog.getDocenteDesignadoNombre());
        verify(programaRepository).save(prog);
        verify(sincronizacionBidireccionalPlanService).propagarActualizacionAsignatura(f.plan, updated);
    }

    @Test
    @DisplayName("calcularEstadosCompletitud determina 2.2 y 3 en lote sin N+1")
    void calcularEstadosCompletitudEnLote() {
        Fixture f = crearFixture(TipoSeccionEstandar22.EJES, TipoEvaluacionEstandar22.ESCALA_UNICA, 50);

        ActividadCurricularEvaluacion22JpaEntity ev = new ActividadCurricularEvaluacion22JpaEntity();
        ev.setId(UUID.randomUUID());
        ev.setActividadCurricular(f.asig);
        ev.setEstandarItem(f.itemHabilitado);
        ev.setEscalaEvaluacion(f.escala1);

        ActividadCurricularCargaHorariaAreaJpaEntity carga311 = new ActividadCurricularCargaHorariaAreaJpaEntity();
        carga311.setId(UUID.randomUUID());
        carga311.setActividadCurricular(f.asig);
        carga311.setAreaCargaHoraria(f.areaHabilitada);
        carga311.setHorasTotalPresencial(50);
        carga311.setHorasTotalDistancia(0);
        carga311.setHorasPracticaPresencial(0);
        carga311.setHorasPracticaDistancia(0);

        ActividadCurricularCargaHorariaAreaJpaEntity carga312 = new ActividadCurricularCargaHorariaAreaJpaEntity();
        carga312.setId(UUID.randomUUID());
        carga312.setActividadCurricular(f.asig);
        carga312.setAreaCargaHoraria(f.areaPracticaHabilitada);
        carga312.setHorasTotalPresencial(0);
        carga312.setHorasTotalDistancia(0);
        carga312.setHorasPracticaPresencial(10);
        carga312.setHorasPracticaDistancia(0);

        ActividadCurricularCargaHorariaResumenJpaEntity resumen = new ActividadCurricularCargaHorariaResumenJpaEntity();
        resumen.setActividadCurricularId(f.asig.getId());
        resumen.setAmbitosFormacionPractica("Aula taller");
        resumen.setHorasSemanalTotalPresencial(4);
        resumen.setHorasSemanalTotalDistancia(0);
        resumen.setHorasSemanalPracticaPresencial(1);
        resumen.setHorasSemanalPracticaDistancia(0);

        when(evaluacion22Repository.findByActividadCurricularIdIn(List.of(f.asig.getId()))).thenReturn(List.of(ev));
        when(cargaAreaRepository.findByActividadCurricularIdIn(List.of(f.asig.getId()))).thenReturn(List.of(carga311, carga312));
        when(cargaResumenRepository.findByActividadCurricularIdIn(List.of(f.asig.getId()))).thenReturn(List.of(resumen));

        Map<UUID, ActividadEstandarFichaService.CompletitudEstado> map =
                service.calcularEstadosCompletitud(f.plan, List.of(f.asig));

        assertTrue(map.get(f.asig.getId()).seccion22Completa());
        assertTrue(map.get(f.asig.getId()).seccion3Completa());
    }

    @Test
    @DisplayName("Mecatrónica (EJES): bloques 3.1.1 (90h) y modalidades 3.1.2 independientes (55h ok, 95h -> 422)")
    void mecatronicaEjesBloques311YModalidades312() {
        Fixture f = crearFixture(TipoSeccionEstandar22.EJES, TipoEvaluacionEstandar22.ESCALA_UNICA, 90);
        f.areaHabilitada.setCodigo("CIENCIAS_BASICAS_INGENIERIA");
        f.areaHabilitada.setNombre("Ciencias Básicas de la Ingeniería");
        f.areaPracticaHabilitada.setCodigo("INSTANCIAS_SUPERVISADAS_PRACTICA");
        f.areaPracticaHabilitada.setNombre("Instancias supervisadas de formación práctica");

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(UUID.randomUUID(), "AEA")));
        when(cargaResumenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(estandarService.toDetalleDto(f.estandar)).thenReturn(f.detalleDto);

        List<FichaEstandarActividadDto.Respuesta22Dto> evalOk = List.of(
                new FichaEstandarActividadDto.Respuesta22Dto(f.itemHabilitado.getId(), f.escala1.getId()));

        FichaEstandarActividadDto saved = service.guardarFicha(f.plan.getId(), f.asig.getId(),
                new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaHabilitada.getId(), 90, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaPracticaHabilitada.getId(), 0, 0, 55, 0)
                        ),
                        new FichaEstandarActividadDto.ResumenCargaDto("Aulas", 6, 0, 4, 0)
                ));
        assertNotNull(saved);

        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(f.plan.getId(), f.asig.getId(), new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaHabilitada.getId(), 90, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaPracticaHabilitada.getId(), 0, 0, 95, 0)
                        ),
                        new FichaEstandarActividadDto.ResumenCargaDto("Aulas", 6, 0, 4, 0)
                )));
    }

    @Test
    @DisplayName("Agronomía (COMPETENCIAS): áreas 3.1.1 y 3.1.2 con mismo código validan límite por área")
    void agronomiaCompetenciasValidaLimitePorCodigoArea() {
        Fixture f = crearFixture(TipoSeccionEstandar22.COMPETENCIAS, TipoEvaluacionEstandar22.MATRIZ_MULTIPLE, 75);
        EstandarAreaCargaHorariaJpaEntity areaApp311 = new EstandarAreaCargaHorariaJpaEntity();
        areaApp311.setId(UUID.randomUUID());
        areaApp311.setEstandar(f.estandar);
        areaApp311.setTipoSeccion(TipoSeccionCargaHoraria.CARGA_TOTAL_311);
        areaApp311.setOrden(2);
        areaApp311.setCodigo("FORMACION_APLICADA");
        areaApp311.setNombre("Formación Aplicada");
        areaApp311.setHabilitada(true);

        EstandarAreaCargaHorariaJpaEntity areaApp312 = new EstandarAreaCargaHorariaJpaEntity();
        areaApp312.setId(UUID.randomUUID());
        areaApp312.setEstandar(f.estandar);
        areaApp312.setTipoSeccion(TipoSeccionCargaHoraria.PRACTICA_312);
        areaApp312.setOrden(2);
        areaApp312.setCodigo("FORMACION_APLICADA");
        areaApp312.setNombre("Formación Aplicada");
        areaApp312.setHabilitada(true);

        when(areaRepository.findByEstandarIdOrderByOrdenAsc(f.estandar.getId()))
                .thenReturn(List.of(f.areaHabilitada, areaApp311, f.areaPracticaHabilitada, areaApp312));
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(UUID.randomUUID(), "AEA")));
        when(cargaResumenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(estandarService.toDetalleDto(f.estandar)).thenReturn(f.detalleDto);

        List<FichaEstandarActividadDto.Respuesta22Dto> evalOk = List.of(
                new FichaEstandarActividadDto.Respuesta22Dto(f.itemHabilitado.getId(), f.escala1.getId()));

        // Valid: 20 + 55 = 75 total; practice 10 <= 20 (BASICA) and 10 <= 55 (APLICADA)
        assertDoesNotThrow(() -> service.guardarFicha(f.plan.getId(), f.asig.getId(),
                new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaHabilitada.getId(), 20, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(areaApp311.getId(), 55, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaPracticaHabilitada.getId(), 0, 0, 10, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(areaApp312.getId(), 0, 0, 10, 0)
                        ),
                        new FichaEstandarActividadDto.ResumenCargaDto("Campo", 5, 0, 2, 0)
                )));

        // Invalid: BASICA practice 25 > 20 BASICA total (even though 25 <= 75 overall)
        assertThrows(DomainValidationException.class,
                () -> service.guardarFicha(f.plan.getId(), f.asig.getId(), new GuardarFichaEstandarRequest(
                        evalOk,
                        List.of(
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaHabilitada.getId(), 20, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(areaApp311.getId(), 55, 0, 0, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(f.areaPracticaHabilitada.getId(), 0, 0, 25, 0),
                                new FichaEstandarActividadDto.CargaAreaDto(areaApp312.getId(), 0, 0, 0, 0)
                        ),
                        new FichaEstandarActividadDto.ResumenCargaDto("Campo", 5, 0, 2, 0)
                )));
    }

    private static class Fixture {
        CarreraJpaEntity carrera;
        EstandarJpaEntity estandar;
        EstandarEscalaEvaluacionJpaEntity escala1;
        EstandarGrupo22JpaEntity grupoHabilitado;
        EstandarGrupo22JpaEntity grupoDeshabilitado;
        EstandarItem22JpaEntity itemHabilitado;
        EstandarItem22JpaEntity itemDeshabilitado;
        EstandarAreaCargaHorariaJpaEntity areaHabilitada;
        EstandarAreaCargaHorariaJpaEntity areaPracticaHabilitada;
        EstandarAreaCargaHorariaJpaEntity areaDeshabilitada;
        PlanEstudioFt001JpaEntity plan;
        ActividadCurricularPlanJpaEntity asig;
        EstandarDetalleDto detalleDto;
    }

    private Fixture crearFixture(TipoSeccionEstandar22 tipo22, TipoEvaluacionEstandar22 eval22, int horasReloj) {
        Fixture f = new Fixture();
        f.carrera = new CarreraJpaEntity();
        f.carrera.setId(UUID.randomUUID());
        f.carrera.setNombre("Sistemas");
        f.carrera.setSigla("ISI");

        f.estandar = new EstandarJpaEntity();
        f.estandar.setId(UUID.randomUUID());
        f.estandar.setCarrera(f.carrera);
        f.estandar.setNombre("Estándar ISI");
        f.estandar.setEstadoVigencia(EstadoVigenciaEstandar.VIGENTE);
        f.estandar.setTipoSeccion22(tipo22);
        f.estandar.setTipoEvaluacion22(eval22);

        f.escala1 = new EstandarEscalaEvaluacionJpaEntity();
        f.escala1.setId(UUID.randomUUID());
        f.escala1.setEstandar(f.estandar);
        f.escala1.setOrden(1);
        f.escala1.setCodigo("APRENDE");
        f.escala1.setEtiqueta("Aprende");

        f.grupoHabilitado = new EstandarGrupo22JpaEntity();
        f.grupoHabilitado.setId(UUID.randomUUID());
        f.grupoHabilitado.setEstandar(f.estandar);
        f.grupoHabilitado.setOrden(1);
        f.grupoHabilitado.setCodigo("BASICA");
        f.grupoHabilitado.setTitulo("Formación Básica");
        f.grupoHabilitado.setHabilitado(true);

        f.grupoDeshabilitado = new EstandarGrupo22JpaEntity();
        f.grupoDeshabilitado.setId(UUID.randomUUID());
        f.grupoDeshabilitado.setEstandar(f.estandar);
        f.grupoDeshabilitado.setOrden(2);
        f.grupoDeshabilitado.setCodigo("APLICADA");
        f.grupoDeshabilitado.setTitulo("Formación Aplicada");
        f.grupoDeshabilitado.setHabilitado(false);

        f.itemHabilitado = new EstandarItem22JpaEntity();
        f.itemHabilitado.setId(UUID.randomUUID());
        f.itemHabilitado.setGrupo(f.grupoHabilitado);
        f.itemHabilitado.setOrden(1);
        f.itemHabilitado.setEnunciado("Habilidad 1");

        f.itemDeshabilitado = new EstandarItem22JpaEntity();
        f.itemDeshabilitado.setId(UUID.randomUUID());
        f.itemDeshabilitado.setGrupo(f.grupoDeshabilitado);
        f.itemDeshabilitado.setOrden(1);
        f.itemDeshabilitado.setEnunciado("Habilidad deshabilitada");

        f.areaHabilitada = new EstandarAreaCargaHorariaJpaEntity();
        f.areaHabilitada.setId(UUID.randomUUID());
        f.areaHabilitada.setEstandar(f.estandar);
        f.areaHabilitada.setTipoSeccion(TipoSeccionCargaHoraria.CARGA_TOTAL_311);
        f.areaHabilitada.setOrden(1);
        f.areaHabilitada.setCodigo("FORMACION_BASICA");
        f.areaHabilitada.setNombre("Formación Básica");
        f.areaHabilitada.setEtiquetaPractica("Formación Básica");
        f.areaHabilitada.setHabilitada(true);

        f.areaPracticaHabilitada = new EstandarAreaCargaHorariaJpaEntity();
        f.areaPracticaHabilitada.setId(UUID.randomUUID());
        f.areaPracticaHabilitada.setEstandar(f.estandar);
        f.areaPracticaHabilitada.setTipoSeccion(TipoSeccionCargaHoraria.PRACTICA_312);
        f.areaPracticaHabilitada.setOrden(1);
        f.areaPracticaHabilitada.setCodigo("FORMACION_BASICA");
        f.areaPracticaHabilitada.setNombre("Formación Básica");
        f.areaPracticaHabilitada.setEtiquetaPractica("Formación Básica");
        f.areaPracticaHabilitada.setHabilitada(true);

        f.areaDeshabilitada = new EstandarAreaCargaHorariaJpaEntity();
        f.areaDeshabilitada.setId(UUID.randomUUID());
        f.areaDeshabilitada.setEstandar(f.estandar);
        f.areaDeshabilitada.setTipoSeccion(TipoSeccionCargaHoraria.CARGA_TOTAL_311);
        f.areaDeshabilitada.setOrden(2);
        f.areaDeshabilitada.setCodigo("FORMACION_APLICADA");
        f.areaDeshabilitada.setNombre("Formación Aplicada");
        f.areaDeshabilitada.setEtiquetaPractica("Formación Aplicada");
        f.areaDeshabilitada.setHabilitada(false);

        f.plan = new PlanEstudioFt001JpaEntity();
        f.plan.setId(UUID.randomUUID());
        f.plan.setCarrera(f.carrera);
        f.plan.setEstandar(f.estandar);
        f.plan.setNombrePlan("Plan 2026");

        f.asig = new ActividadCurricularPlanJpaEntity();
        f.asig.setId(UUID.randomUUID());
        f.asig.setPlanEstudio(f.plan);
        f.asig.setCodigo("MAT101");
        f.asig.setNombre("Análisis Matemático");
        f.asig.setHorasReloj(horasReloj);

        f.detalleDto = new EstandarDetalleDto(
                f.estandar.getId(),
                f.carrera.getId(),
                f.carrera.getNombre(),
                f.carrera.getSigla(),
                f.estandar.getNombre(),
                "RM 01/26",
                2026,
                EstadoVigenciaEstandar.VIGENTE,
                tipo22,
                eval22,
                null,
                null,
                0L,
                true,
                List.of(new EstandarDetalleDto.EscalaDto(f.escala1.getId(), 1, "APRENDE", "Aprende", "Desc", false)),
                List.of(new EstandarDetalleDto.Grupo22Dto(
                        f.grupoHabilitado.getId(), 1, "BASICA", "Formación Básica", null, true,
                        List.of(new EstandarDetalleDto.Item22Dto(f.itemHabilitado.getId(), 1, "Habilidad 1")))),
                List.of(
                        new EstandarDetalleDto.AreaCargaHorariaDto(
                                f.areaHabilitada.getId(), TipoSeccionCargaHoraria.CARGA_TOTAL_311,
                                1, "FORMACION_BASICA", "Formación Básica", "Formación Básica", true),
                        new EstandarDetalleDto.AreaCargaHorariaDto(
                                f.areaPracticaHabilitada.getId(), TipoSeccionCargaHoraria.PRACTICA_312,
                                1, "FORMACION_BASICA", "Formación Básica", "Formación Básica", true)
                ),
                Instant.now(),
                Instant.now()
        );

        lenient().when(planRepository.findById(f.plan.getId())).thenReturn(Optional.of(f.plan));
        lenient().when(asignaturaRepository.findById(f.asig.getId())).thenReturn(Optional.of(f.asig));
        lenient().when(escalaRepository.findByEstandarIdOrderByOrdenAsc(f.estandar.getId())).thenReturn(List.of(f.escala1));
        lenient().when(grupoRepository.findByEstandarIdOrderByOrdenAsc(f.estandar.getId()))
                .thenReturn(List.of(f.grupoHabilitado, f.grupoDeshabilitado));
        lenient().when(itemRepository.findByGrupoIdInOrderByOrdenAsc(anyCollection()))
                .thenReturn(List.of(f.itemHabilitado));
        lenient().when(itemRepository.findByGrupoEstandarIdOrderByOrdenAsc(f.estandar.getId()))
                .thenReturn(List.of(f.itemHabilitado, f.itemDeshabilitado));
        lenient().when(areaRepository.findByEstandarIdOrderByOrdenAsc(f.estandar.getId()))
                .thenReturn(List.of(f.areaHabilitada, f.areaPracticaHabilitada, f.areaDeshabilitada));
        return f;
    }

    @Test
    @DisplayName("calcularCumplimientoHorarioPlan suma solo asignaturas SE_DICTA, excluye NO_SE_DICTA y evalúa mínimos globales de carrera")
    void calcularCumplimientoHorarioPlanSumaSoloSeDicta() {
        Fixture f = crearFixture(TipoSeccionEstandar22.EJES, TipoEvaluacionEstandar22.ESCALA_UNICA, 100);
        f.estandar.setHabilitar311(true);
        f.estandar.setModo311(ModoSeccion311.BLOQUES_CURRICULARES);
        f.estandar.setHorasMinimasTotal311(300);
        f.estandar.setHabilitar312(true);
        f.estandar.setModo312(ModoSeccion312.MODALIDADES_PRACTICAS);
        f.estandar.setHorasMinimasTotal312(100);
        f.areaHabilitada.setHorasMinimasCarrera(250);
        f.areaPracticaHabilitada.setHorasMinimasCarrera(120);

        ActividadCurricularPlanJpaEntity asig2 = new ActividadCurricularPlanJpaEntity();
        asig2.setId(UUID.randomUUID());
        asig2.setPlanEstudio(f.plan);
        asig2.setCodigo("FIS101");
        asig2.setNombre("Física I");
        asig2.setHorasReloj(160);
        asig2.setEstadoDictado(EstadoDictadoActividad.SE_DICTA);

        ActividadCurricularPlanJpaEntity asigNoSeDicta = new ActividadCurricularPlanJpaEntity();
        asigNoSeDicta.setId(UUID.randomUUID());
        asigNoSeDicta.setPlanEstudio(f.plan);
        asigNoSeDicta.setCodigo("OPT999");
        asigNoSeDicta.setNombre("Optativa inactiva");
        asigNoSeDicta.setHorasReloj(500);
        asigNoSeDicta.setEstadoDictado(EstadoDictadoActividad.NO_SE_DICTA);

        f.asig.setEstadoDictado(EstadoDictadoActividad.SE_DICTA);

        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(f.plan.getId()))
                .thenReturn(List.of(f.asig, asig2, asigNoSeDicta));

        ActividadCurricularCargaHorariaAreaJpaEntity c1 = new ActividadCurricularCargaHorariaAreaJpaEntity();
        c1.setActividadCurricular(f.asig);
        c1.setAreaCargaHoraria(f.areaHabilitada);
        c1.setHorasTotalPresencial(80);
        c1.setHorasTotalDistancia(20);

        ActividadCurricularCargaHorariaAreaJpaEntity c2 = new ActividadCurricularCargaHorariaAreaJpaEntity();
        c2.setActividadCurricular(asig2);
        c2.setAreaCargaHoraria(f.areaHabilitada);
        c2.setHorasTotalPresencial(160);
        c2.setHorasTotalDistancia(0);

        ActividadCurricularCargaHorariaAreaJpaEntity p1 = new ActividadCurricularCargaHorariaAreaJpaEntity();
        p1.setActividadCurricular(f.asig);
        p1.setAreaCargaHoraria(f.areaPracticaHabilitada);
        p1.setHorasPracticaPresencial(40);
        p1.setHorasPracticaDistancia(10);

        when(cargaAreaRepository.findByActividadCurricularIdIn(anyCollection()))
                .thenReturn(List.of(c1, c2, p1));
        when(cargaResumenRepository.findByActividadCurricularIdIn(anyCollection()))
                .thenReturn(List.of());

        CumplimientoCargaHorariaPlanDto res = service.calcularCumplimientoHorarioPlan(f.plan.getId());

        assertEquals(2, res.totalAsignaturasSeDicta());
        // 3.1.1: 80+20 + 160 = 260 >= 250 (cumple fila), pero 260 < 300 (no cumple total global 3.1.1)
        assertEquals(260, res.horasAcumuladasTotal311());
        assertEquals(300, res.horasMinimasTotal311());
        assertEquals(-40, res.diferenciaTotal311());
        assertFalse(res.cumpleTotal311());
        assertEquals(1, res.filas311().size());
        assertTrue(res.filas311().get(0).cumple());
        assertEquals(10, res.filas311().get(0).diferencia());

        // 3.1.2: 40+10 = 50 < 120 (fila) y 50 < 100 (total)
        assertEquals(50, res.horasAcumuladasTotal312());
        assertFalse(res.cumpleTotal312());
        assertFalse(res.filas312().get(0).cumple());
        assertEquals(-70, res.filas312().get(0).diferencia());
        assertFalse(res.cumpleGlobal());
    }

    @Test
    @DisplayName("Una asignatura individual puede guardar 0 h en un área aunque la carrera exija mínimo global")
    void asignaturaIndividualPermiteCeroHorasAunqueExistaMinimoCarrera() {
        Fixture f = crearFixture(TipoSeccionEstandar22.COMPETENCIAS, TipoEvaluacionEstandar22.MATRIZ_MULTIPLE, 80);
        f.areaHabilitada.setHorasMinimasCarrera(750);
        f.areaPracticaHabilitada.setHorasMinimasCarrera(200);
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(user(UUID.randomUUID(), "AEA")));
        when(cargaResumenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(estandarService.toDetalleDto(f.estandar)).thenReturn(f.detalleDto);

        GuardarFichaEstandarRequest req = new GuardarFichaEstandarRequest(
                List.of(new FichaEstandarActividadDto.Respuesta22Dto(f.itemHabilitado.getId(), f.escala1.getId())),
                List.of(
                        new FichaEstandarActividadDto.CargaAreaDto(f.areaHabilitada.getId(), 80, 0, 0, 0),
                        new FichaEstandarActividadDto.CargaAreaDto(f.areaPracticaHabilitada.getId(), 0, 0, 0, 0)
                ),
                new FichaEstandarActividadDto.ResumenCargaDto(null, 5, 0, 0, 0)
        );
        assertDoesNotThrow(() -> service.guardarFicha(f.plan.getId(), f.asig.getId(), req));
    }

    private User user(UUID id, String roleCode) {
        Role role = Role.system(UUID.randomUUID(), roleCode, roleCode, roleCode);
        return User.rehydrate(id, "user_" + roleCode.toLowerCase(), roleCode.toLowerCase() + "@undec.edu.ar",
                "hash", true, false, UUID.randomUUID(), Set.of(role));
    }
}
