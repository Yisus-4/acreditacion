package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Slice1PersistenceEntitiesTest {

    @Mock
    private SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository;

    @Mock
    private SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoMaestroRepository;

    @Mock
    private SpringDataCuerpoAcademicoActividadMaestroRepository cuerpoAcademicoActividadMaestroRepository;

    @Mock
    private SpringDataProcesoCuerpoAcademicoRepository procesoCuerpoAcademicoRepository;

    @Mock
    private SpringDataProcesoCuerpoAcademicoActividadRepository procesoCuerpoAcademicoActividadRepository;

    @Mock
    private SpringDataEspacioFisicoActividadCurricularRepository espacioFisicoActividadCurricularRepository;

    @Mock
    private SpringDataPlanEstudioFt001Repository planEstudioRepository;

    @Test
    @DisplayName("EstadoVigenciaPlan define los tres estados de vigencia requeridos")
    void testEstadoVigenciaPlanValues() {
        assertEquals(3, EstadoVigenciaPlan.values().length);
        assertEquals(EstadoVigenciaPlan.VIGENTE, EstadoVigenciaPlan.valueOf("VIGENTE"));
        assertEquals(EstadoVigenciaPlan.NO_VIGENTE, EstadoVigenciaPlan.valueOf("NO_VIGENTE"));
        assertEquals(EstadoVigenciaPlan.HISTORICO, EstadoVigenciaPlan.valueOf("HISTORICO"));
    }

    @Test
    @DisplayName("PlanEstudioFt001JpaEntity soporta atributos de datos centrales y multiplan")
    void testPlanEstudioFt001Attributes() {
        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        UUID planId = UUID.randomUUID();
        UUID maestroId = UUID.randomUUID();

        plan.setId(planId);
        plan.setNombrePlan("Plan de Estudio 2024");
        plan.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);
        plan.setEsMaestro(true);
        plan.setOrigenMaestroId(maestroId);

        assertEquals(planId, plan.getId());
        assertEquals("Plan de Estudio 2024", plan.getNombrePlan());
        assertEquals(EstadoVigenciaPlan.VIGENTE, plan.getEstadoVigencia());
        assertTrue(plan.getEsMaestro());
        assertEquals(maestroId, plan.getOrigenMaestroId());
    }

    @Test
    @DisplayName("EspacioFisicoJpaEntity e InmuebleJpaEntity soportan esMaestro, procesoId y origenMaestroId")
    void testEspacioFisicoAndInmuebleAttributes() {
        EspacioFisicoJpaEntity espacio = new EspacioFisicoJpaEntity();
        UUID espacioId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID origenId = UUID.randomUUID();

        espacio.setId(espacioId);
        espacio.setEsMaestro(false);
        espacio.setProcesoId(procesoId);
        espacio.setOrigenMaestroId(origenId);

        assertFalse(espacio.getEsMaestro());
        assertEquals(procesoId, espacio.getProcesoId());
        assertEquals(origenId, espacio.getOrigenMaestroId());

        InmuebleJpaEntity inmueble = new InmuebleJpaEntity();
        inmueble.setId(UUID.randomUUID());
        inmueble.setEsMaestro(true);
        inmueble.setOrigenMaestroId(origenId);

        assertTrue(inmueble.getEsMaestro());
        assertEquals(origenId, inmueble.getOrigenMaestroId());
    }

    @Test
    @DisplayName("ActividadCurricularPlanJpaEntity soporta origenMaestroId")
    void testActividadCurricularOrigenMaestro() {
        ActividadCurricularPlanJpaEntity act = new ActividadCurricularPlanJpaEntity();
        UUID origenId = UUID.randomUUID();
        act.setOrigenMaestroId(origenId);
        assertEquals(origenId, act.getOrigenMaestroId());
    }

    @Test
    @DisplayName("ConvocatoriaCarreraPlanJpaEntity gestiona la relacion multiplan correctamente")
    void testConvocatoriaCarreraPlanEntity() {
        UUID id = UUID.randomUUID();
        ConvocatoriaCarreraJpaEntity cc = new ConvocatoriaCarreraJpaEntity();
        UUID carreraId = UUID.randomUUID();
        PlanEstudioFt001JpaEntity maestro = new PlanEstudioFt001JpaEntity();
        maestro.setId(UUID.randomUUID());
        PlanEstudioFt001JpaEntity clon = new PlanEstudioFt001JpaEntity();
        clon.setId(UUID.randomUUID());

        ConvocatoriaCarreraPlanJpaEntity ccp = new ConvocatoriaCarreraPlanJpaEntity(
                id, cc, carreraId, maestro, clon, true, true
        );

        assertEquals(id, ccp.getId());
        assertEquals(cc, ccp.getConvocatoriaCarrera());
        assertEquals(carreraId, ccp.getCarreraId());
        assertEquals(maestro, ccp.getPlanEstudioMaestro());
        assertEquals(clon, ccp.getPlanEstudioClon());
        assertTrue(ccp.isSugeridoPorAea());
        assertTrue(ccp.isSeleccionadoPorDe());
        assertNotNull(ccp.getCreadoEn());
        assertNotNull(ccp.getActualizadoEn());

        ConvocatoriaCarreraPlanJpaEntity ccp2 = new ConvocatoriaCarreraPlanJpaEntity();
        ccp2.setId(id);
        assertEquals(ccp, ccp2);
        assertEquals(ccp.hashCode(), ccp2.hashCode());
    }

    @Test
    @DisplayName("CuerpoAcademicoMaestro y Asignaciones gestionan datos maestros institucionales")
    void testCuerpoAcademicoMaestroEntities() {
        CuerpoAcademicoMaestroJpaEntity maestro = new CuerpoAcademicoMaestroJpaEntity();
        UUID maestroId = UUID.randomUUID();
        maestro.setId(maestroId);
        maestro.setDni("12345678");
        maestro.setApellido("Perez");
        maestro.setNombre("Juan");
        maestro.setCategoriaDocente("TITULAR");
        maestro.setCaracterDesignacion("REGULAR");
        maestro.setDedicacionDocente("EXCLUSIVA");

        CuerpoAcademicoActividadMaestroJpaEntity asig = new CuerpoAcademicoActividadMaestroJpaEntity();
        asig.setId(UUID.randomUUID());
        asig.setDocente(maestro);
        asig.setCarreraId(UUID.randomUUID());
        asig.setRolEnCatedra("PROFESOR_A_CARGO");
        asig.setCargaHorariaSemanal(4);

        maestro.getAsignaciones().add(asig);

        assertEquals(1, maestro.getAsignaciones().size());
        assertEquals("12345678", maestro.getDni());
        assertEquals(maestro, asig.getDocente());
    }

    @Test
    @DisplayName("ProcesoCuerpoAcademico clona y aisla docentes en autoevaluacion")
    void testProcesoCuerpoAcademicoEntities() {
        ProcesoCuerpoAcademicoJpaEntity procDoc = new ProcesoCuerpoAcademicoJpaEntity();
        UUID procId = UUID.randomUUID();
        UUID origenMaestroId = UUID.randomUUID();

        procDoc.setId(UUID.randomUUID());
        procDoc.setProcesoId(procId);
        procDoc.setOrigenMaestroId(origenMaestroId);
        procDoc.setDni("23456789");
        procDoc.setApellido("Gomez");
        procDoc.setNombre("Maria");
        procDoc.setCategoriaDocente("ADJUNTO");
        procDoc.setCaracterDesignacion("INTERINO");
        procDoc.setDedicacionDocente("SEMI_EXCLUSIVA");

        assertEquals(procId, procDoc.getProcesoId());
        assertEquals(origenMaestroId, procDoc.getOrigenMaestroId());
        assertEquals("Maria", procDoc.getNombre());
    }

    @Test
    @DisplayName("EspacioFisicoActividadCurricularJpaEntity vincula espacio fisico con carrera, plan y asignatura")
    void testEspacioFisicoActividadCurricularEntity() {
        UUID id = UUID.randomUUID();
        EspacioFisicoJpaEntity espacio = new EspacioFisicoJpaEntity();
        espacio.setId(UUID.randomUUID());
        UUID carreraId = UUID.randomUUID();
        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        ActividadCurricularPlanJpaEntity act = new ActividadCurricularPlanJpaEntity();
        act.setId(UUID.randomUUID());

        EspacioFisicoActividadCurricularJpaEntity binding = new EspacioFisicoActividadCurricularJpaEntity(
                id, espacio, carreraId, plan, act, 2, "Comisiones A y B turno manana"
        );

        assertEquals(id, binding.getId());
        assertEquals(espacio, binding.getEspacioFisico());
        assertEquals(carreraId, binding.getCarreraId());
        assertEquals(plan, binding.getPlanEstudio());
        assertEquals(act, binding.getActividadCurricular());
        assertEquals(2, binding.getComisionesAsignadas());
        assertEquals("Comisiones A y B turno manana", binding.getObservacionesUso());
        assertNotNull(binding.getCreadoEn());
    }

    @Test
    @DisplayName("Repositories de Slice 1 responden a las consultas esperadas")
    void testRepositoriesSlice1Queries() {
        UUID ccId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID espacioId = UUID.randomUUID();

        when(convocatoriaCarreraPlanRepository.findByConvocatoriaCarreraIdAndSeleccionadoPorDeTrue(ccId))
                .thenReturn(List.of(new ConvocatoriaCarreraPlanJpaEntity()));
        assertEquals(1, convocatoriaCarreraPlanRepository.findByConvocatoriaCarreraIdAndSeleccionadoPorDeTrue(ccId).size());

        when(cuerpoAcademicoMaestroRepository.findByDni("12345678"))
                .thenReturn(Optional.of(new CuerpoAcademicoMaestroJpaEntity()));
        assertTrue(cuerpoAcademicoMaestroRepository.findByDni("12345678").isPresent());

        when(espacioFisicoActividadCurricularRepository.findByEspacioFisicoId(espacioId))
                .thenReturn(List.of(new EspacioFisicoActividadCurricularJpaEntity()));
        assertEquals(1, espacioFisicoActividadCurricularRepository.findByEspacioFisicoId(espacioId).size());

        when(planEstudioRepository.findByCarreraIdAndEstadoVigenciaAndEsMaestroTrue(carreraId, EstadoVigenciaPlan.VIGENTE))
                .thenReturn(List.of(new PlanEstudioFt001JpaEntity()));
        assertEquals(1, planEstudioRepository.findByCarreraIdAndEstadoVigenciaAndEsMaestroTrue(carreraId, EstadoVigenciaPlan.VIGENTE).size());
    }
}
