package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AsignarActividadEspacioRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EspacioFisicoActividadCurricularControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SpringDataEspacioFisicoRepository espacioFisicoRepository;
    @Mock
    private SpringDataEspacioFisicoActividadCurricularRepository espacioActividadRepository;
    @Mock
    private SpringDataPlanEstudioFt001Repository planEstudioRepository;
    @Mock
    private SpringDataActividadCurricularPlanRepository actividadRepository;

    @InjectMocks
    private EspacioFisicoActividadCurricularController controller;

    private UUID espacioId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        espacioId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/autoevaluacion/espacios/{id}/actividades-curriculares retorna lista de vinculaciones")
    void debeListarActividadesPorEspacio() throws Exception {
        when(espacioFisicoRepository.existsById(espacioId)).thenReturn(true);

        EspacioFisicoJpaEntity espacio = new EspacioFisicoJpaEntity();
        espacio.setId(espacioId);

        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setNombre("Ingeniería en Sistemas");

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setNombrePlan("Plan 2024");
        plan.setCarrera(carrera);

        ActividadCurricularPlanJpaEntity actividad = new ActividadCurricularPlanJpaEntity();
        actividad.setId(UUID.randomUUID());
        actividad.setCodigo("ISI-101");
        actividad.setNombre("Álgebra");

        EspacioFisicoActividadCurricularJpaEntity binding = new EspacioFisicoActividadCurricularJpaEntity(
                UUID.randomUUID(), espacio, UUID.randomUUID(), plan, actividad, 2, "Uso intensivo"
        );

        when(espacioActividadRepository.findByEspacioFisicoId(espacioId)).thenReturn(List.of(binding));

        mockMvc.perform(get("/api/autoevaluacion/espacios/" + espacioId + "/actividades-curriculares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].espacioFisicoId").value(espacioId.toString()))
                .andExpect(jsonPath("$[0].planEstudioNombre").value("Plan 2024"))
                .andExpect(jsonPath("$[0].actividadNombre").value("Álgebra"))
                .andExpect(jsonPath("$[0].comisionesAsignadas").value(2));
    }

    @Test
    @DisplayName("PUT /api/autoevaluacion/espacios/{id}/actividades-curriculares reemplaza vinculaciones por lote")
    void debeReemplazarActividadesPorEspacio() throws Exception {
        EspacioFisicoJpaEntity espacio = new EspacioFisicoJpaEntity();
        espacio.setId(espacioId);
        when(espacioFisicoRepository.findById(espacioId)).thenReturn(Optional.of(espacio));

        UUID planId = UUID.randomUUID();
        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);
        plan.setNombrePlan("Plan 2024");
        when(planEstudioRepository.findById(planId)).thenReturn(Optional.of(plan));

        UUID actId = UUID.randomUUID();
        ActividadCurricularPlanJpaEntity act = new ActividadCurricularPlanJpaEntity();
        act.setId(actId);
        act.setNombre("Física I");
        when(actividadRepository.findById(actId)).thenReturn(Optional.of(act));

        when(espacioActividadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AsignarActividadEspacioRequest req = new AsignarActividadEspacioRequest(
                UUID.randomUUID(), planId, actId, 3, "Clases matutinas"
        );

        mockMvc.perform(put("/api/autoevaluacion/espacios/" + espacioId + "/actividades-curriculares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(req))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].espacioFisicoId").value(espacioId.toString()))
                .andExpect(jsonPath("$[0].actividadNombre").value("Física I"))
                .andExpect(jsonPath("$[0].comisionesAsignadas").value(3));

        verify(espacioActividadRepository).deleteByEspacioFisicoId(espacioId);
        verify(espacioActividadRepository).save(any());
    }
}
