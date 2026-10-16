package com.undec.acreditacion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;
import com.undec.acreditacion.infrastructure.web.dto.IniciarConvocatoriaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConvocatoriaControllerIntegrationTest {

    private MockMvc mockMvc;
    private IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase;
    private ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase;
    private ListarConvocatoriasUseCase listarConvocatoriasUseCase;
    private ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase;
    private RegistroConvocatoriaRepositoryPort registroRepository;
    private CarreraRepositoryPort carreraRepository;
    private DocumentoAcreditacionRepositoryPort documentoRepository;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        iniciarConvocatoriaUseCase = mock(IniciarConvocatoriaUseCase.class);
        consultarConvocatoriaUseCase = mock(ConsultarConvocatoriaUseCase.class);
        listarConvocatoriasUseCase = mock(ListarConvocatoriasUseCase.class);
        consultarPlanTrabajoUseCase = mock(ConsultarPlanTrabajoUseCase.class);
        registroRepository = mock(RegistroConvocatoriaRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        documentoRepository = mock(DocumentoAcreditacionRepositoryPort.class);
        userRepository = mock(UserRepository.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ConvocatoriaController controller = new ConvocatoriaController(
                iniciarConvocatoriaUseCase,
                consultarConvocatoriaUseCase,
                listarConvocatoriasUseCase,
                consultarPlanTrabajoUseCase,
                registroRepository,
                carreraRepository,
                documentoRepository,
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
                proceso.getId(), "RESFC-2026-145", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 11, 30), "Obs"
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
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 11, 30),
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
    @DisplayName("GET /api/convocatorias debe retornar 200 OK con listado")
    void debeListarConvocatorias() throws Exception {
        UUID carreraId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026-ISI-01", "Convocatoria ISI 2026",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );
        when(listarConvocatoriasUseCase.listar(any())).thenReturn(List.of(proceso));

        mockMvc.perform(get("/api/convocatorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("CONV-2026-ISI-01"));
    }

    @Test
    @DisplayName("GET /api/convocatorias/{id} debe retornar 200 OK con detalle")
    void debeObtenerConvocatoriaPorId() throws Exception {
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026-ISI-01", "Convocatoria ISI 2026",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, UUID.randomUUID()
        );

        when(consultarConvocatoriaUseCase.obtenerPorId(procesoId)).thenReturn(proceso);

        mockMvc.perform(get("/api/convocatorias/{id}", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CONV-2026-ISI-01"));
    }

    @Test
    @DisplayName("GET /api/convocatorias/{id}/plan-trabajo debe retornar 200 OK con lista de actividades")
    void debeObtenerPlanTrabajo() throws Exception {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        when(consultarPlanTrabajoUseCase.obtenerPorProcesoId(procesoId)).thenReturn(plan);

        mockMvc.perform(get("/api/convocatorias/{id}/plan-trabajo", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("PT-001-ISI-2026"))
                .andExpect(jsonPath("$.actividades[0].codigoActividad").value(ActividadPlanTrabajo.CODIGO_ACT_01));
    }
}
