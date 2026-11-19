package com.undec.acreditacion.infrastructure.user;

import com.undec.acreditacion.application.input.ChangeUserStatusUseCase;
import com.undec.acreditacion.application.input.CreateUserCommand;
import com.undec.acreditacion.application.input.CreateUserUseCase;
import com.undec.acreditacion.application.input.ListUsersUseCase;
import com.undec.acreditacion.application.input.UpdateUserCommand;
import com.undec.acreditacion.application.input.UpdateUserUseCase;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private ListUsersUseCase listUsersUseCase;
    private CreateUserUseCase createUserUseCase;
    private UpdateUserUseCase updateUserUseCase;
    private ChangeUserStatusUseCase changeUserStatusUseCase;

    @BeforeEach
    void setUp() {
        listUsersUseCase = mock(ListUsersUseCase.class);
        createUserUseCase = mock(CreateUserUseCase.class);
        updateUserUseCase = mock(UpdateUserUseCase.class);
        changeUserStatusUseCase = mock(ChangeUserStatusUseCase.class);

        UserController controller = new UserController(
                listUsersUseCase,
                createUserUseCase,
                updateUserUseCase,
                changeUserStatusUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UserExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/users returns list of users with 200 OK")
    void listUsersReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        Role role = Role.system(UUID.randomUUID(), "AEA", "AEA", "Desc");
        User user = User.rehydrate(userId, "admin", "admin@undec.edu.ar", "pwd", true, true, Set.of(role));

        when(listUsersUseCase.listUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].email").value("admin@undec.edu.ar"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].systemUser").value(true))
                .andExpect(jsonPath("$[0].roleCodes[0]").value("AEA"));
    }

    @Test
    @DisplayName("POST /api/users creates user and returns 201 Created")
    void createUserReturns201() throws Exception {
        UUID userId = UUID.randomUUID();
        Role role = Role.system(UUID.randomUUID(), "AC_COORDINATOR", "Coord", "Desc");
        User user = User.rehydrate(userId, "coord", "coord@undec.edu.ar", "pwd", true, false, Set.of(role));

        when(createUserUseCase.createUser(any(CreateUserCommand.class))).thenReturn(user);

        String json = """
                {
                    "username": "coord",
                    "email": "coord@undec.edu.ar",
                    "password": "secretpassword",
                    "roleCodes": ["AC_COORDINATOR"]
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("coord"))
                .andExpect(jsonPath("$.email").value("coord@undec.edu.ar"))
                .andExpect(jsonPath("$.roleCodes[0]").value("AC_COORDINATOR"));

        verify(createUserUseCase).createUser(any(CreateUserCommand.class));
    }

    @Test
    @DisplayName("PUT /api/users/{id} updates user and returns 200 OK")
    void updateUserReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        User updated = User.rehydrate(userId, "new_name", "new@undec.edu.ar", "pwd", true, false, Set.of());

        when(updateUserUseCase.updateUser(any(UpdateUserCommand.class))).thenReturn(updated);

        String json = """
                {
                    "username": "new_name",
                    "email": "new@undec.edu.ar",
                    "roleCodes": [],
                    "newPassword": "new_password"
                }
                """;

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("new_name"))
                .andExpect(jsonPath("$.email").value("new@undec.edu.ar"));

        verify(updateUserUseCase).updateUser(any(UpdateUserCommand.class));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/status changes status and returns 200 OK")
    void changeStatusReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        User updated = User.rehydrate(userId, "user", "user@undec.edu.ar", "pwd", false, false, Set.of());

        when(changeUserStatusUseCase.changeStatus(eq(userId), eq(false))).thenReturn(updated);

        String json = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(changeUserStatusUseCase).changeStatus(userId, false);
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/status throws 400 when deactivating system user")
    void deactivatingSystemUserReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(changeUserStatusUseCase.changeStatus(eq(userId), eq(false)))
                .thenThrow(new DomainValidationException("System user cannot be deactivated"));

        String json = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("System user cannot be deactivated"));
    }
}
