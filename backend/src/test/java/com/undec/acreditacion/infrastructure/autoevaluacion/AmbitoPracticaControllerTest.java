package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AmbitoPracticaDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AutoevaluacionCarreraAmbitoDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ValidarPrecedenciaAmbitosService;
import com.undec.acreditacion.infrastructure.persistence.postgres.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Mock
    private SpringDataCarreraRepository carreraRepository;

    @Mock
    private SpringDataProcesoAcreditacionRepository procesoRepository;

    @Mock
    private UserRepository userRepository;

    private AmbitoPracticaController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AmbitoPracticaController(
                ambitoRepository,
                autoevaluacionAmbitoRepository,
                inmuebleRepository,
                convenioRepository,
                validarPrecedenciaAmbitosService,
                carreraRepository,
                procesoRepository,
                userRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AutoevaluacionExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String email, String role, UUID unidadAcademicaId) {
        User user = User.register("testuser", email, "secret", unidadAcademicaId);
        lenient().when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                email,
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
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
                .andExpect(jsonPath("$[0].ambito.denominacion", is("Campo Experimental Los Sarmientos")))
                .andExpect(jsonPath("$[0].responsablePractica", is("Gómez, Carlos; Martino, Laura")));
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId} persiste vinculación para la carrera especificada (Admin bypass)")
    void deberiaGuardarVinculacionParaCarreraEspecificada() throws Exception {
        authenticateUser("admin@undec.edu.ar", "ADMINISTRATOR", null);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(ambitoId);
        ambito.setDenominacion("Hospital Escuela");
        ambito.setTipoCategoria("EXTERNO");

        when(ambitoRepository.findById(ambitoId)).thenReturn(Optional.of(ambito));

        AutoevaluacionCarreraAmbitoDto requestDto = new AutoevaluacionCarreraAmbitoDto(
                null,
                procesoId,
                carreraId,
                ambitoId,
                null,
                true,
                "Práctica Profesional [MED-05]",
                "Dr. Pérez",
                "Lunes a Viernes 8-12",
                "Rotaciones Clínicas",
                "Sin observaciones",
                null,
                null
        );

        when(autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdAndAmbitoId(procesoId, carreraId, ambitoId))
                .thenReturn(Optional.empty());

        AutoevaluacionCarreraAmbitoJpaEntity guardado = new AutoevaluacionCarreraAmbitoJpaEntity();
        guardado.setId(UUID.randomUUID());
        guardado.setProcesoId(procesoId);
        guardado.setCarreraId(carreraId);
        guardado.setAmbito(ambito);
        guardado.setUtilizadoPorCarrera(true);
        guardado.setCreadoEn(Instant.now());

        ArgumentCaptor<AutoevaluacionCarreraAmbitoJpaEntity> captor = ArgumentCaptor.forClass(AutoevaluacionCarreraAmbitoJpaEntity.class);
        when(autoevaluacionAmbitoRepository.save(captor.capture())).thenReturn(guardado);

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}", procesoId)
                        .param("carreraId", carreraId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carreraId", is(carreraId.toString())))
                .andExpect(jsonPath("$.procesoId", is(procesoId.toString())));

        assertEquals(carreraId, captor.getValue().getCarreraId());
        assertEquals(procesoId, captor.getValue().getProcesoId());
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/autoevaluacion persiste vinculación con alias autoevaluacion (Admin bypass)")
    void deberiaGuardarVinculacionConAliasAutoevaluacion() throws Exception {
        authenticateUser("admin@undec.edu.ar", "ADMINISTRATOR", null);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(ambitoId);
        ambito.setDenominacion("Laboratorio Central");
        ambito.setTipoCategoria("INTERNO");

        when(ambitoRepository.findById(ambitoId)).thenReturn(Optional.of(ambito));

        AutoevaluacionCarreraAmbitoDto requestDto = new AutoevaluacionCarreraAmbitoDto(
                null,
                procesoId,
                carreraId,
                ambitoId,
                null,
                true,
                "Química [QUI-01]",
                "Dra. Sánchez",
                "Miércoles 14-18",
                "Ensayos",
                "Ninguna",
                null,
                null
        );

        when(autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdAndAmbitoId(procesoId, carreraId, ambitoId))
                .thenReturn(Optional.empty());

        AutoevaluacionCarreraAmbitoJpaEntity guardado = new AutoevaluacionCarreraAmbitoJpaEntity();
        guardado.setId(UUID.randomUUID());
        guardado.setProcesoId(procesoId);
        guardado.setCarreraId(carreraId);
        guardado.setAmbito(ambito);
        guardado.setUtilizadoPorCarrera(true);
        guardado.setCreadoEn(Instant.now());

        ArgumentCaptor<AutoevaluacionCarreraAmbitoJpaEntity> captor = ArgumentCaptor.forClass(AutoevaluacionCarreraAmbitoJpaEntity.class);
        when(autoevaluacionAmbitoRepository.save(captor.capture())).thenReturn(guardado);

        mockMvc.perform(post("/api/ambitos-practica/autoevaluacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carreraId", is(carreraId.toString())));

        assertEquals(carreraId, captor.getValue().getCarreraId());
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId} rechaza 403 cuando el usuario académico no pertenece a la UA de la carrera")
    void deberiaRechazarVinculacionCarreraDeOtraUnidadAcademica() throws Exception {
        UUID uaUser = UUID.randomUUID();
        UUID uaCarrera = UUID.randomUUID();
        authenticateUser("director@undec.edu.ar", "DIRECTOR_CARRERA", uaUser);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        UnidadAcademicaJpaEntity uaEntity = new UnidadAcademicaJpaEntity();
        uaEntity.setId(uaCarrera);

        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setId(carreraId);
        carrera.setUnidadAcademica(uaEntity);

        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        AutoevaluacionCarreraAmbitoDto requestDto = new AutoevaluacionCarreraAmbitoDto(
                null, procesoId, carreraId, ambitoId, null, true, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}", procesoId)
                        .param("carreraId", carreraId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CRIT-02: POST resuelve carreraId dinámicamente desde Proceso si carreraId es nulo y valida pertenencia UA")
    void deberiaResolverCarreraIdDinamicoDesdeProceso() throws Exception {
        UUID uaId = UUID.randomUUID();
        authenticateUser("director@undec.edu.ar", "DIRECTOR_CARRERA", uaId);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        ProcesoAcreditacionJpaEntity procesoEntity = new ProcesoAcreditacionJpaEntity();
        procesoEntity.setId(procesoId);
        procesoEntity.setCarreraId(carreraId);

        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(procesoEntity));

        UnidadAcademicaJpaEntity uaEntity = new UnidadAcademicaJpaEntity();
        uaEntity.setId(uaId);

        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setId(carreraId);
        carrera.setUnidadAcademica(uaEntity);

        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(ambitoId);
        when(ambitoRepository.findById(ambitoId)).thenReturn(Optional.of(ambito));

        AutoevaluacionCarreraAmbitoJpaEntity guardado = new AutoevaluacionCarreraAmbitoJpaEntity();
        guardado.setId(UUID.randomUUID());
        guardado.setProcesoId(procesoId);
        guardado.setCarreraId(carreraId);
        guardado.setAmbito(ambito);
        when(autoevaluacionAmbitoRepository.save(any())).thenReturn(guardado);

        AutoevaluacionCarreraAmbitoDto requestDto = new AutoevaluacionCarreraAmbitoDto(
                null, procesoId, null, ambitoId, null, true, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(procesoRepository).findById(procesoId);
        verify(carreraRepository).findById(carreraId);
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId}/completar rechaza con 422 cuando el plan tiene 0 asignaturas")
    void deberiaRechazarCompletarActividadCon422SiPlanTiene0Asignaturas() throws Exception {
        authenticateUser("admin@undec.edu.ar", "ADMINISTRATOR", null);
        UUID procesoId = UUID.randomUUID();

        doThrow(new DomainValidationException(
                "Precedencia requerida (CONEAU PRG-002): No es posible completar esta actividad hasta que se carguen las asignaturas en la Dimensión 8.3 (Plan de Estudio)."
        )).when(validarPrecedenciaAmbitosService).validar(eq(procesoId), isNull(), eq("INTERNOS_FT008"));

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}/completar", procesoId)
                        .param("actividad", "INTERNOS_FT008")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("Precedencia requerida (CONEAU PRG-002): No es posible completar esta actividad hasta que se carguen las asignaturas en la Dimensión 8.3 (Plan de Estudio).")));
    }

    @Test
    @DisplayName("POST /completar rechaza con 422 cuando no existe al menos 1 ámbito vinculado y utilizado de la categoría")
    void deberiaRechazarCompletarSinAmbitosVinculados() throws Exception {
        authenticateUser("admin@undec.edu.ar", "ADMINISTRATOR", null);
        UUID procesoId = UUID.randomUUID();

        doNothing().when(validarPrecedenciaAmbitosService).validar(eq(procesoId), isNull(), eq("INTERNOS_FT008"));
        when(autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}/completar", procesoId)
                        .param("actividad", "INTERNOS_FT008")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId}/completar completa exitosamente con 200 cuando el plan tiene asignaturas y ámbito vinculado")
    void deberiaCompletarActividadCon200CuandoPlanTieneAsignaturas() throws Exception {
        authenticateUser("admin@undec.edu.ar", "ADMINISTRATOR", null);
        UUID procesoId = UUID.randomUUID();

        doNothing().when(validarPrecedenciaAmbitosService).validar(eq(procesoId), isNull(), eq("INTERNOS_FT008"));

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(UUID.randomUUID());
        ambito.setTipoCategoria("INTERNO");

        AutoevaluacionCarreraAmbitoJpaEntity vinculo = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculo.setId(UUID.randomUUID());
        vinculo.setAmbito(ambito);
        vinculo.setUtilizadoPorCarrera(true);

        when(autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId))
                .thenReturn(List.of(vinculo));

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}/completar", procesoId)
                        .param("actividad", "INTERNOS_FT008")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completado", is(true)))
                .andExpect(jsonPath("$.actividad", is("INTERNOS_FT008")));
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/autoevaluacion/completar completa reenviando carreraId a servicio")
    void deberiaCompletarConAliasAutoevaluacionYCarreraId() throws Exception {
        authenticateUser("admin@undec.edu.ar", "ADMINISTRATOR", null);
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

        doNothing().when(validarPrecedenciaAmbitosService).validar(eq(procesoId), eq(carreraId), eq("EXTERNOS_FT009"));

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(UUID.randomUUID());
        ambito.setTipoCategoria("EXTERNO");

        AutoevaluacionCarreraAmbitoJpaEntity vinculo = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculo.setId(UUID.randomUUID());
        vinculo.setAmbito(ambito);
        vinculo.setUtilizadoPorCarrera(true);

        when(autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdOrderByAmbitoNumeroOrdenAsc(procesoId, carreraId))
                .thenReturn(List.of(vinculo));

        mockMvc.perform(post("/api/ambitos-practica/autoevaluacion/completar")
                        .param("procesoId", procesoId.toString())
                        .param("carreraId", carreraId.toString())
                        .param("actividad", "EXTERNOS_FT009")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completado", is(true)))
                .andExpect(jsonPath("$.actividad", is("EXTERNOS_FT009")));

        verify(validarPrecedenciaAmbitosService).validar(eq(procesoId), eq(carreraId), eq("EXTERNOS_FT009"));
    }
}
