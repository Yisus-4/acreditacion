package com.undec.acreditacion.infrastructure.user;

import com.undec.acreditacion.application.input.ListRolesUseCase;
import com.undec.acreditacion.domain.entities.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerTest {

    private MockMvc mockMvc;
    private ListRolesUseCase listRolesUseCase;

    @BeforeEach
    void setUp() {
        listRolesUseCase = mock(ListRolesUseCase.class);
        RoleController controller = new RoleController(listRolesUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/roles returns list of roles with 200 OK")
    void listRolesReturns200() throws Exception {
        UUID roleId = UUID.randomUUID();
        Role role = Role.system(roleId, "AEA", "Área de Evaluación y Acreditación", "Desc");

        when(listRolesUseCase.listRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(roleId.toString()))
                .andExpect(jsonPath("$[0].code").value("AEA"))
                .andExpect(jsonPath("$[0].name").value("Área de Evaluación y Acreditación"))
                .andExpect(jsonPath("$[0].description").value("Desc"));
    }
}
