package com.undec.acreditacion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.EstadoRegistroCa;
import com.undec.acreditacion.domain.entities.RolClaustroCa;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.infrastructure.web.dto.GuardarRegistroCaBorradorRequest;
import com.undec.acreditacion.infrastructure.web.dto.ObservarRegistroCaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ComisionAutoevaluacionControllerIntegrationTest {

    private MockMvc mockMvc;
    private ConsultarRegistroCaUseCase consultarUseCase;
    private SolicitarConformacionCaUseCase solicitarUseCase;
    private GuardarRegistroCaBorradorUseCase guardarBorradorUseCase;
    private RemitirRegistroCaUseCase remitirUseCase;
    private AprobarRegistroCaUseCase aprobarUseCase;
    private ObservarRegistroCaUseCase observarUseCase;
    private ConfirmarRegistroMiembroCaUseCase confirmarRegistroUseCase;
    private CancelarRegistroMiembroCaUseCase cancelarRegistroUseCase;
    private DocumentStoragePort storagePort;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    private UUID procesoId;
    private UUID miembroId;

    @BeforeEach
    void setUp() {
        consultarUseCase = mock(ConsultarRegistroCaUseCase.class);
        solicitarUseCase = mock(SolicitarConformacionCaUseCase.class);
        guardarBorradorUseCase = mock(GuardarRegistroCaBorradorUseCase.class);
        remitirUseCase = mock(RemitirRegistroCaUseCase.class);
        aprobarUseCase = mock(AprobarRegistroCaUseCase.class);
        observarUseCase = mock(ObservarRegistroCaUseCase.class);
        confirmarRegistroUseCase = mock(ConfirmarRegistroMiembroCaUseCase.class);
        cancelarRegistroUseCase = mock(CancelarRegistroMiembroCaUseCase.class);
        storagePort = mock(DocumentStoragePort.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper();

        ComisionAutoevaluacionController controller = new ComisionAutoevaluacionController(
                consultarUseCase, solicitarUseCase, guardarBorradorUseCase,
                remitirUseCase, aprobarUseCase, observarUseCase,
                confirmarRegistroUseCase, cancelarRegistroUseCase,
                storagePort, userRepository, objectMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ConvocatoriaExceptionHandler())
                .build();

        procesoId = UUID.randomUUID();
        miembroId = UUID.randomUUID();
    }

    private RegistroCaDto crearDtoEjemplo(EstadoRegistroCa estado) {
        return new RegistroCaDto(
                UUID.randomUUID(), procesoId, estado, false,
                Instant.now(), UUID.randomUUID(), LocalDate.now().plusDays(15), 15L, "Faltan 15 días",
                null, null, null, null, null,
                null, null, null, null, Instant.now(), Instant.now(),
                List.of(new RegistroCaDto.MiembroCaDto(
                        miembroId, UUID.randomUUID(), "Perez", "Juan", null, "Juan Perez",
                        "12345678", "12345678", "20-12345678-9", "juan@undec.edu.ar",
                        "juan@gmail.com", "Docente", "123", RolClaustroCa.COORDINADOR,
                        true, true, true, false, 1
                ))
        );
    }

    @Test
    @DisplayName("GET /api/convocatorias/{procesoId}/comision-autoevaluacion retorna 200 con DTO")
    void consultarRegistro() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.PENDIENTE_SOLICITUD);
        when(consultarUseCase.consultarPorProcesoId(procesoId)).thenReturn(dto);

        mockMvc.perform(get("/api/convocatorias/{procesoId}/comision-autoevaluacion", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_SOLICITUD"))
                .andExpect(jsonPath("$.miembros[0].apellido").value("Perez"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{procesoId}/comision-autoevaluacion/solicitar inicia proceso")
    void solicitarConformacion() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.SOLICITADA_A_DC);
        when(solicitarUseCase.solicitar(eq(procesoId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/convocatorias/{procesoId}/comision-autoevaluacion/solicitar", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SOLICITADA_A_DC"))
                .andExpect(jsonPath("$.diasRestantes").value(15));
    }

    @Test
    @DisplayName("GET /api/convocatorias/{procesoId}/comision-autoevaluacion/instructivo-it001 descarga PDF")
    void descargarInstructivo() throws Exception {
        mockMvc.perform(get("/api/convocatorias/{procesoId}/comision-autoevaluacion/instructivo-it001", procesoId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("IT-001")));
    }

    @Test
    @DisplayName("PUT /api/convocatorias/{procesoId}/comision-autoevaluacion/borrador guarda borrador")
    void guardarBorrador() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.EN_CARGA_BORRADOR);
        when(guardarBorradorUseCase.guardarBorrador(eq(procesoId), eq(true), any())).thenReturn(dto);

        GuardarRegistroCaBorradorRequest req = new GuardarRegistroCaBorradorRequest(
                true, List.of(new MiembroCaInputDto(
                null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                null, RolClaustroCa.COORDINADOR, true, true, true, 1
        )));

        mockMvc.perform(put("/api/convocatorias/{procesoId}/comision-autoevaluacion/borrador", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CARGA_BORRADOR"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{procesoId}/comision-autoevaluacion/remitir envía con resolución adjunta")
    void remitirConResolucion() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.REMITIDA_A_AEA);
        when(remitirUseCase.remitir(eq(procesoId), any(), eq(false), any(), any(), any())).thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile("resolucion", "resolucion.pdf", "application/pdf", "PDF dummy".getBytes());

        mockMvc.perform(multipart("/api/convocatorias/{procesoId}/comision-autoevaluacion/remitir", procesoId)
                        .file(file)
                        .param("coincideCurricular", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REMITIDA_A_AEA"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{procesoId}/comision-autoevaluacion/aprobar aprueba registro")
    void aprobar() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.APROBADA);
        when(aprobarUseCase.aprobar(eq(procesoId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/convocatorias/{procesoId}/comision-autoevaluacion/aprobar", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{procesoId}/comision-autoevaluacion/observar observa registro")
    void observar() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.OBSERVADA);
        when(observarUseCase.observar(eq(procesoId), any(), eq("Faltan miembros"))).thenReturn(dto);

        ObservarRegistroCaRequest req = new ObservarRegistroCaRequest("Faltan miembros");

        mockMvc.perform(post("/api/convocatorias/{procesoId}/comision-autoevaluacion/observar", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OBSERVADA"));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{procesoId}/comision-autoevaluacion/miembros/{miembroId}/confirmar-registro confirma registro en catálogo")
    void confirmarRegistroMiembro() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.EN_CARGA_BORRADOR);
        when(confirmarRegistroUseCase.confirmarRegistro(eq(procesoId), eq(miembroId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/convocatorias/{procesoId}/comision-autoevaluacion/miembros/{miembroId}/confirmar-registro", procesoId, miembroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros[0].pendienteAltaSistema").value(false));
    }

    @Test
    @DisplayName("POST /api/convocatorias/{procesoId}/comision-autoevaluacion/miembros/{miembroId}/cancelar-registro cancela alta en catálogo")
    void cancelarRegistroMiembro() throws Exception {
        RegistroCaDto dto = crearDtoEjemplo(EstadoRegistroCa.EN_CARGA_BORRADOR);
        when(cancelarRegistroUseCase.cancelarRegistro(eq(procesoId), eq(miembroId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/convocatorias/{procesoId}/comision-autoevaluacion/miembros/{miembroId}/cancelar-registro", procesoId, miembroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros[0].pendienteAltaSistema").value(false));
    }
}
