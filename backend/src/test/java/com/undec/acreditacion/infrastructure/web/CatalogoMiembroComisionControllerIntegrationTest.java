package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.ConsultarCatalogoMiembrosCaUseCase;
import com.undec.acreditacion.application.output.CatalogoMiembroComisionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogoMiembroComisionControllerIntegrationTest {

    private MockMvc mockMvc;
    private ConsultarCatalogoMiembrosCaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = mock(ConsultarCatalogoMiembrosCaUseCase.class);
        CatalogoMiembroComisionController controller = new CatalogoMiembroComisionController(useCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/comision-autoevaluacion/catalogo retorna lista de miembros")
    void consultarCatalogo() throws Exception {
        CatalogoMiembroComisionDto item = new CatalogoMiembroComisionDto(
                UUID.randomUUID(), "Salerno", "Julio Luis", "8.386.710", "20-08386710-9",
                "jsalerno@undec.edu.ar", "salernojulioluis@gmail.com", "Director de Carrera",
                true, Instant.now()
        );
        when(useCase.buscar("Salerno")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/comision-autoevaluacion/catalogo").param("busqueda", "Salerno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apellido").value("Salerno"))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Julio Luis"))
                .andExpect(jsonPath("$[0].cargoFuncion").value("Director de Carrera"));
    }
}
