package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.DocenteResumenResponse;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ListarDocentesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AutoevaluacionDocenteControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ListarDocentesService listarDocentesService;

    private AutoevaluacionDocenteController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AutoevaluacionDocenteController(listarDocentesService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AutoevaluacionExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/autoevaluacion/docentes sin query retorna lista completa de docentes activos")
    void deberiaListarTodosLosDocentesActivos() throws Exception {
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();

        List<DocenteResumenResponse> mockDocentes = List.of(
                new DocenteResumenResponse(doc1Id, "carlos.gomez", "Carlos Gómez", "cgomez@undec.edu.ar", uaId, "Escuela de Ingeniería"),
                new DocenteResumenResponse(doc2Id, "laura.martino", "Laura Martino", "lmartino@undec.edu.ar", uaId, "Escuela de Ingeniería")
        );

        when(listarDocentesService.listarDocentes(null)).thenReturn(mockDocentes);

        mockMvc.perform(get("/api/autoevaluacion/docentes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("carlos.gomez")))
                .andExpect(jsonPath("$[0].nombreCompleto", is("Carlos Gómez")))
                .andExpect(jsonPath("$[1].username", is("laura.martino")))
                .andExpect(jsonPath("$[1].nombreCompleto", is("Laura Martino")));

        verify(listarDocentesService).listarDocentes(null);
    }

    @Test
    @DisplayName("GET /api/autoevaluacion/docentes?query=gomez retorna docentes filtrados por texto")
    void deberiaFiltrarDocentesPorQuery() throws Exception {
        UUID doc1Id = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();

        List<DocenteResumenResponse> mockDocentes = List.of(
                new DocenteResumenResponse(doc1Id, "carlos.gomez", "Carlos Gómez", "cgomez@undec.edu.ar", uaId, "Escuela de Ingeniería")
        );

        when(listarDocentesService.listarDocentes("gomez")).thenReturn(mockDocentes);

        mockMvc.perform(get("/api/autoevaluacion/docentes")
                        .param("query", "gomez")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("carlos.gomez")))
                .andExpect(jsonPath("$[0].nombreCompleto", is("Carlos Gómez")));

        verify(listarDocentesService).listarDocentes("gomez");
    }

    @Test
    @DisplayName("Verifica que AutoevaluacionDocenteController cuenta con la seguridad @PreAuthorize requerida")
    void deberiaContarConPreAuthorizeAdecuado() throws NoSuchMethodException {
        Method method = AutoevaluacionDocenteController.class.getMethod("listarDocentes", String.class, String.class);
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        if (preAuth == null) {
            preAuth = AutoevaluacionDocenteController.class.getAnnotation(PreAuthorize.class);
        }

        assertNotNull(preAuth, "El endpoint /api/autoevaluacion/docentes debe estar protegido con @PreAuthorize");
        String value = preAuth.value();
        assertTrue(value.contains("COMISION_AUTOEVALUACION"), "Debe permitir COMISION_AUTOEVALUACION");
        assertTrue(value.contains("DIRECTOR_CARRERA"), "Debe permitir DIRECTOR_CARRERA");
        assertTrue(value.contains("AEA"), "Debe permitir roles de gestión");
    }
}
