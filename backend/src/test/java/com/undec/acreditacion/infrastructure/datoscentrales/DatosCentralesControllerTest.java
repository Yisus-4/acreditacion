package com.undec.acreditacion.infrastructure.datoscentrales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.*;
import com.undec.acreditacion.infrastructure.datoscentrales.service.DatosCentralesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DatosCentralesControllerTest {

    private MockMvc mockMvc;
    private DatosCentralesService datosCentralesService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        datosCentralesService = mock(DatosCentralesService.class);
        objectMapper = new ObjectMapper();
        DatosCentralesController controller = new DatosCentralesController(datosCentralesService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/datos-centrales/resumen debe retornar métricas agregadas")
    void debeRetornarResumen() throws Exception {
        ResumenDatosCentralesDto resumen = new ResumenDatosCentralesDto(12, 85, 34, 8);
        when(datosCentralesService.obtenerResumen()).thenReturn(resumen);

        mockMvc.perform(get("/api/datos-centrales/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planesEstudioVigentes").value(12))
                .andExpect(jsonPath("$.cuerpoAcademicoActivo").value(85))
                .andExpect(jsonPath("$.espaciosFisicos").value(34))
                .andExpect(jsonPath("$.ambitosPractica").value(8));

        verify(datosCentralesService).obtenerResumen();
    }

    @Test
    @DisplayName("GET /api/datos-centrales/planes-estudio debe listar planes maestros filtrados")
    void debeListarPlanesEstudio() throws Exception {
        UUID carreraId = UUID.randomUUID();
        PlanEstudioMaestroDto planDto = new PlanEstudioMaestroDto(
                UUID.randomUUID(), carreraId, "Ingeniería en Sistemas", "ISI",
                "Plan 2024", EstadoVigenciaPlan.VIGENTE, true, "RES-2024-01",
                2024, 2024, 5, 3000, 500, 3500, 200, true, 42,
                Instant.now(), Instant.now()
        );
        when(datosCentralesService.listarPlanesEstudio(eq(carreraId), eq(EstadoVigenciaPlan.VIGENTE)))
                .thenReturn(List.of(planDto));

        mockMvc.perform(get("/api/datos-centrales/planes-estudio")
                        .param("carreraId", carreraId.toString())
                        .param("estadoVigencia", "VIGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombrePlan").value("Plan 2024"))
                .andExpect(jsonPath("$[0].estadoVigencia").value("VIGENTE"))
                .andExpect(jsonPath("$[0].esMaestro").value(true));
    }

    @Test
    @DisplayName("POST /api/datos-centrales/planes-estudio debe crear un nuevo plan maestro")
    void debeCrearPlanEstudioMaestro() throws Exception {
        UUID carreraId = UUID.randomUUID();
        CrearPlanEstudioMaestroRequest request = new CrearPlanEstudioMaestroRequest(
                carreraId, "Plan 2024", "RES-2024-01", 2024, 2024, 5,
                3000, 500, 3500, 200, "{}", "Fundamentacion", "Sintetica",
                "Comunes", "Analista", "3 años", "Actividades", "Tesis", "Otra"
        );
        PlanEstudioMaestroDto creado = new PlanEstudioMaestroDto(
                UUID.randomUUID(), carreraId, "Ingeniería en Sistemas", "ISI",
                "Plan 2024", EstadoVigenciaPlan.VIGENTE, true, "RES-2024-01",
                2024, 2024, 5, 3000, 500, 3500, 200, true, 0,
                Instant.now(), Instant.now()
        );
        when(datosCentralesService.crearPlanEstudioMaestro(any())).thenReturn(creado);

        mockMvc.perform(post("/api/datos-centrales/planes-estudio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombrePlan").value("Plan 2024"))
                .andExpect(jsonPath("$.esMaestro").value(true));
    }

    @Test
    @DisplayName("PATCH /api/datos-centrales/planes-estudio/{id}/estado-vigencia debe actualizar estado")
    void debeCambiarEstadoVigencia() throws Exception {
        UUID planId = UUID.randomUUID();
        CambiarVigenciaPlanRequest request = new CambiarVigenciaPlanRequest(EstadoVigenciaPlan.HISTORICO);
        PlanEstudioMaestroDto actualizado = new PlanEstudioMaestroDto(
                planId, UUID.randomUUID(), "Ingeniería en Sistemas", "ISI",
                "Plan 2010", EstadoVigenciaPlan.HISTORICO, true, "RES-2010-01",
                2010, 2010, 5, 3000, 500, 3500, 200, true, 40,
                Instant.now(), Instant.now()
        );
        when(datosCentralesService.cambiarEstadoVigenciaPlan(eq(planId), eq(EstadoVigenciaPlan.HISTORICO)))
                .thenReturn(actualizado);

        mockMvc.perform(patch("/api/datos-centrales/planes-estudio/" + planId + "/estado-vigencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVigencia").value("HISTORICO"));
    }

    @Test
    @DisplayName("GET /api/datos-centrales/cuerpo-academico debe buscar docentes maestros")
    void debeBuscarCuerpoAcademico() throws Exception {
        CuerpoAcademicoMaestroDto docente = new CuerpoAcademicoMaestroDto(
                UUID.randomUUID(), "30123456", "20-30123456-7", "Perez", "Juan",
                "jperez@undec.edu.ar", "jperez@gmail.com", "3825123456",
                "Ingeniero en Sistemas", "Magíster en Software", "TITULAR", "REGULAR", "EXCLUSIVA",
                40, true, List.of(), Instant.now(), Instant.now()
        );
        when(datosCentralesService.listarCuerpoAcademico(null, "Perez"))
                .thenReturn(List.of(docente));

        mockMvc.perform(get("/api/datos-centrales/cuerpo-academico")
                        .param("query", "Perez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apellido").value("Perez"))
                .andExpect(jsonPath("$[0].dni").value("30123456"));
    }

    @Test
    @DisplayName("POST /api/datos-centrales/cuerpo-academico debe registrar docente maestro")
    void debeRegistrarDocenteMaestro() throws Exception {
        CrearCuerpoAcademicoMaestroRequest request = new CrearCuerpoAcademicoMaestroRequest(
                "30123456", "20-30123456-7", "Perez", "Juan",
                "jperez@undec.edu.ar", null, "3825123456",
                "Ingeniero", "Magíster", "TITULAR", "REGULAR", "EXCLUSIVA",
                40, List.of()
        );
        CuerpoAcademicoMaestroDto creado = new CuerpoAcademicoMaestroDto(
                UUID.randomUUID(), "30123456", "20-30123456-7", "Perez", "Juan",
                "jperez@undec.edu.ar", null, "3825123456",
                "Ingeniero", "Magíster", "TITULAR", "REGULAR", "EXCLUSIVA",
                40, true, List.of(), Instant.now(), Instant.now()
        );
        when(datosCentralesService.crearCuerpoAcademicoMaestro(any())).thenReturn(creado);

        mockMvc.perform(post("/api/datos-centrales/cuerpo-academico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("30123456"))
                .andExpect(jsonPath("$.apellido").value("Perez"));
    }
}
