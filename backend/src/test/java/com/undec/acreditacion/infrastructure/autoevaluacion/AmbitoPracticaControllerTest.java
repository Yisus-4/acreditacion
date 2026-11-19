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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Mock
    private SpringDataAutoevaluacionAmbitoActividadRepository autoevaluacionAmbitoActividadRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository actividadCurricularRepository;

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
                userRepository,
                autoevaluacionAmbitoActividadRepository,
                actividadCurricularRepository
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
        ambito.setEsMaestro(false);
        ambito.setProcesoId(procesoId);
        ambito.setActivo(true);

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
        authenticateUser("admin@undec.edu.ar", "AEA", null);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(ambitoId);
        ambito.setDenominacion("Hospital Escuela");
        ambito.setTipoCategoria("EXTERNO");
        ambito.setEsMaestro(false);
        ambito.setProcesoId(procesoId);
        ambito.setActivo(true);

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
        authenticateUser("admin@undec.edu.ar", "AEA", null);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(ambitoId);
        ambito.setDenominacion("Laboratorio Central");
        ambito.setTipoCategoria("INTERNO");
        ambito.setEsMaestro(false);
        ambito.setProcesoId(procesoId);
        ambito.setActivo(true);

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
        ambito.setEsMaestro(false);
        ambito.setProcesoId(procesoId);
        ambito.setActivo(true);
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
        authenticateUser("admin@undec.edu.ar", "AEA", null);
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
        authenticateUser("admin@undec.edu.ar", "AEA", null);
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
        authenticateUser("admin@undec.edu.ar", "AEA", null);
        UUID procesoId = UUID.randomUUID();

        doNothing().when(validarPrecedenciaAmbitosService).validar(eq(procesoId), isNull(), eq("INTERNOS_FT008"));

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(UUID.randomUUID());
        ambito.setTipoCategoria("INTERNO");
        ambito.setEsMaestro(false);
        ambito.setProcesoId(procesoId);
        ambito.setActivo(true);

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
        authenticateUser("admin@undec.edu.ar", "AEA", null);
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

        doNothing().when(validarPrecedenciaAmbitosService).validar(eq(procesoId), eq(carreraId), eq("EXTERNOS_FT009"));

        AmbitoPracticaJpaEntity ambito = new AmbitoPracticaJpaEntity();
        ambito.setId(UUID.randomUUID());
        ambito.setTipoCategoria("EXTERNO");
        ambito.setEsMaestro(false);
        ambito.setProcesoId(procesoId);
        ambito.setActivo(true);

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

    @Test
    @DisplayName("GET /api/ambitos-practica sin procesoId retorna únicamente ámbitos maestros")
    void deberiaListarSoloAmbitosMaestrosCuandoProcesoIdEsNulo() throws Exception {
        AmbitoPracticaJpaEntity maestro = new AmbitoPracticaJpaEntity();
        maestro.setId(UUID.randomUUID());
        maestro.setDenominacion("Laboratorio Central Maestro");
        maestro.setTipoCategoria("INTERNO");
        maestro.setEsMaestro(true);
        maestro.setActivo(true);

        when(ambitoRepository.findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc())
                .thenReturn(List.of(maestro));

        mockMvc.perform(get("/api/ambitos-practica")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].denominacion", is("Laboratorio Central Maestro")))
                .andExpect(jsonPath("$[0].esMaestro", is(true)));

        verify(ambitoRepository).findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc();
        verify(ambitoRepository, never()).findByProcesoIdAndActivoTrueOrderByNumeroOrdenAsc(any());
    }

    @Test
    @DisplayName("GET /api/ambitos-practica?procesoId={id} retorna únicamente copias de la carrera/proceso")
    void deberiaListarSoloAmbitosDeProcesoCuandoProcesoIdEstaPresente() throws Exception {
        UUID procesoId = UUID.randomUUID();
        AmbitoPracticaJpaEntity copia = new AmbitoPracticaJpaEntity();
        copia.setId(UUID.randomUUID());
        copia.setDenominacion("Laboratorio Topografía Carrera A");
        copia.setTipoCategoria("INTERNO");
        copia.setEsMaestro(false);
        copia.setProcesoId(procesoId);
        copia.setActivo(true);

        when(ambitoRepository.findByProcesoIdAndActivoTrueOrderByNumeroOrdenAsc(procesoId))
                .thenReturn(List.of(copia));

        mockMvc.perform(get("/api/ambitos-practica")
                        .param("procesoId", procesoId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].denominacion", is("Laboratorio Topografía Carrera A")))
                .andExpect(jsonPath("$[0].esMaestro", is(false)))
                .andExpect(jsonPath("$[0].procesoId", is(procesoId.toString())));

        verify(ambitoRepository).findByProcesoIdAndActivoTrueOrderByNumeroOrdenAsc(procesoId);
        verify(ambitoRepository, never()).findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc();
    }

    @Test
    @DisplayName("POST /api/ambitos-practica permite crear ámbito aislado para la carrera a DIRECTOR_CARRERA de su UA")
    void deberiaCrearCopiaDeProcesoParaDirectorCarreraDeMismaUA() throws Exception {
        UUID uaId = UUID.randomUUID();
        authenticateUser("director@undec.edu.ar", "DIRECTOR_CARRERA", uaId);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

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

        when(ambitoRepository.save(any(AmbitoPracticaJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AmbitoPracticaDto dto = new AmbitoPracticaDto(
                null, 1, "Taller Propio Agrimensura", "INTERNO", "Taller", null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, true, null, null,
                false, procesoId, null
        );

        mockMvc.perform(post("/api/ambitos-practica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion", is("Taller Propio Agrimensura")))
                .andExpect(jsonPath("$.esMaestro", is(false)))
                .andExpect(jsonPath("$.procesoId", is(procesoId.toString())));

        ArgumentCaptor<AmbitoPracticaJpaEntity> captor = ArgumentCaptor.forClass(AmbitoPracticaJpaEntity.class);
        verify(ambitoRepository).save(captor.capture());
        assertFalse(captor.getValue().getEsMaestro());
        assertEquals(procesoId, captor.getValue().getProcesoId());
        assertNull(captor.getValue().getOrigenMaestroId());
    }

    @Test
    @DisplayName("POST /api/ambitos-practica rechaza 403 cuando DIRECTOR_CARRERA intenta crear ámbito maestro")
    void deberiaRechazarCreacionMaestroPorDirectorCarrera() throws Exception {
        UUID uaId = UUID.randomUUID();
        authenticateUser("director@undec.edu.ar", "DIRECTOR_CARRERA", uaId);

        AmbitoPracticaDto dto = new AmbitoPracticaDto(
                null, 1, "Intento Maestro", "INTERNO", "Laboratorio", null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, true, null, null,
                true, null, null
        );

        mockMvc.perform(post("/api/ambitos-practica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verify(ambitoRepository, never()).save(any());
    }

    @Test
    @DisplayName("DELETE /api/ambitos-practica/{id} da de baja la copia de proceso y elimina sus vinculaciones")
    void deberiaEliminarCopiaDeProcesoYLimpiarVinculaciones() throws Exception {
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

        AmbitoPracticaJpaEntity copia = new AmbitoPracticaJpaEntity();
        copia.setId(ambitoId);
        copia.setDenominacion("Ambito de Carrera a Eliminar");
        copia.setTipoCategoria("INTERNO");
        copia.setEsMaestro(false);
        copia.setProcesoId(procesoId);
        copia.setActivo(true);
        when(ambitoRepository.findById(ambitoId)).thenReturn(Optional.of(copia));

        AutoevaluacionCarreraAmbitoJpaEntity vinculo = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculo.setId(UUID.randomUUID());
        vinculo.setProcesoId(procesoId);
        vinculo.setAmbito(copia);
        when(autoevaluacionAmbitoRepository.findByAmbitoId(ambitoId)).thenReturn(List.of(vinculo));

        mockMvc.perform(delete("/api/ambitos-practica/{id}", ambitoId))
                .andExpect(status().isNoContent());

        assertFalse(copia.getActivo());
        verify(ambitoRepository).save(copia);
        verify(autoevaluacionAmbitoRepository).deleteAll(List.of(vinculo));
    }

    @Test
    @DisplayName("GET /api/ambitos-practica/autoevaluacion filtra vinculaciones huérfanas que apuntan a ámbitos maestros")
    void deberiaFiltrarAmbitosMaestrosAlListarAutoevaluacion() throws Exception {
        UUID procesoId = UUID.randomUUID();

        AmbitoPracticaJpaEntity maestro = new AmbitoPracticaJpaEntity();
        maestro.setId(UUID.randomUUID());
        maestro.setDenominacion("Gabinete de Enfermería");
        maestro.setTipoCategoria("INTERNO");
        maestro.setEsMaestro(true);
        maestro.setProcesoId(null);
        maestro.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity vinculoHuerfano = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculoHuerfano.setId(UUID.randomUUID());
        vinculoHuerfano.setProcesoId(procesoId);
        vinculoHuerfano.setAmbito(maestro);
        vinculoHuerfano.setUtilizadoPorCarrera(true);
        vinculoHuerfano.setAsignaturasVinculadas("analisis 1");

        AmbitoPracticaJpaEntity clon = new AmbitoPracticaJpaEntity();
        clon.setId(UUID.randomUUID());
        clon.setDenominacion("Campo Experimental Clonado");
        clon.setTipoCategoria("UNIDAD_DEMOSTRATIVA");
        clon.setEsMaestro(false);
        clon.setProcesoId(procesoId);
        clon.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity vinculoClon = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculoClon.setId(UUID.randomUUID());
        vinculoClon.setProcesoId(procesoId);
        vinculoClon.setAmbito(clon);
        vinculoClon.setUtilizadoPorCarrera(true);

        when(autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId))
                .thenReturn(List.of(vinculoHuerfano, vinculoClon));

        mockMvc.perform(get("/api/ambitos-practica/autoevaluacion")
                        .param("procesoId", procesoId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ambito.denominacion", is("Campo Experimental Clonado")));
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId} persiste vínculos relacionales por UUID y guarda solo manuales en texto")
    void deberiaPersistirYRetornarVinculosRelacionalesConNombreVivoYManuales() throws Exception {
        authenticateUser("admin@undec.edu.ar", "AEA", null);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();
        UUID actId1 = UUID.randomUUID();
        UUID actId2 = UUID.randomUUID();

        AmbitoPracticaJpaEntity clon = new AmbitoPracticaJpaEntity();
        clon.setId(ambitoId);
        clon.setDenominacion("Campo Experimental (200 ha)");
        clon.setTipoCategoria("UNIDAD_DEMOSTRATIVA");
        clon.setEsMaestro(false);
        clon.setProcesoId(procesoId);
        clon.setActivo(true);
        when(ambitoRepository.findById(ambitoId)).thenReturn(Optional.of(clon));

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(UUID.randomUUID());
        plan.setProcesoId(procesoId);

        ActividadCurricularPlanJpaEntity act1 = new ActividadCurricularPlanJpaEntity();
        act1.setId(actId1);
        act1.setPlanEstudio(plan);
        act1.setNumeroOrden(1);
        act1.setCodigo("101");
        act1.setNombre("Análisis 1");

        ActividadCurricularPlanJpaEntity act2 = new ActividadCurricularPlanJpaEntity();
        act2.setId(actId2);
        act2.setPlanEstudio(plan);
        act2.setNumeroOrden(2);
        act2.setCodigo("202");
        act2.setNombre("Edafología");

        when(actividadCurricularRepository.findAllById(any()))
                .thenReturn(List.of(act1, act2));
        when(autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdAndAmbitoId(procesoId, carreraId, ambitoId))
                .thenReturn(Optional.empty());
        when(autoevaluacionAmbitoRepository.save(any(AutoevaluacionCarreraAmbitoJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(autoevaluacionAmbitoActividadRepository.findByAutoevaluacionAmbitoId(any()))
                .thenReturn(Collections.emptyList());
        when(autoevaluacionAmbitoActividadRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AutoevaluacionCarreraAmbitoDto requestDto = new AutoevaluacionCarreraAmbitoDto(
                null,
                procesoId,
                carreraId,
                ambitoId,
                null,
                true,
                "Texto Viejo Ignorado",
                "Ing. Gómez",
                null,
                "Prácticas",
                null,
                null,
                null
        );
        requestDto.setActividadCurricularIds(List.of(actId1, actId2));
        requestDto.setAsignaturasManuales(List.of("Práctica Libre"));

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}", procesoId)
                        .param("carreraId", carreraId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividadCurricularIds", hasSize(2)))
                .andExpect(jsonPath("$.actividadCurricularIds[0]", is(actId1.toString())))
                .andExpect(jsonPath("$.actividadCurricularIds[1]", is(actId2.toString())))
                .andExpect(jsonPath("$.actividadesVinculadasDetalle", hasSize(2)))
                .andExpect(jsonPath("$.actividadesVinculadasDetalle[0].nombre", is("Análisis 1")))
                .andExpect(jsonPath("$.actividadesVinculadasDetalle[0].codigo", is("101")))
                .andExpect(jsonPath("$.asignaturasManuales", hasSize(1)))
                .andExpect(jsonPath("$.asignaturasManuales[0]", is("Práctica Libre")))
                .andExpect(jsonPath("$.asignaturasVinculadas", is("Análisis 1 [101]; Edafología [202]; Práctica Libre")));

        ArgumentCaptor<AutoevaluacionCarreraAmbitoJpaEntity> captor =
                ArgumentCaptor.forClass(AutoevaluacionCarreraAmbitoJpaEntity.class);
        verify(autoevaluacionAmbitoRepository).save(captor.capture());
        assertEquals("Práctica Libre", captor.getValue().getAsignaturasVinculadas());
    }

    @Test
    @DisplayName("POST /api/ambitos-practica/carrera/{procesoId} rechaza con 422 vincular directamente un ámbito maestro")
    void deberiaRechazarCon422VincularAmbitoMaestro() throws Exception {
        authenticateUser("admin@undec.edu.ar", "AEA", null);

        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoMaestroId = UUID.randomUUID();

        AmbitoPracticaJpaEntity maestro = new AmbitoPracticaJpaEntity();
        maestro.setId(ambitoMaestroId);
        maestro.setDenominacion("Gabinete Maestro");
        maestro.setTipoCategoria("INTERNO");
        maestro.setEsMaestro(true);
        maestro.setProcesoId(null);
        when(ambitoRepository.findById(ambitoMaestroId)).thenReturn(Optional.of(maestro));

        AutoevaluacionCarreraAmbitoDto requestDto = new AutoevaluacionCarreraAmbitoDto(
                null, procesoId, carreraId, ambitoMaestroId, null, true,
                "Física", null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/ambitos-practica/carrera/{procesoId}", procesoId)
                        .param("carreraId", carreraId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnprocessableEntity());

        verify(autoevaluacionAmbitoRepository, never()).save(any());
    }
}
