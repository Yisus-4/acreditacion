package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.EstadoActividad;
import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActividadCurricularPlanDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActualizarAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.CrearAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ActualizarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.CrearAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.EliminarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.SincronizacionBidireccionalPlanService;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlanEstudioControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @Mock
    private SpringDataCarreraRepository carreraRepository;

    @Mock
    private SpringDataProcesoAcreditacionRepository procesoRepository;

    @Mock
    private CrearAsignaturaPlanService crearAsignaturaPlanService;

    @Mock
    private ActualizarAsignaturaPlanService actualizarAsignaturaPlanService;

    @Mock
    private EliminarAsignaturaPlanService eliminarAsignaturaPlanService;

    @Mock
    private SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;

    @Mock
    private PlanTrabajoRepositoryPort planTrabajoRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private PlanEstudioController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new PlanEstudioController(
                planRepository,
                asignaturaRepository,
                carreraRepository,
                procesoRepository,
                crearAsignaturaPlanService,
                actualizarAsignaturaPlanService,
                eliminarAsignaturaPlanService,
                sincronizacionBidireccionalPlanService,
                planTrabajoRepository,
                authenticatedUserProvider
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AutoevaluacionExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/planes-estudio?procesoId={id}&carreraId={cId} retorna vacío sin hacer fallback al maestro si el proceso no tiene copia")
    void deberiaRetornarVacioSinFallbackAMaestroCuandoProcesoNoTienePlanYSePasaCarreraId() throws Exception {
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/planes-estudio")
                        .param("procesoId", procesoId.toString())
                        .param("carreraId", carreraId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(planRepository).findByProcesoIdAndActivoTrue(procesoId);
        verify(planRepository, never()).findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(any());
    }

    @Test
    @DisplayName("GET /api/planes-estudio?procesoId={id} retorna vacío sin hacer fallback al maestro cuando no se pasa carreraId")
    void deberiaRetornarVacioSinFallbackAMaestroCuandoNoSePasaCarreraId() throws Exception {
        UUID procesoId = UUID.randomUUID();

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/planes-estudio")
                        .param("procesoId", procesoId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(planRepository).findByProcesoIdAndActivoTrue(procesoId);
        verify(planRepository, never()).findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(any());
    }

    @Test
    @DisplayName("GET /api/planes-estudio?procesoId={id} retorna vacío si proceso no tiene plan")
    void deberiaRetornarVacioSiProcesoNoTienePlan() throws Exception {
        UUID procesoId = UUID.randomUUID();
        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/planes-estudio")
                        .param("procesoId", procesoId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(planRepository).findByProcesoIdAndActivoTrue(procesoId);
        verify(planRepository, never()).findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(any());
    }

    @Test
    @DisplayName("GET /api/planes-estudio?procesoId={id} retorna el plan de estudio de la convocatoria")
    void deberiaRetornarPlanDeLaConvocatoria() throws Exception {
        UUID procesoId = UUID.randomUUID();
        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setProcesoId(procesoId);
        plan.setCodigoResolucion("RES-2026-001");
        plan.setActivo(true);
        plan.setCreadoEn(Instant.now());

        when(planRepository.findByProcesoIdAndActivoTrue(procesoId)).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/planes-estudio")
                        .param("procesoId", procesoId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].procesoId", is(procesoId.toString())))
                .andExpect(jsonPath("$[0].codigoResolucion", is("RES-2026-001")));
    }

    @Test
    @DisplayName("POST /api/planes-estudio/{planId}/asignaturas retorna 201 Created con DTO")
    void deberiaCrearAsignaturaConExito() throws Exception {
        UUID planId = UUID.randomUUID();
        UUID asigId = UUID.randomUUID();

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "CBI-01",
                "Álgebra y Geometría Analítica",
                "C.B.I.",
                "1° Año",
                128,
                96,
                32,
                "Sin correlativas previas",
                true,
                1
        );

        ActividadCurricularPlanDto responseDto = new ActividadCurricularPlanDto(
                asigId,
                planId,
                1,
                "CBI-01",
                "Álgebra y Geometría Analítica",
                "C.B.I.",
                "1° Año",
                128,
                96,
                32,
                "Sin correlativas previas",
                true
        );

        when(crearAsignaturaPlanService.ejecutar(eq(planId), any(CrearAsignaturaRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/planes-estudio/{planId}/asignaturas", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(asigId.toString())))
                .andExpect(jsonPath("$.nombre", is("Álgebra y Geometría Analítica")))
                .andExpect(jsonPath("$.numeroOrden", is(1)))
                .andExpect(jsonPath("$.horasReloj", is(128)));
    }

    @Test
    @DisplayName("POST /api/planes-estudio/{planId}/asignaturas retorna 422 Unprocessable Entity en validación de dominio")
    void deberiaRetornar422CuandoFallaValidacionDeDominio() throws Exception {
        UUID planId = UUID.randomUUID();

        CrearAsignaturaRequest request = new CrearAsignaturaRequest(
                "CBI-01",
                "",
                "C.B.I.",
                "1° Año",
                -10,
                0,
                0,
                null,
                true,
                1
        );

        when(crearAsignaturaPlanService.ejecutar(eq(planId), any(CrearAsignaturaRequest.class)))
                .thenThrow(new DomainValidationException("Las horas reloj no pueden ser negativas."));

        mockMvc.perform(post("/api/planes-estudio/{planId}/asignaturas", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("Las horas reloj no pueden ser negativas.")));
    }

    @Test
    @DisplayName("PUT /api/planes-estudio/{planId}/asignaturas/{asigId} retorna 200 OK con DTO actualizado")
    void deberiaActualizarAsignaturaConExito() throws Exception {
        UUID planId = UUID.randomUUID();
        UUID asigId = UUID.randomUUID();

        ActualizarAsignaturaRequest request = new ActualizarAsignaturaRequest(
                asigId,
                "CBI-01",
                "Álgebra Lineal",
                "C.B.I.",
                "1° Año",
                128,
                96,
                32,
                "Sin correlativas previas",
                true,
                1
        );

        ActividadCurricularPlanDto responseDto = new ActividadCurricularPlanDto(
                asigId,
                planId,
                1,
                "CBI-01",
                "Álgebra Lineal",
                "C.B.I.",
                "1° Año",
                128,
                96,
                32,
                "Sin correlativas previas",
                true
        );

        when(actualizarAsignaturaPlanService.ejecutar(eq(planId), eq(asigId), any(ActualizarAsignaturaRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/planes-estudio/{planId}/asignaturas/{asigId}", planId, asigId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(asigId.toString())))
                .andExpect(jsonPath("$.nombre", is("Álgebra Lineal")));
    }

    @Test
    @DisplayName("DELETE /api/planes-estudio/{planId}/asignaturas/{asigId} retorna 204 No Content cuando no tiene dependencias")
    void deberiaEliminarAsignaturaSinDependencias() throws Exception {
        UUID planId = UUID.randomUUID();
        UUID asigId = UUID.randomUUID();

        doNothing().when(eliminarAsignaturaPlanService).ejecutar(planId, asigId);

        mockMvc.perform(delete("/api/planes-estudio/{planId}/asignaturas/{asigId}", planId, asigId))
                .andExpect(status().isNoContent());

        verify(eliminarAsignaturaPlanService).ejecutar(planId, asigId);
    }

    @Test
    @DisplayName("DELETE /api/planes-estudio/{planId}/asignaturas/{asigId} retorna 409 Conflict si está vinculada a ámbitos")
    void deberiaRetornar409CuandoAsignaturaEstaVinculadaAAmbitos() throws Exception {
        UUID planId = UUID.randomUUID();
        UUID asigId = UUID.randomUUID();

        doThrow(new BusinessRuleConflictException("No es posible eliminar la asignatura porque se encuentra vinculada a los ámbitos de práctica: Campo Escuela"))
                .when(eliminarAsignaturaPlanService).ejecutar(planId, asigId);

        mockMvc.perform(delete("/api/planes-estudio/{planId}/asignaturas/{asigId}", planId, asigId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("No es posible eliminar la asignatura porque se encuentra vinculada a los ámbitos de práctica: Campo Escuela")));
    }

    @Test
    @DisplayName("POST /api/planes-estudio/validar-completar-8-3 completa Actividad 8.3 cuando hay actividades curriculares activas")
    void deberiaCompletarActividad83CuandoHayAsignaturasActivas() throws Exception {
        UUID procesoId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);
        plan.setProcesoId(procesoId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
        asig.setId(UUID.randomUUID());
        asig.setEstadoDictado(EstadoDictadoActividad.SE_DICTA);
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId)).thenReturn(List.of(asig));

        ActividadPlanTrabajo act83 = mock(ActividadPlanTrabajo.class);
        when(act83.getCodigoActividad()).thenReturn("8.3");
        when(act83.getEstado()).thenReturn(EstadoActividad.PENDIENTE);
        PlanTrabajo planTrabajo = mock(PlanTrabajo.class);
        when(planTrabajo.getActividades()).thenReturn(List.of(act83));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(planTrabajo));
        when(authenticatedUserProvider.getAuthenticatedUserIdOrNull()).thenReturn(usuarioId);

        mockMvc.perform(post("/api/planes-estudio/validar-completar-8-3")
                        .param("procesoId", procesoId.toString())
                        .param("planEstudioId", planId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.completada", is(true)))
                .andExpect(jsonPath("$.totalAsignaturas", is(1)));

        verify(act83).completar(eq(usuarioId), contains("1 actividades curriculares"));
        verify(planTrabajoRepository).saveActividad(act83);
    }

    @Test
    @DisplayName("POST /api/planes-estudio/validar-completar-8-3 rechaza con 422 cuando el plan tiene 0 asignaturas activas")
    void deberiaRechazarCompletarActividad83SinAsignaturas() throws Exception {
        UUID procesoId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);
        plan.setProcesoId(procesoId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/planes-estudio/validar-completar-8-3")
                        .param("procesoId", procesoId.toString())
                        .param("planEstudioId", planId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)));

        verify(planTrabajoRepository, never()).saveActividad(any());
    }

    @Test
    @DisplayName("GET /api/planes-estudio/estado-actividad-8-3 retorna estado completado y conteo de asignaturas activas")
    void deberiaRetornarEstadoActividad83() throws Exception {
        UUID procesoId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);
        plan.setProcesoId(procesoId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        ActividadCurricularPlanJpaEntity a1 = new ActividadCurricularPlanJpaEntity();
        a1.setEstadoDictado(EstadoDictadoActividad.SE_DICTA);
        ActividadCurricularPlanJpaEntity a2 = new ActividadCurricularPlanJpaEntity();
        a2.setEstadoDictado(EstadoDictadoActividad.NO_SE_DICTA);
        when(asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId)).thenReturn(List.of(a1, a2));

        ActividadPlanTrabajo act83 = mock(ActividadPlanTrabajo.class);
        when(act83.getCodigoActividad()).thenReturn("8.3");
        when(act83.getEstado()).thenReturn(EstadoActividad.COMPLETADA);
        PlanTrabajo planTrabajo = mock(PlanTrabajo.class);
        when(planTrabajo.getActividades()).thenReturn(List.of(act83));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(planTrabajo));

        mockMvc.perform(get("/api/planes-estudio/estado-actividad-8-3")
                        .param("procesoId", procesoId.toString())
                        .param("planEstudioId", planId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completada", is(true)))
                .andExpect(jsonPath("$.totalAsignaturas", is(1)));
    }
}
