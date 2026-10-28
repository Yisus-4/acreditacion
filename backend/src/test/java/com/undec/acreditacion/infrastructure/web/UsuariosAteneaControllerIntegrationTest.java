package com.undec.acreditacion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.ConsultarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.input.FinalizarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.input.GuardarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.input.GuardarUsuarioPermisoAteneaInputDto;
import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.EstadoRegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.GrupoUsuarioAtenea;
import com.undec.acreditacion.domain.entities.TipoAccesoAtenea;
import com.undec.acreditacion.domain.entities.TipoPermisoMatrizAtenea;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.infrastructure.web.dto.GuardarUsuariosAteneaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

class UsuariosAteneaControllerIntegrationTest {

    private MockMvc mockMvc;
    private ConsultarRegistroUsuariosAteneaUseCase consultarUseCase;
    private GuardarRegistroUsuariosAteneaUseCase guardarUseCase;
    private FinalizarRegistroUsuariosAteneaUseCase finalizarUseCase;
    private DocumentStoragePort storagePort;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    private UUID procesoId;

    @BeforeEach
    void setUp() {
        consultarUseCase = mock(ConsultarRegistroUsuariosAteneaUseCase.class);
        guardarUseCase = mock(GuardarRegistroUsuariosAteneaUseCase.class);
        finalizarUseCase = mock(FinalizarRegistroUsuariosAteneaUseCase.class);
        storagePort = mock(DocumentStoragePort.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper();

        UsuariosAteneaController controller = new UsuariosAteneaController(
                consultarUseCase, guardarUseCase, finalizarUseCase, storagePort, userRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        procesoId = UUID.randomUUID();
    }

    private RegistroUsuariosAteneaDto crearDtoEjemplo(EstadoRegistroUsuariosAtenea estado) {
        return new RegistroUsuariosAteneaDto(
                UUID.randomUUID(),
                procesoId,
                estado,
                LocalDate.now().plusDays(2),
                2,
                "Dentro del plazo (2 días hábiles restantes)",
                estado == EstadoRegistroUsuariosAtenea.FINALIZADO ? Instant.now() : null,
                estado == EstadoRegistroUsuariosAtenea.FINALIZADO ? Instant.now() : null,
                UUID.randomUUID(),
                estado == EstadoRegistroUsuariosAtenea.FINALIZADO ? "minio/doc.pdf" : null,
                estado == EstadoRegistroUsuariosAtenea.FINALIZADO,
                List.of(new RegistroUsuariosAteneaDto.UsuarioPermisoAteneaDto(
                        UUID.randomUUID(),
                        GrupoUsuarioAtenea.AEA,
                        "Valeria",
                        "Castro",
                        "Castro, Valeria",
                        "27-32111222-3",
                        "vcastro@undec.edu.ar",
                        "Área de Evaluación y Acreditación (AEA)",
                        "Equipo Técnico AEA",
                        TipoAccesoAtenea.ADMINISTRACION,
                        "Integral ATENEA",
                        false,
                        true,
                        true
                )),
                List.of(new RegistroUsuariosAteneaDto.ItemMatrizPermisoAteneaDto(
                        UUID.randomUUID(),
                        "1. Infraestructura y equipamiento para enseñanza y aprendizaje",
                        "Todos",
                        "Todos",
                        TipoPermisoMatrizAtenea.LECTURA,
                        "Lectura",
                        true,
                        true,
                        1
                ))
        );
    }

    @Test
    @DisplayName("GET /api/procesos/{procesoId}/usuarios-atenea debe retornar registro 200 OK con usuarios y matriz")
    void debeConsultarRegistroUsuariosAtenea() throws Exception {
        RegistroUsuariosAteneaDto dto = crearDtoEjemplo(EstadoRegistroUsuariosAtenea.EN_DEFINICION);
        when(consultarUseCase.consultarPorProcesoId(procesoId)).thenReturn(dto);

        mockMvc.perform(get("/api/procesos/{procesoId}/usuarios-atenea", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesoId").value(procesoId.toString()))
                .andExpect(jsonPath("$.estado").value("EN_DEFINICION"))
                .andExpect(jsonPath("$.usuarios[0].nombre").value("Valeria"))
                .andExpect(jsonPath("$.usuarios[0].grupoUsuario").value("AEA"))
                .andExpect(jsonPath("$.matrizPermisos[0].item").value("Todos"))
                .andExpect(jsonPath("$.matrizPermisos[0].tipoPermiso").value("LECTURA"));
    }

    @Test
    @DisplayName("PUT /api/procesos/{procesoId}/usuarios-atenea debe guardar cambios y retornar 200 OK")
    void debeGuardarUsuariosAtenea() throws Exception {
        RegistroUsuariosAteneaDto dto = crearDtoEjemplo(EstadoRegistroUsuariosAtenea.EN_DEFINICION);
        when(guardarUseCase.guardar(eq(procesoId), any(), any(), any())).thenReturn(dto);

        GuardarUsuariosAteneaRequest req = new GuardarUsuariosAteneaRequest(
                List.of(new GuardarUsuarioPermisoAteneaInputDto(
                        UUID.randomUUID(), GrupoUsuarioAtenea.AEA, "Valeria", "Castro",
                        "27-32111222-3", "vcastro@undec.edu.ar", "AEA", "Técnica",
                        TipoAccesoAtenea.ADMINISTRACION, "Formulario", false, true, true
                )),
                null
        );

        mockMvc.perform(put("/api/procesos/{procesoId}/usuarios-atenea", procesoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarios[0].apellido").value("Castro"));
    }

    @Test
    @DisplayName("POST /api/procesos/{procesoId}/usuarios-atenea/finalizar debe finalizar y retornar 200 OK")
    void debeFinalizarRegistroUsuariosAtenea() throws Exception {
        RegistroUsuariosAteneaDto dto = crearDtoEjemplo(EstadoRegistroUsuariosAtenea.FINALIZADO);
        when(finalizarUseCase.finalizar(eq(procesoId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/procesos/{procesoId}/usuarios-atenea/finalizar", procesoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.finalizado").value(true));
    }

    @Test
    @DisplayName("GET /api/procesos/{procesoId}/usuarios-atenea/descargar debe retornar PDF 200 OK")
    void debeDescargarPdf() throws Exception {
        RegistroUsuariosAteneaDto dto = crearDtoEjemplo(EstadoRegistroUsuariosAtenea.FINALIZADO);
        when(consultarUseCase.consultarPorProcesoId(procesoId)).thenReturn(dto);
        byte[] fakePdf = "%PDF-1.4 fake content".getBytes();
        when(storagePort.downloadFile("minio/doc.pdf")).thenReturn(fakePdf);

        mockMvc.perform(get("/api/procesos/{procesoId}/usuarios-atenea/descargar", procesoId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"doc.pdf\""))
                .andExpect(content().bytes(fakePdf));
    }
}
