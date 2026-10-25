package com.undec.acreditacion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;
import com.undec.acreditacion.infrastructure.web.dto.ElaborarPlanTrabajoRequest;
import com.undec.acreditacion.infrastructure.web.dto.GuardarRg001Request;
import com.undec.acreditacion.infrastructure.web.dto.IniciarConvocatoriaRequest;
import com.undec.acreditacion.infrastructure.web.dto.ItemActividadPlanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConvocatoriaControllerIntegrationTest {

    private MockMvc mockMvc;
    private IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase;
    private IniciarConvocatoriaEscuelaUseCase iniciarConvocatoriaEscuelaUseCase;
    private ConfirmarConvocatoriaDirectorEscuelaUseCase confirmarConvocatoriaDirectorEscuelaUseCase;
    private ConsultarConvocatoriaEscuelaUseCase consultarConvocatoriaEscuelaUseCase;
    private com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort convocatoriaEscuelaRepository;
    private ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase;
    private ListarConvocatoriasUseCase listarConvocatoriasUseCase;
    private EliminarConvocatoriaUseCase eliminarConvocatoriaUseCase;
    private ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase;
    private ElaborarPlanTrabajoUseCase elaborarPlanTrabajoUseCase;
    private ObtenerPlantillaPlanTrabajoUseCase obtenerPlantillaPlanTrabajoUseCase;
    private RegistroConvocatoriaRepositoryPort registroRepository;
    private CarreraRepositoryPort carreraRepository;
    private DocumentoAcreditacionRepositoryPort documentoRepository;
    private com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort planTrabajoRepository;
    private com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort procesoRepository;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        iniciarConvocatoriaUseCase = mock(IniciarConvocatoriaUseCase.class);
        iniciarConvocatoriaEscuelaUseCase = mock(IniciarConvocatoriaEscuelaUseCase.class);
        confirmarConvocatoriaDirectorEscuelaUseCase = mock(ConfirmarConvocatoriaDirectorEscuelaUseCase.class);
        consultarConvocatoriaEscuelaUseCase = mock(ConsultarConvocatoriaEscuelaUseCase.class);
        convocatoriaEscuelaRepository = mock(com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort.class);
        consultarConvocatoriaUseCase = mock(ConsultarConvocatoriaUseCase.class);
        listarConvocatoriasUseCase = mock(ListarConvocatoriasUseCase.class);
        eliminarConvocatoriaUseCase = mock(EliminarConvocatoriaUseCase.class);
        consultarPlanTrabajoUseCase = mock(ConsultarPlanTrabajoUseCase.class);
        elaborarPlanTrabajoUseCase = mock(ElaborarPlanTrabajoUseCase.class);
        obtenerPlantillaPlanTrabajoUseCase = mock(ObtenerPlantillaPlanTrabajoUseCase.class);
        registroRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        documentoRepository = mock(DocumentoAcreditacionRepositoryPort.class);
        unidadAcademicaRepository = mock(com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort.class);
        planTrabajoRepository = mock(com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort.class);
        procesoRepository = mock(com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort.class);
        userRepository = mock(UserRepository.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ConvocatoriaController controller = new ConvocatoriaController(
                iniciarConvocatoriaUseCase,
                iniciarConvocatoriaEscuelaUseCase,
                confirmarConvocatoriaDirectorEscuelaUseCase,
                consultarConvocatoriaEscuelaUseCase,
                convocatoriaEscuelaRepository,
                consultarConvocatoriaUseCase,
                listarConvocatoriasUseCase,
                eliminarConvocatoriaUseCase,
                consultarPlanTrabajoUseCase,
                elaborarPlanTrabajoUseCase,
                obtenerPlantillaPlanTrabajoUseCase,
                registroRepository,
                carreraRepository,
                documentoRepository,
                unidadAcademicaRepository,
                planTrabajoRepository,
                procesoRepository,
                userRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ConvocatoriaExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/convocatorias debe responder 201 Created con detalle de la convocatoria")
    void debeIniciarConvocatoriaRetornando201() throws Exception {
        UUID carreraId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();

        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026-ISI-01", "Convocatoria ISI 2026",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        RegistroConvocatoria reg = RegistroConvocatoria.crear(
                proceso.getId(), "RESFC-2026-145", LocalDate.now(), LocalDate.now().plusMonths(6), "Obs"
        );
        PlanTrabajo plan = PlanTrabajo.inicializar(proceso.getId(), "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        when(iniciarConvocatoriaUseCase.iniciar(any())).thenReturn(proceso);
        when(registroRepository.findByProcesoId(proceso.getId())).thenReturn(Optional.of(reg));
        when(consultarPlanTrabajoUseCase.obtenerPorProcesoId(proceso.getId())).thenReturn(plan);

        Carrera carrera = Carrera.rehydrate(
                carreraId, UUID.randomUUID(), "Ingeniería en Sistemas de Información", "ISI", "GRADO",
                "Computación", ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO,
                "Ingeniero", 5, "Desc", true, null, null, Collections.emptyList()
        );
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        IniciarConvocatoriaRequest request = new IniciarConvocatoriaRequest(
                carreraId,
                "RESFC-2026-145",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "Obs"
        );

        mockMvc.perform(post("/api/convocatorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("CONV-2026-ISI-01"))
                .andExpect(jsonPath("$.carreraNombre").value("Ingeniería en Sistemas de Información"))
                .andExpect(jsonPath("$.estado").value("INICIADO"))
                .andExpect(jsonPath("$.registroConvocatoria.nroResolucionConeau").value("RESFC-2026-145"))
                .andExpect(jsonPath("$.planTrabajo.codigo").value("PT-001-ISI-2026"));
    }

    @Test
    @DisplayName("GET /api/convocatorias debe retornar 200 OK con listado de escuelas convocadas")
    void debeListarConvocatorias() throws Exception {
        UUID escuelaId = UUID.randomUUID();
        ConvocatoriaCarreraItemDto carreraItem = new ConvocatoriaCarreraItemDto(
                UUID.randomUUID(), UUID.randomUUID(), "Ingeniería en Sistemas de Información", "ISI",
                "PRESENCIAL", "GRADO", "Computación", true, true, true, UUID.randomUUID(), "INICIADO"
        );
        ConvocatoriaEscuelaDetalleDto escuelaDetalle = new ConvocatoriaEscuelaDetalleDto(
                UUID.randomUUID(), escuelaId, "Escuela de Ingeniería", "EI",
                "CONV-2026-EI-01", "Convocatoria Escuela de Ingeniería",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, "RESFC-2026-APN",
                LocalDate.now(), LocalDate.now().plusMonths(6), EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA,
                "Obs", UUID.randomUUID(), null, null,
                Instant.now(), Instant.now(), List.of(carreraItem)
        );
        when(consultarConvocatoriaEscuelaUseCase.listarTodas()).thenReturn(List.of(escuelaDetalle));

        mockMvc.perform(get("/api/convocatorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("CONV-2026-EI-01"))
                .andExpect(jsonPath("$[0].escuelaNombre").value("Escuela de Ingeniería"));
    }

    @Test
    @DisplayName("GET /api/convocatorias debe filtrar convocatorias por escuela si el usuario es Director de Escuela")
    void debeFiltrarConvocatoriasPorEscuelaSiEsDirectorDeEscuela() throws Exception {
        UUID escuelaIngenieriaId = UUID.randomUUID();
        UUID escuelaSocialesId = UUID.randomUUID();

        ConvocatoriaEscuelaDetalleDto convEI = new ConvocatoriaEscuelaDetalleDto(
                UUID.randomUUID(), escuelaIngenieriaId, "Escuela de Ingeniería", "EI",
                "CONV-2026-EI-01", "Convocatoria EI", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-EI", LocalDate.now(), LocalDate.now().plusMonths(6), EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA,
                "Obs", UUID.randomUUID(), null, null, Instant.now(), Instant.now(), Collections.emptyList()
        );
        ConvocatoriaEscuelaDetalleDto convSoc = new ConvocatoriaEscuelaDetalleDto(
                UUID.randomUUID(), escuelaSocialesId, "Escuela de Ciencias Sociales", "ECSoc",
                "CONV-2026-CS-01", "Convocatoria Sociales", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-SOC", LocalDate.now(), LocalDate.now().plusMonths(6), EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA,
                "Obs", UUID.randomUUID(), null, null, Instant.now(), Instant.now(), Collections.emptyList()
        );

        when(consultarConvocatoriaEscuelaUseCase.listarTodas()).thenReturn(List.of(convEI, convSoc));

        // Mock director de Escuela de Ingeniería
        User directorEI = User.rehydrate(
                UUID.randomUUID(), "director_ei", "de_ei@undec.edu.ar", "hash",
                true, false, escuelaIngenieriaId,
                java.util.Set.of(Role.custom(UUID.randomUUID(), "SCHOOL_DIRECTOR", "Director Escuela", "DE"))
        );

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn("de_ei@undec.edu.ar");
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByEmail("de_ei@undec.edu.ar")).thenReturn(Optional.of(directorEI));

        try {
            mockMvc.perform(get("/api/convocatorias"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].codigo").value("CONV-2026-EI-01"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("GET /api/convocatorias/{id} debe retornar 403 Forbidden si Director de Escuela accede a otra escuela")
    void debeRetornar403SiDirectorEscuelaAccedeAOtraEscuela() throws Exception {
        UUID escuelaIngenieriaId = UUID.randomUUID();
        UUID escuelaSocialesId = UUID.randomUUID();
        UUID convSocialesId = UUID.randomUUID();

        ConvocatoriaEscuelaDetalleDto convSoc = new ConvocatoriaEscuelaDetalleDto(
                convSocialesId, escuelaSocialesId, "Escuela de Ciencias Sociales", "ECSoc",
                "CONV-2026-CS-01", "Convocatoria Sociales", TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                "RES-SOC", LocalDate.now(), LocalDate.now().plusMonths(6), EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA,
                "Obs", UUID.randomUUID(), null, null, Instant.now(), Instant.now(), Collections.emptyList()
        );

        when(convocatoriaEscuelaRepository.findById(convSocialesId))
                .thenReturn(Optional.of(mock(Convocatoria.class)));
        when(consultarConvocatoriaEscuelaUseCase.obtenerPorId(convSocialesId)).thenReturn(convSoc);

        User directorEI = User.rehydrate(
                UUID.randomUUID(), "director_ei", "de_ei@undec.edu.ar", "hash",
                true, false, escuelaIngenieriaId,
                java.util.Set.of(Role.custom(UUID.randomUUID(), "SCHOOL_DIRECTOR", "Director Escuela", "DE"))
        );

        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn("de_ei@undec.edu.ar");
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByEmail("de_ei@undec.edu.ar")).thenReturn(Optional.of(directorEI));

        try {
            mockMvc.perform(get("/api/convocatorias/" + convSocialesId))
                    .andExpect(status().isForbidden());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("GET /api/convocatorias/{id} y /api/v1/procesos-acreditacion/{id} deben retornar 200 OK con detalle enriquecido y actividades")
    void debeObtenerConvocatoriaPorId() throws Exception {
        UUID carreraId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026-ISI-01", "Convocatoria ISI 2026",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        UUID procesoId = proceso.getId();

        RegistroConvocatoria reg = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-145", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 11, 30),
                "Observaciones", "RM 1254/2018", FaseEvaluacion.PRIMERA_FASE, LocalDate.of(2026, 12, 1)
        );
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        when(consultarConvocatoriaUseCase.obtenerPorId(procesoId)).thenReturn(proceso);
        when(registroRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(reg));
        when(consultarPlanTrabajoUseCase.obtenerPorProcesoId(procesoId)).thenReturn(plan);

        // Test con ruta /api/convocatorias/{id}
        mockMvc.perform(get("/api/convocatorias/{id}", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CONV-2026-ISI-01"))
                .andExpect(jsonPath("$.registroConvocatoria.rmEstandares").value("RM 1254/2018"))
                .andExpect(jsonPath("$.registroConvocatoria.faseEvaluacion").value("PRIMERA_FASE"))
                .andExpect(jsonPath("$.actividades[0].codigoActividad").value(ActividadPlanTrabajo.CODIGO_ACT_01))
                .andExpect(jsonPath("$.actividades[0].hoja").value("INFO"))
                .andExpect(jsonPath("$.actividades[0].prioridad").value("MEDIA"));

        // Test con ruta /api/v1/procesos-acreditacion/{id}
        mockMvc.perform(get("/api/v1/procesos-acreditacion/{id}", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CONV-2026-ISI-01"))
                .andExpect(jsonPath("$.registroConvocatoria.rmEstandares").value("RM 1254/2018"));
    }

    @Test
    @DisplayName("GET /api/v1/procesos-acreditacion/{id}/plan-trabajo/plantilla y alias /plantilla-plan deben retornar plantilla sugerida")
    void debeObtenerPlantillaPlanTrabajo() throws Exception {
        UUID procesoId = UUID.randomUUID();
        PlantillaActividadDto actInfo = new PlantillaActividadDto(
                3, "ACT-03-DOC", "Relevamiento cuerpo docente y CVs",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director Carrera"
        );
        PlantillaActividadDto actConeau = new PlantillaActividadDto(
                4, "ACT-04-FICHAS", "Carga de fichas docentes",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Equipo Docente / AEA"
        );

        PlantillaPlanTrabajoDto plantilla = new PlantillaPlanTrabajoDto(
                TipoPlanTrabajo.PT_001_ACREDITACION,
                List.of(actInfo),
                List.of(actConeau)
        );

        when(obtenerPlantillaPlanTrabajoUseCase.obtenerPorProcesoId(procesoId)).thenReturn(plantilla);

        // Ruta canónica
        mockMvc.perform(get("/api/v1/procesos-acreditacion/{id}/plan-trabajo/plantilla", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPlan").value("PT_001_ACREDITACION"))
                .andExpect(jsonPath("$.actividadesInfo[0].nombre").value("Relevamiento cuerpo docente y CVs"))
                .andExpect(jsonPath("$.actividadesConeau[0].nombre").value("Carga de fichas docentes"));

        // Alias canónico /plantilla-plan
        mockMvc.perform(get("/api/convocatorias/{id}/plantilla-plan", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPlan").value("PT_001_ACREDITACION"))
                .andExpect(jsonPath("$.actividadesInfo[0].nombre").value("Relevamiento cuerpo docente y CVs"));
    }

    @Test
    @DisplayName("POST /api/v1/procesos-acreditacion/{id}/plan-trabajo/elaborar y alias /elaborar-plan deben procesar y retornar plan elaborado")
    void debeElaborarPlanTrabajoExitosamente() throws Exception {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        when(elaborarPlanTrabajoUseCase.elaborar(any(ElaborarPlanTrabajoCommand.class))).thenReturn(plan);

        ElaborarPlanTrabajoRequest request = new ElaborarPlanTrabajoRequest(
                "RM 1254/2018",
                FaseEvaluacion.PRIMERA_FASE,
                LocalDate.of(2026, 12, 10),
                List.of(
                        new ItemActividadPlanRequest(
                                3,
                                "ACT-03-DOC",
                                "Relevamiento cuerpo docente y CVs",
                                HojaPlanTrabajo.INFO,
                                LocalDate.of(2026, 4, 1),
                                LocalDate.of(2026, 5, 1),
                                "Director Carrera",
                                PrioridadActividad.ALTA
                        )
                )
        );

        // Ruta canónica
        mockMvc.perform(post("/api/v1/procesos-acreditacion/{id}/plan-trabajo/elaborar", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("PT-001-ISI-2026"));

        // Alias /elaborar-plan con payload JSON usando alias fechaLimitePresentacionConeau
        String jsonConAlias = """
                {
                  "rmEstandares": "RM 1254/2018",
                  "faseEvaluacion": "PRIMERA_FASE",
                  "fechaLimitePresentacionConeau": "2026-12-10",
                  "actividades": []
                }
                """;
        mockMvc.perform(post("/api/convocatorias/{id}/elaborar-plan", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConAlias))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("PT-001-ISI-2026"));
    }

    @Test
    @DisplayName("POST /api/v1/procesos-acreditacion/{id}/plan-trabajo/elaborar debe responder 400 Bad Request si validación falla")
    void debeResponder400SiValidacionFalla() throws Exception {
        UUID procesoId = UUID.randomUUID();

        when(elaborarPlanTrabajoUseCase.elaborar(any(ElaborarPlanTrabajoCommand.class)))
                .thenThrow(new DomainValidationException("La fecha límite CONEAU no puede ser anterior a la fecha de cierre"));

        ElaborarPlanTrabajoRequest request = new ElaborarPlanTrabajoRequest(
                "RM 1254/2018",
                FaseEvaluacion.PRIMERA_FASE,
                LocalDate.of(2026, 1, 1),
                Collections.emptyList()
        );

        mockMvc.perform(post("/api/v1/procesos-acreditacion/{id}/plan-trabajo/elaborar", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha límite CONEAU no puede ser anterior a la fecha de cierre"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{id}/rg001/borrador debe guardar metadatos en borrador")
    void debeGuardarRg001Borrador() throws Exception {
        UUID procesoId = UUID.randomUUID();
        RegistroConvocatoria reg = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-APN", LocalDate.now(), LocalDate.now().plusMonths(6), "Obs"
        );
        when(registroRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(reg));
        when(registroRepository.save(any(RegistroConvocatoria.class))).thenAnswer(i -> i.getArgument(0));

        GuardarRg001Request request = new GuardarRg001Request(
                "RESFC-2026-APN", LocalDate.now(), LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(6), "RM 1254/18", "Informática",
                "PRESENCIAL", "EN_FUNCIONAMIENTO", "PRIMERA_FASE", "Obs actualizadas"
        );

        mockMvc.perform(post("/api/convocatorias/{id}/rg001/borrador", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRg001").value("BORRADOR"))
                .andExpect(jsonPath("$.rmEstandares").value("RM 1254/18"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{id}/rg001/enviar debe marcar RG-001 como ENVIADO")
    void debeEnviarRg001() throws Exception {
        UUID procesoId = UUID.randomUUID();
        RegistroConvocatoria reg = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-APN", LocalDate.now(), LocalDate.now().plusMonths(6), "Obs"
        );
        when(registroRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(reg));
        when(registroRepository.save(any(RegistroConvocatoria.class))).thenAnswer(i -> i.getArgument(0));

        GuardarRg001Request request = new GuardarRg001Request(
                "RESFC-2026-APN", LocalDate.now(), LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(6), "RM 1254/18", "Informática",
                "PRESENCIAL", "EN_FUNCIONAMIENTO", "PRIMERA_FASE", "Obs actualizadas"
        );

        mockMvc.perform(post("/api/convocatorias/{id}/rg001/enviar", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRg001").value("ENVIADO"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{id}/plan-trabajo/finalizar-actividad debe responder 400 si RG-001 o Plan no están enviados")
    void debeFallarFinalizarActividadSiNoEstanEnviadosAmbos() throws Exception {
        UUID procesoId = UUID.randomUUID();
        RegistroConvocatoria reg = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-APN", LocalDate.now(), LocalDate.now().plusMonths(6), "Obs"
        );
        reg.marcarComoBorrador();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-TEST", TipoPlanTrabajo.PT_001_ACREDITACION);
        plan.marcarComoBorrador();

        when(registroRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(reg));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        mockMvc.perform(post("/api/convocatorias/{id}/plan-trabajo/finalizar-actividad", procesoId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede finalizar la actividad: el formulario RG-001 debe ser enviado previamente."));
    }

    @Test
    @DisplayName("DELETE /api/convocatorias/{id} debe eliminar la convocatoria y retornar 204 No Content")
    void debeEliminarConvocatoriaRetornando204() throws Exception {
        UUID procesoId = UUID.randomUUID();
        doNothing().when(eliminarConvocatoriaUseCase).ejecutar(procesoId);

        mockMvc.perform(delete("/api/convocatorias/{id}", procesoId))
                .andExpect(status().isNoContent());

        verify(eliminarConvocatoriaUseCase).ejecutar(procesoId);
    }
}
