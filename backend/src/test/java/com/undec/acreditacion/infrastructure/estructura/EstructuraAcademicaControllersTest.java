package com.undec.acreditacion.infrastructure.estructura;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.ActualizarCarreraCommand;
import com.undec.acreditacion.application.input.ActualizarCarreraUseCase;
import com.undec.acreditacion.application.input.ActualizarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.ActualizarSedeLocalizacionUseCase;
import com.undec.acreditacion.application.input.ActualizarUnidadAcademicaCommand;
import com.undec.acreditacion.application.input.ActualizarUnidadAcademicaUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoCarreraUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoSedeLocalizacionUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoUnidadAcademicaUseCase;
import com.undec.acreditacion.application.input.EliminarCarreraUseCase;
import com.undec.acreditacion.application.input.EliminarSedeLocalizacionUseCase;
import com.undec.acreditacion.application.input.EliminarUnidadAcademicaUseCase;
import com.undec.acreditacion.application.input.ListarCarrerasUseCase;
import com.undec.acreditacion.application.input.ListarSedesLocalizacionesUseCase;
import com.undec.acreditacion.application.input.ListarUnidadesAcademicasUseCase;
import com.undec.acreditacion.application.input.RegistrarCarreraCommand;
import com.undec.acreditacion.application.input.RegistrarCarreraUseCase;
import com.undec.acreditacion.application.input.RegistrarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.RegistrarSedeLocalizacionUseCase;
import com.undec.acreditacion.application.input.RegistrarUnidadAcademicaCommand;
import com.undec.acreditacion.application.input.RegistrarUnidadAcademicaUseCase;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.EstadoCarrera;
import com.undec.acreditacion.domain.entities.ModalidadCarrera;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.estructura.dto.ActualizarCarreraRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.ActualizarSedeLocalizacionRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.ActualizarUnidadAcademicaRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.CambiarEstadoRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.RegistrarCarreraRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.RegistrarSedeLocalizacionRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.RegistrarUnidadAcademicaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstructuraAcademicaControllersTest {

    private ObjectMapper objectMapper;

    private RegistrarUnidadAcademicaUseCase registrarUnidadAcademicaUseCase;
    private ListarUnidadesAcademicasUseCase listarUnidadesAcademicasUseCase;
    private ActualizarUnidadAcademicaUseCase actualizarUnidadAcademicaUseCase;
    private CambiarEstadoUnidadAcademicaUseCase cambiarEstadoUnidadAcademicaUseCase;
    private EliminarUnidadAcademicaUseCase eliminarUnidadAcademicaUseCase;
    private MockMvc mockMvcUa;

    private RegistrarSedeLocalizacionUseCase registrarSedeLocalizacionUseCase;
    private ListarSedesLocalizacionesUseCase listarSedesLocalizacionesUseCase;
    private ActualizarSedeLocalizacionUseCase actualizarSedeLocalizacionUseCase;
    private CambiarEstadoSedeLocalizacionUseCase cambiarEstadoSedeLocalizacionUseCase;
    private EliminarSedeLocalizacionUseCase eliminarSedeLocalizacionUseCase;
    private MockMvc mockMvcSede;

    private RegistrarCarreraUseCase registrarCarreraUseCase;
    private ListarCarrerasUseCase listarCarrerasUseCase;
    private ActualizarCarreraUseCase actualizarCarreraUseCase;
    private CambiarEstadoCarreraUseCase cambiarEstadoCarreraUseCase;
    private EliminarCarreraUseCase eliminarCarreraUseCase;
    private MockMvc mockMvcCarrera;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Unidad Academica
        registrarUnidadAcademicaUseCase = mock(RegistrarUnidadAcademicaUseCase.class);
        listarUnidadesAcademicasUseCase = mock(ListarUnidadesAcademicasUseCase.class);
        actualizarUnidadAcademicaUseCase = mock(ActualizarUnidadAcademicaUseCase.class);
        cambiarEstadoUnidadAcademicaUseCase = mock(CambiarEstadoUnidadAcademicaUseCase.class);
        eliminarUnidadAcademicaUseCase = mock(EliminarUnidadAcademicaUseCase.class);
        UnidadAcademicaController uaController = new UnidadAcademicaController(
                registrarUnidadAcademicaUseCase,
                listarUnidadesAcademicasUseCase,
                actualizarUnidadAcademicaUseCase,
                cambiarEstadoUnidadAcademicaUseCase,
                eliminarUnidadAcademicaUseCase
        );
        mockMvcUa = MockMvcBuilders.standaloneSetup(uaController)
                .setControllerAdvice(new EstructuraAcademicaExceptionHandler())
                .build();

        // Sede Localizacion
        registrarSedeLocalizacionUseCase = mock(RegistrarSedeLocalizacionUseCase.class);
        listarSedesLocalizacionesUseCase = mock(ListarSedesLocalizacionesUseCase.class);
        actualizarSedeLocalizacionUseCase = mock(ActualizarSedeLocalizacionUseCase.class);
        cambiarEstadoSedeLocalizacionUseCase = mock(CambiarEstadoSedeLocalizacionUseCase.class);
        eliminarSedeLocalizacionUseCase = mock(EliminarSedeLocalizacionUseCase.class);
        SedeLocalizacionController sedeController = new SedeLocalizacionController(
                registrarSedeLocalizacionUseCase,
                listarSedesLocalizacionesUseCase,
                actualizarSedeLocalizacionUseCase,
                cambiarEstadoSedeLocalizacionUseCase,
                eliminarSedeLocalizacionUseCase
        );
        mockMvcSede = MockMvcBuilders.standaloneSetup(sedeController)
                .setControllerAdvice(new EstructuraAcademicaExceptionHandler())
                .build();

        // Carrera
        registrarCarreraUseCase = mock(RegistrarCarreraUseCase.class);
        listarCarrerasUseCase = mock(ListarCarrerasUseCase.class);
        actualizarCarreraUseCase = mock(ActualizarCarreraUseCase.class);
        cambiarEstadoCarreraUseCase = mock(CambiarEstadoCarreraUseCase.class);
        eliminarCarreraUseCase = mock(EliminarCarreraUseCase.class);
        CarreraController carreraController = new CarreraController(
                registrarCarreraUseCase,
                listarCarrerasUseCase,
                actualizarCarreraUseCase,
                cambiarEstadoCarreraUseCase,
                eliminarCarreraUseCase
        );
        mockMvcCarrera = MockMvcBuilders.standaloneSetup(carreraController)
                .setControllerAdvice(new EstructuraAcademicaExceptionHandler())
                .build();
    }

    // ──────────────────────────── Unidad Academica Endpoints ────────────────────────────

    @Test
    @DisplayName("GET /api/unidades-academicas debe retornar listado 200 OK")
    void listarUnidadesAcademicas() throws Exception {
        UUID instId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(instId, "Escuela de Ingeniería", "EI", "Desc");
        when(listarUnidadesAcademicasUseCase.listarTodas()).thenReturn(List.of(ua));

        mockMvcUa.perform(get("/api/unidades-academicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Escuela de Ingeniería"))
                .andExpect(jsonPath("$[0].sigla").value("EI"));
    }

    @Test
    @DisplayName("POST /api/unidades-academicas debe crear unidad académica y retornar 201 Created")
    void registrarUnidadAcademica() throws Exception {
        UUID instId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(instId, "Escuela de Ingeniería", "EI", "Desc");
        when(registrarUnidadAcademicaUseCase.registrar(any(RegistrarUnidadAcademicaCommand.class))).thenReturn(ua);

        RegistrarUnidadAcademicaRequest req = new RegistrarUnidadAcademicaRequest(instId, "Escuela de Ingeniería", "EI", "Desc");

        mockMvcUa.perform(post("/api/unidades-academicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Escuela de Ingeniería"));
    }

    @Test
    @DisplayName("PUT /api/unidades-academicas/{id} debe actualizar unidad académica y retornar 200 OK")
    void actualizarUnidadAcademica() throws Exception {
        UUID uaId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(UUID.randomUUID(), "Nuevo Nombre", "NN", "Nueva desc");
        when(actualizarUnidadAcademicaUseCase.actualizar(any(ActualizarUnidadAcademicaCommand.class))).thenReturn(ua);

        ActualizarUnidadAcademicaRequest req = new ActualizarUnidadAcademicaRequest("Nuevo Nombre", "NN", "Nueva desc");

        mockMvcUa.perform(put("/api/unidades-academicas/" + uaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo Nombre"))
                .andExpect(jsonPath("$.sigla").value("NN"));
    }

    @Test
    @DisplayName("PATCH /api/unidades-academicas/{id}/estado debe cambiar estado y retornar 200 OK")
    void cambiarEstadoUnidadAcademica() throws Exception {
        UUID uaId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(UUID.randomUUID(), "Escuela", "E", "Desc");
        ua.cambiarEstado(false);
        when(cambiarEstadoUnidadAcademicaUseCase.cambiarEstado(eq(uaId), eq(false))).thenReturn(ua);

        CambiarEstadoRequest req = new CambiarEstadoRequest(false);

        mockMvcUa.perform(patch("/api/unidades-academicas/" + uaId + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("DELETE /api/unidades-academicas/{id} debe eliminar y retornar 204 No Content")
    void eliminarUnidadAcademica() throws Exception {
        UUID uaId = UUID.randomUUID();
        doNothing().when(eliminarUnidadAcademicaUseCase).eliminar(uaId);

        mockMvcUa.perform(delete("/api/unidades-academicas/" + uaId))
                .andExpect(status().isNoContent());

        verify(eliminarUnidadAcademicaUseCase).eliminar(uaId);
    }

    @Test
    @DisplayName("DELETE /api/unidades-academicas/{id} con carreras asociadas retorna 400 Bad Request")
    void eliminarUnidadAcademicaConCarrerasFalla() throws Exception {
        UUID uaId = UUID.randomUUID();
        doThrow(new DomainValidationException("No se puede eliminar la unidad académica porque tiene carreras asociadas"))
                .when(eliminarUnidadAcademicaUseCase).eliminar(uaId);

        mockMvcUa.perform(delete("/api/unidades-academicas/" + uaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede eliminar la unidad académica porque tiene carreras asociadas"));
    }

    // ──────────────────────────── Sede Localizacion Endpoints ────────────────────────────

    @Test
    @DisplayName("GET /api/sedes-localizaciones debe retornar listado 200 OK")
    void listarSedes() throws Exception {
        UUID instId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                instId, "Campus Los Sarmientos", "Ruta Los Peregrinos", "s/n", null, "5361", 1L, 1L, 1L, true
        );
        when(listarSedesLocalizacionesUseCase.listarTodas()).thenReturn(List.of(sede));

        mockMvcSede.perform(get("/api/sedes-localizaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Campus Los Sarmientos"));
    }

    @Test
    @DisplayName("POST /api/sedes-localizaciones debe crear sede y retornar 201 Created")
    void registrarSede() throws Exception {
        UUID instId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                instId, "Campus Los Sarmientos", "Ruta Los Peregrinos", "s/n", null, "5361", 1L, 1L, 1L, true
        );
        when(registrarSedeLocalizacionUseCase.registrar(any(RegistrarSedeLocalizacionCommand.class))).thenReturn(sede);

        RegistrarSedeLocalizacionRequest req = new RegistrarSedeLocalizacionRequest(
                instId, "Campus Los Sarmientos", "Ruta Los Peregrinos", "s/n", null, "5361", 1L, 1L, 1L, true
        );

        mockMvcSede.perform(post("/api/sedes-localizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Campus Los Sarmientos"));
    }

    @Test
    @DisplayName("PUT /api/sedes-localizaciones/{id} debe actualizar sede y retornar 200 OK")
    void actualizarSede() throws Exception {
        UUID sedeId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                UUID.randomUUID(), "Campus Actualizado", "Calle 1", "10", null, "5360", 1L, 1L, 1L, false
        );
        when(actualizarSedeLocalizacionUseCase.actualizar(any(ActualizarSedeLocalizacionCommand.class))).thenReturn(sede);

        ActualizarSedeLocalizacionRequest req = new ActualizarSedeLocalizacionRequest(
                "Campus Actualizado", "Calle 1", "10", null, "5360", 1L, 1L, 1L, false
        );

        mockMvcSede.perform(put("/api/sedes-localizaciones/" + sedeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Campus Actualizado"))
                .andExpect(jsonPath("$.esPropia").value(false));
    }

    @Test
    @DisplayName("PATCH /api/sedes-localizaciones/{id}/estado debe cambiar estado y retornar 200 OK")
    void cambiarEstadoSede() throws Exception {
        UUID sedeId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                UUID.randomUUID(), "Sede", "Calle", "1", null, "5360", 1L, 1L, 1L, true
        );
        sede.cambiarEstado(false);
        when(cambiarEstadoSedeLocalizacionUseCase.cambiarEstado(eq(sedeId), eq(false))).thenReturn(sede);

        CambiarEstadoRequest req = new CambiarEstadoRequest(false);

        mockMvcSede.perform(patch("/api/sedes-localizaciones/" + sedeId + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("DELETE /api/sedes-localizaciones/{id} debe eliminar y retornar 204 No Content")
    void eliminarSede() throws Exception {
        UUID sedeId = UUID.randomUUID();
        doNothing().when(eliminarSedeLocalizacionUseCase).eliminar(sedeId);

        mockMvcSede.perform(delete("/api/sedes-localizaciones/" + sedeId))
                .andExpect(status().isNoContent());

        verify(eliminarSedeLocalizacionUseCase).eliminar(sedeId);
    }

    @Test
    @DisplayName("DELETE /api/sedes-localizaciones/{id} asociada a carrera retorna 400 Bad Request")
    void eliminarSedeConCarreraFalla() throws Exception {
        UUID sedeId = UUID.randomUUID();
        doThrow(new DomainValidationException("No se puede eliminar la sede porque está asignada a una o más carreras"))
                .when(eliminarSedeLocalizacionUseCase).eliminar(sedeId);

        mockMvcSede.perform(delete("/api/sedes-localizaciones/" + sedeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede eliminar la sede porque está asignada a una o más carreras"));
    }

    // ──────────────────────────── Carrera Endpoints ────────────────────────────

    @Test
    @DisplayName("GET /api/carreras debe retornar listado 200 OK")
    void listarCarreras() throws Exception {
        UUID uaId = UUID.randomUUID();
        Carrera carrera = Carrera.registrar(
                uaId, "Licenciatura en Sistemas", "LSI", "Ciencias Aplicadas",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Licenciado/a", 4, "Desc", List.of()
        );
        when(listarCarrerasUseCase.listarTodas()).thenReturn(List.of(carrera));

        mockMvcCarrera.perform(get("/api/carreras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Licenciatura en Sistemas"))
                .andExpect(jsonPath("$[0].nivel").value("GRADO"));
    }

    @Test
    @DisplayName("POST /api/carreras debe crear carrera y retornar 201 Created")
    void registrarCarrera() throws Exception {
        UUID uaId = UUID.randomUUID();
        Carrera carrera = Carrera.registrar(
                uaId, "Licenciatura en Sistemas", "LSI", "Ciencias Aplicadas",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Licenciado/a", 4, "Desc", List.of()
        );
        when(registrarCarreraUseCase.registrar(any(RegistrarCarreraCommand.class))).thenReturn(carrera);

        RegistrarCarreraRequest req = new RegistrarCarreraRequest(
                uaId, "Licenciatura en Sistemas", "LSI", "Ciencias Aplicadas",
                "PRESENCIAL", "EN_FUNCIONAMIENTO", "Licenciado/a", 4, "Desc", List.of()
        );

        mockMvcCarrera.perform(post("/api/carreras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Licenciatura en Sistemas"))
                .andExpect(jsonPath("$.nivel").value("GRADO"));
    }

    @Test
    @DisplayName("PUT /api/carreras/{id} debe actualizar carrera y retornar 200 OK")
    void actualizarCarrera() throws Exception {
        UUID carreraId = UUID.randomUUID();
        Carrera carrera = Carrera.registrar(
                UUID.randomUUID(), "Ingeniería en Computación", "IC", "Informática",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero/a", 5, "Desc", List.of()
        );
        when(actualizarCarreraUseCase.actualizar(any(ActualizarCarreraCommand.class))).thenReturn(carrera);

        ActualizarCarreraRequest req = new ActualizarCarreraRequest(
                "Ingeniería en Computación", "IC", "Informática",
                "PRESENCIAL", "EN_FUNCIONAMIENTO", "Ingeniero/a", 5, "Desc", true, List.of()
        );

        mockMvcCarrera.perform(put("/api/carreras/" + carreraId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ingeniería en Computación"))
                .andExpect(jsonPath("$.nivel").value("GRADO"));
    }

    @Test
    @DisplayName("PATCH /api/carreras/{id}/estado debe cambiar estado y retornar 200 OK")
    void cambiarEstadoCarrera() throws Exception {
        UUID carreraId = UUID.randomUUID();
        Carrera carrera = Carrera.registrar(
                UUID.randomUUID(), "Carrera", "C", "Area",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Titulo", 5, "Desc", List.of()
        );
        carrera.cambiarEstado(false);
        when(cambiarEstadoCarreraUseCase.cambiarEstado(eq(carreraId), eq(false))).thenReturn(carrera);

        CambiarEstadoRequest req = new CambiarEstadoRequest(false);

        mockMvcCarrera.perform(patch("/api/carreras/" + carreraId + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("DELETE /api/carreras/{id} debe eliminar carrera y retornar 204 No Content")
    void eliminarCarrera() throws Exception {
        UUID carreraId = UUID.randomUUID();
        doNothing().when(eliminarCarreraUseCase).eliminar(carreraId);

        mockMvcCarrera.perform(delete("/api/carreras/" + carreraId))
                .andExpect(status().isNoContent());

        verify(eliminarCarreraUseCase).eliminar(carreraId);
    }
}
