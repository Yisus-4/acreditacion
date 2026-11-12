package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ActualizarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.CrearAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.EliminarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlanesEstudioVigentesEndpointTest {

    private MockMvc mockMvc;
    private SpringDataPlanEstudioFt001Repository planRepository;

    @BeforeEach
    void setUp() {
        planRepository = mock(SpringDataPlanEstudioFt001Repository.class);
        SpringDataActividadCurricularPlanRepository asigRepo = mock(SpringDataActividadCurricularPlanRepository.class);
        SpringDataCarreraRepository carreraRepo = mock(SpringDataCarreraRepository.class);
        SpringDataProcesoAcreditacionRepository procRepo = mock(SpringDataProcesoAcreditacionRepository.class);
        CrearAsignaturaPlanService crearAsigService = mock(CrearAsignaturaPlanService.class);
        ActualizarAsignaturaPlanService actAsigService = mock(ActualizarAsignaturaPlanService.class);
        EliminarAsignaturaPlanService elimAsigService = mock(EliminarAsignaturaPlanService.class);

        PlanEstudioController controller = new PlanEstudioController(
                planRepository, asigRepo, carreraRepo, procRepo,
                crearAsigService, actAsigService, elimAsigService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/planes-estudio/carrera/{carreraId}/vigentes debe retornar solo planes maestros vigentes")
    void debeRetornarSoloPlanesVigentes() throws Exception {
        UUID carreraId = UUID.randomUUID();
        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setId(carreraId);
        carrera.setNombre("Ingeniería en Sistemas");

        PlanEstudioFt001JpaEntity plan1 = new PlanEstudioFt001JpaEntity();
        plan1.setId(UUID.randomUUID());
        plan1.setCarrera(carrera);
        plan1.setNombrePlan("Plan 2018");
        plan1.setCodigoResolucion("RES-2018");
        plan1.setAnioAprobacion(2018);
        plan1.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);
        plan1.setEsMaestro(true);

        PlanEstudioFt001JpaEntity plan2 = new PlanEstudioFt001JpaEntity();
        plan2.setId(UUID.randomUUID());
        plan2.setCarrera(carrera);
        plan2.setNombrePlan("Plan 2024");
        plan2.setCodigoResolucion("RES-2024");
        plan2.setAnioAprobacion(2024);
        plan2.setEstadoVigencia(EstadoVigenciaPlan.VIGENTE);
        plan2.setEsMaestro(true);

        when(planRepository.findByCarreraIdAndEstadoVigenciaAndEsMaestroTrueOrderByAnioAprobacionDesc(
                carreraId, EstadoVigenciaPlan.VIGENTE))
                .thenReturn(List.of(plan2, plan1));

        mockMvc.perform(get("/api/planes-estudio/carrera/" + carreraId + "/vigentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombrePlan").value("Plan 2024"))
                .andExpect(jsonPath("$[0].estadoVigencia").value("VIGENTE"))
                .andExpect(jsonPath("$[0].esMaestro").value(true))
                .andExpect(jsonPath("$[1].nombrePlan").value("Plan 2018"))
                .andExpect(jsonPath("$[1].estadoVigencia").value("VIGENTE"))
                .andExpect(jsonPath("$[1].esMaestro").value(true));

        verify(planRepository).findByCarreraIdAndEstadoVigenciaAndEsMaestroTrueOrderByAnioAprobacionDesc(
                carreraId, EstadoVigenciaPlan.VIGENTE);
    }
}
