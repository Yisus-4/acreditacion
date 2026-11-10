package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AutoevaluacionCarreraAmbitoDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ValidarPrecedenciaAmbitosService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AmbitoPracticaControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SpringDataAmbitoPracticaRepository ambitoRepository;

    @Mock
    private SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository;

    @Mock
    private SpringDataInmuebleRepository inmuebleRepository;

    @Mock
    private SpringDataConvenioInstitucionalRepository convenioRepository;

    @Mock
    private ValidarPrecedenciaAmbitosService validarPrecedenciaAmbitosService;

    private AmbitoPracticaController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AmbitoPracticaController(
                ambitoRepository,
                autoevaluacionAmbitoRepository,
                inmuebleRepository,
                convenioRepository,
                validarPrecedenciaAmbitosService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AutoevaluacionExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/ambitos-practica/autoevaluacion?procesoId={id} retorna 200 con vinculaciones del proceso")
    void deberiaListarAutoevaluacionConAliasAutoevaluacion() throws Exception {
        UUID procesoId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(ambitoId);
        ambito.setDenominacion("Campo Experimental Los Sarmientos");
        ambito.setTipoCategoria("INTERNO");

        AutoevaluacionCarreraAmbitoJpaEntity vinculo = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculo.setId(UUID.randomUUID());
        vinculo.setProcesoId(procesoId);
        vinculo.setAmbito(ambito);
        vinculo.setUtilizadoPorCarrera(true);
        vinculo.setAsignaturasVinculadas("Topografía [AG-01]");
        vinculo.setResponsablePractica("Gómez, Carlos; Martino, Laura");

        when(autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId))
                .thenReturn(List.of(vinculo));

        mockMvc.perform(get("/api/ambitos-practica/autoevaluacion")
                        .param("procesoId", procesoId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].procesoId", is(procesoId.toString())))
                .andExpect(jsonPath("$[0].responsablePractica", is("Gómez, Carlos; Martino, Laura")))
                .andExpect(jsonPath("$[0].asignaturasVinculadas", is("Topografía [AG-01]")));
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/autoevaluacion guarda vinculación usando el alias")
    void deberiaGuardarVinculacionConAliasAutoevaluacion() throws Exception {
        UUID procesoId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(ambitoId);
        ambito.setDenominacion("Campo Experimental Los Sarmientos");
        ambito.setTipoCategoria("INTERNO");

        AutoevaluacionCarreraAmbitoDto requestDto = new AutoevaluacionCarreraAmbitoDto(
                null,
                procesoId,
                ambitoId,
                null,
                true,
                "Topografía [AG-01]; Geodesia [AG-02]",
                "Pérez, Juan; García, María",
                "Lunes y Miércoles 8-12",
                "Prácticas de campo",
                "Sin observaciones",
                null,
                null
        );

        when(ambitoRepository.findById(ambitoId)).thenReturn(Optional.of(ambito));
        when(autoevaluacionAmbitoRepository.findByProcesoIdAndAmbitoId(procesoId, ambitoId)).thenReturn(Optional.empty());

        AutoevaluacionCarreraAmbitoJpaEntity guardado = new AutoevaluacionCarreraAmbitoJpaEntity();
        guardado.setId(UUID.randomUUID());
        guardado.setProcesoId(procesoId);
        guardado.setAmbito(ambito);
        guardado.setUtilizadoPorCarrera(true);
        guardado.setAsignaturasVinculadas("Topografía [AG-01]; Geodesia [AG-02]");
        guardado.setResponsablePractica("Pérez, Juan; García, María");
        guardado.setCreadoEn(Instant.now());

        when(autoevaluacionAmbitoRepository.save(any(AutoevaluacionCarreraAmbitoJpaEntity.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/ambitos-practica/autoevaluacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesoId", is(procesoId.toString())))
                .andExpect(jsonPath("$.responsablePractica", is("Pérez, Juan; García, María")));
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId}/completar rechaza con 422 cuando el plan tiene 0 asignaturas")
    void deberiaRechazarCompletarActividadCon422SiPlanTiene0Asignaturas() throws Exception {
        UUID procesoId = UUID.randomUUID();

        doThrow(new DomainValidationException(
                "Precedencia requerida (CONEAU PRG-002): No es posible completar esta actividad hasta que se carguen las asignaturas en la Dimensión 8.3 (Plan de Estudio)."
        )).when(validarPrecedenciaAmbitosService).validar(eq(procesoId), eq("INTERNOS_FT008"));

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}/completar", procesoId)
                        .param("actividad", "INTERNOS_FT008")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("Precedencia requerida (CONEAU PRG-002): No es posible completar esta actividad hasta que se carguen las asignaturas en la Dimensión 8.3 (Plan de Estudio).")));
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId}/completar completa exitosamente con 200 cuando el plan tiene asignaturas")
    void deberiaCompletarActividadCon200CuandoPlanTieneAsignaturas() throws Exception {
        UUID procesoId = UUID.randomUUID();

        doNothing().when(validarPrecedenciaAmbitosService).validar(eq(procesoId), eq("INTERNOS_FT008"));

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}/completar", procesoId)
                        .param("actividad", "INTERNOS_FT008")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completado", is(true)))
                .andExpect(jsonPath("$.actividad", is("INTERNOS_FT008")));
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/autoevaluacion/completar completa con alias y queryParam procesoId")
    void deberiaCompletarConAliasAutoevaluacion() throws Exception {
        UUID procesoId = UUID.randomUUID();

        doNothing().when(validarPrecedenciaAmbitosService).validar(eq(procesoId), eq("EXTERNOS_FT009"));

        mockMvc.perform(post("/api/ambitos-practica/autoevaluacion/completar")
                        .param("procesoId", procesoId.toString())
                        .param("actividad", "EXTERNOS_FT009")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completado", is(true)))
                .andExpect(jsonPath("$.actividad", is("EXTERNOS_FT009")));
    }
}
