package com.undec.acreditacion.infrastructure.institucion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.ActualizarInstitucionUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoInstitucionUseCase;
import com.undec.acreditacion.application.input.ConsultarInstitucionUseCase;
import com.undec.acreditacion.application.input.EliminarInstitucionUseCase;
import com.undec.acreditacion.application.input.ListarInstitucionesUseCase;
import com.undec.acreditacion.application.input.RegistrarInstitucionCommand;
import com.undec.acreditacion.application.input.RegistrarInstitucionUseCase;
import com.undec.acreditacion.domain.entities.AutoridadInstitucional;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.entities.SedeCentral;
import com.undec.acreditacion.domain.exceptions.ArchivoInvalidoException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.InstitucionDuplicadaException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstitucionControllerTest {

    private MockMvc mockMvc;
    private RegistrarInstitucionUseCase registrarInstitucionUseCase;
    private ListarInstitucionesUseCase listarInstitucionesUseCase;
    private ConsultarInstitucionUseCase consultarInstitucionUseCase;
    private ActualizarInstitucionUseCase actualizarInstitucionUseCase;
    private CambiarEstadoInstitucionUseCase cambiarEstadoInstitucionUseCase;
    private EliminarInstitucionUseCase eliminarInstitucionUseCase;
    private DocumentStoragePort documentStoragePort;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        registrarInstitucionUseCase = mock(RegistrarInstitucionUseCase.class);
        listarInstitucionesUseCase = mock(ListarInstitucionesUseCase.class);
        consultarInstitucionUseCase = mock(ConsultarInstitucionUseCase.class);
        actualizarInstitucionUseCase = mock(ActualizarInstitucionUseCase.class);
        cambiarEstadoInstitucionUseCase = mock(CambiarEstadoInstitucionUseCase.class);
        eliminarInstitucionUseCase = mock(EliminarInstitucionUseCase.class);
        documentStoragePort = mock(DocumentStoragePort.class);
        objectMapper = new ObjectMapper();

        InstitucionController controller = new InstitucionController(
                registrarInstitucionUseCase,
                listarInstitucionesUseCase,
                consultarInstitucionUseCase,
                actualizarInstitucionUseCase,
                cambiarEstadoInstitucionUseCase,
                eliminarInstitucionUseCase,
                documentStoragePort,
                objectMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new InstitucionExceptionHandler())
                .build();
    }

    private Institucion buildSampleInstitucion() {
        AutoridadInstitucional maxAut = AutoridadInstitucional.maximaAutoridad(
                "Pérez", "Juan", null, "+543825111111", "rector@undec.edu.ar");
        AutoridadInstitucional adminAut = AutoridadInstitucional.administradorInstitucional(
                "Gómez", "María", null, "+543825222222", "admin@undec.edu.ar", "Secretario", "Rectorado");
        SedeCentral sede = SedeCentral.crear(
                "9 de Julio", "22", null, null, "5360", 1L, 1L, 1L);
        return Institucion.registrar(
                "Universidad Nacional de Chilecito",
                "UNdeC",
                "logos/undec.png",
                maxAut,
                adminAut,
                sede
        );
    }

    @Test
    @DisplayName("POST /api/instituciones multipart exitoso retorna 201 Created")
    void registrarInstitucionMultipartRetorna201() throws Exception {
        Institucion institucion = buildSampleInstitucion();
        when(registrarInstitucionUseCase.registrar(any(RegistrarInstitucionCommand.class)))
                .thenReturn(institucion);
        when(documentStoragePort.getFileUrl("logos/undec.png"))
                .thenReturn("http://localhost:9000/instituciones-logos/logos/undec.png");

        String datosJson = """
                {
                    "nombre": "Universidad Nacional de Chilecito",
                    "sigla": "UNdeC",
                    "maximaAutoridad": {
                        "apellido": "Pérez",
                        "primerNombre": "Juan",
                        "telefono": "+543825111111",
                        "email": "rector@undec.edu.ar"
                    },
                    "administradorInstitucional": {
                        "apellido": "Gómez",
                        "primerNombre": "María",
                        "telefono": "+543825222222",
                        "email": "admin@undec.edu.ar",
                        "cargo": "Secretario",
                        "ambito": "Rectorado"
                    },
                    "sedeCentral": {
                        "calle": "9 de Julio",
                        "numero": "22",
                        "codigoPostal": "5360",
                        "provinciaId": 1,
                        "departamentoId": 1,
                        "localidadId": 1
                    }
                }
                """;

        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", "application/json", datosJson.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile(
                "logotipo", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/instituciones")
                        .file(datosPart)
                        .file(logoPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Universidad Nacional de Chilecito"))
                .andExpect(jsonPath("$.sigla").value("UNdeC"))
                .andExpect(jsonPath("$.logoPath").value("logos/undec.png"))
                .andExpect(jsonPath("$.logoUrl").value("http://localhost:9000/instituciones-logos/logos/undec.png"))
                .andExpect(jsonPath("$.maximaAutoridad.apellido").value("Pérez"))
                .andExpect(jsonPath("$.administradorInstitucional.apellido").value("Gómez"))
                .andExpect(jsonPath("$.sedeCentral.calle").value("9 de Julio"));
    }

    @Test
    @DisplayName("POST /api/instituciones con institución duplicada retorna 409 Conflict")
    void registrarInstitucionDuplicadaRetorna409() throws Exception {
        when(registrarInstitucionUseCase.registrar(any(RegistrarInstitucionCommand.class)))
                .thenThrow(new InstitucionDuplicadaException("La institución ingresada ya se encuentra registrada en el sistema"));

        String datosJson = """
                {
                    "nombre": "Universidad Nacional de Chilecito",
                    "sigla": "UNdeC"
                }
                """;
        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", "application/json", datosJson.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile(
                "logotipo", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/instituciones")
                        .file(datosPart)
                        .file(logoPart))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La institución ingresada ya se encuentra registrada en el sistema"));
    }

    @Test
    @DisplayName("POST /api/instituciones con violación de integridad retorna 409 Conflict")
    void registrarInstitucionDataIntegrityViolationRetorna409() throws Exception {
        when(registrarInstitucionUseCase.registrar(any(RegistrarInstitucionCommand.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        String datosJson = """
                {
                    "nombre": "Universidad Nacional de Chilecito",
                    "sigla": "UNdeC"
                }
                """;
        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", "application/json", datosJson.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile(
                "logotipo", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/instituciones")
                        .file(datosPart)
                        .file(logoPart))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto de integridad de datos: ya existe una institución con datos duplicados"));
    }

    @Test
    @DisplayName("POST /api/instituciones con formato de archivo inválido retorna 400 Bad Request")
    void registrarInstitucionArchivoInvalidoRetorna400() throws Exception {
        when(registrarInstitucionUseCase.registrar(any(RegistrarInstitucionCommand.class)))
                .thenThrow(new ArchivoInvalidoException("Debe adjuntar un formato de imagen válido (PNG o JPG)"));

        String datosJson = """
                {
                    "nombre": "Universidad Test",
                    "sigla": "UTEST"
                }
                """;
        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", "application/json", datosJson.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile(
                "logotipo", "documento.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/instituciones")
                        .file(datosPart)
                        .file(logoPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe adjuntar un formato de imagen válido (PNG o JPG)"));
    }

    @Test
    @DisplayName("POST /api/instituciones con datos faltantes retorna 400 Bad Request")
    void registrarInstitucionDatosFaltantesRetorna400() throws Exception {
        when(registrarInstitucionUseCase.registrar(any(RegistrarInstitucionCommand.class)))
                .thenThrow(new DomainValidationException("El nombre de la institución es obligatorio"));

        String datosJson = "{}";
        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", "application/json", datosJson.getBytes());

        mockMvc.perform(multipart("/api/instituciones")
                        .file(datosPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El nombre de la institución es obligatorio"));
    }

    @Test
    @DisplayName("GET /api/instituciones retorna lista con 200 OK")
    void listarInstitucionesRetorna200() throws Exception {
        Institucion institucion = buildSampleInstitucion();
        when(listarInstitucionesUseCase.listar()).thenReturn(List.of(institucion));
        when(documentStoragePort.getFileUrl("logos/undec.png"))
                .thenReturn("http://localhost:9000/instituciones-logos/logos/undec.png");

        mockMvc.perform(get("/api/instituciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Universidad Nacional de Chilecito"))
                .andExpect(jsonPath("$[0].sigla").value("UNdeC"))
                .andExpect(jsonPath("$[0].logoUrl").value("http://localhost:9000/instituciones-logos/logos/undec.png"));
    }

    @Test
    @DisplayName("GET /api/instituciones/{id} retorna institución con 200 OK")
    void obtenerPorIdRetorna200() throws Exception {
        UUID id = UUID.randomUUID();
        Institucion institucion = buildSampleInstitucion();
        when(consultarInstitucionUseCase.consultarPorId(id)).thenReturn(institucion);
        when(documentStoragePort.getFileUrl("logos/undec.png"))
                .thenReturn("http://localhost:9000/instituciones-logos/logos/undec.png");

        mockMvc.perform(get("/api/instituciones/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Universidad Nacional de Chilecito"))
                .andExpect(jsonPath("$.sigla").value("UNdeC"));
    }

    @Test
    @DisplayName("GET /api/instituciones/{id} no encontrada retorna 404 Not Found")
    void obtenerPorIdInexistenteRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarInstitucionUseCase.consultarPorId(id))
                .thenThrow(new ResourceNotFoundException("Institución no encontrada: " + id));

        mockMvc.perform(get("/api/instituciones/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Institución no encontrada: " + id));
    }

    @Test
    @DisplayName("PUT /api/instituciones/{id} multipart actualiza y retorna 200 OK")
    void actualizarMultipartRetorna200() throws Exception {
        UUID id = UUID.randomUUID();
        Institucion actualizada = buildSampleInstitucion();
        when(actualizarInstitucionUseCase.actualizar(eq(id), any(RegistrarInstitucionCommand.class)))
                .thenReturn(actualizada);

        String datosJson = """
                {
                    "nombre": "Universidad Nacional de Chilecito",
                    "sigla": "UNdeC",
                    "maximaAutoridad": {
                        "apellido": "Pérez",
                        "primerNombre": "Juan",
                        "telefono": "+543825111111",
                        "email": "rector@undec.edu.ar"
                    },
                    "administradorInstitucional": {
                        "apellido": "Gómez",
                        "primerNombre": "María",
                        "telefono": "+543825222222",
                        "email": "admin@undec.edu.ar",
                        "cargo": "Secretario",
                        "ambito": "Rectorado"
                    },
                    "sedeCentral": {
                        "calle": "9 de Julio",
                        "numero": "22",
                        "codigoPostal": "5360",
                        "provinciaId": 1,
                        "departamentoId": 1,
                        "localidadId": 1
                    }
                }
                """;

        MockMultipartFile datosPart = new MockMultipartFile(
                "datos", "", "application/json", datosJson.getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/instituciones/{id}", id)
                        .file(datosPart)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Universidad Nacional de Chilecito"));
    }

    @Test
    @DisplayName("PUT /api/instituciones/{id} JSON actualiza y retorna 200 OK")
    void actualizarJsonRetorna200() throws Exception {
        UUID id = UUID.randomUUID();
        Institucion actualizada = buildSampleInstitucion();
        when(actualizarInstitucionUseCase.actualizar(eq(id), any(RegistrarInstitucionCommand.class)))
                .thenReturn(actualizada);

        String jsonBody = """
                {
                    "nombre": "Universidad Nacional de Chilecito",
                    "sigla": "UNdeC",
                    "maximaAutoridad": {
                        "apellido": "Pérez",
                        "primerNombre": "Juan",
                        "telefono": "+543825111111",
                        "email": "rector@undec.edu.ar"
                    },
                    "administradorInstitucional": {
                        "apellido": "Gómez",
                        "primerNombre": "María",
                        "telefono": "+543825222222",
                        "email": "admin@undec.edu.ar",
                        "cargo": "Secretario",
                        "ambito": "Rectorado"
                    },
                    "sedeCentral": {
                        "calle": "9 de Julio",
                        "numero": "22",
                        "codigoPostal": "5360",
                        "provinciaId": 1,
                        "departamentoId": 1,
                        "localidadId": 1
                    }
                }
                """;

        mockMvc.perform(put("/api/instituciones/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Universidad Nacional de Chilecito"));
    }

    @Test
    @DisplayName("PATCH /api/instituciones/{id}/estado actualiza estado y retorna 200 OK")
    void cambiarEstadoRetorna200() throws Exception {
        UUID id = UUID.randomUUID();
        Institucion actualizada = buildSampleInstitucion();
        when(cambiarEstadoInstitucionUseCase.cambiarEstado(id, false)).thenReturn(actualizada);

        mockMvc.perform(patch("/api/instituciones/{id}/estado?activo=false", id))
                .andExpect(status().isOk());

        verify(cambiarEstadoInstitucionUseCase).cambiarEstado(id, false);
    }

    @Test
    @DisplayName("DELETE /api/instituciones/{id} elimina y retorna 204 No Content")
    void eliminarRetorna204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(eliminarInstitucionUseCase).eliminar(id);

        mockMvc.perform(delete("/api/instituciones/{id}", id))
                .andExpect(status().isNoContent());

        verify(eliminarInstitucionUseCase).eliminar(id);
    }
}
