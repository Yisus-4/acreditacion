package com.undec.acreditacion.infrastructure.auth;

import com.undec.acreditacion.application.input.RegisterUserUseCase;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegisterControllerTest {

    private MockMvc mockMvc;
    private RegisterUserUseCase registerUserUseCase;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordHasher passwordHasher;

    @BeforeEach
    void setUp() {
        registerUserUseCase = mock(RegisterUserUseCase.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordHasher = mock(PasswordHasher.class);

        RegisterController controller = new RegisterController(
                registerUserUseCase,
                userRepository,
                roleRepository,
                passwordHasher
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("POST /auth/register creates user and returns 201 with role codes")
    void registersUserSuccessfully() throws Exception {
        UUID roleId = UUID.randomUUID();
        Role role = Role.system(roleId, "DIRECTOR_CARRERA", "Coordinador", "Desc");
        User registered = User.register("coordinadora", "coordinadora@undec.edu.ar", "hashed_pwd");

        when(passwordHasher.hash("undec2026")).thenReturn("hashed_pwd");
        when(registerUserUseCase.register(eq("coordinadora"), eq("coordinadora@undec.edu.ar"), eq("hashed_pwd")))
                .thenReturn(registered);
        when(roleRepository.findByCode("DIRECTOR_CARRERA")).thenReturn(Optional.of(role));

        String requestJson = """
                {
                    "username": "coordinadora",
                    "email": "coordinadora@undec.edu.ar",
                    "password": "undec2026",
                    "roleCodes": ["DIRECTOR_CARRERA"]
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("coordinadora"))
                .andExpect(jsonPath("$.email").value("coordinadora@undec.edu.ar"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roleCodes[0]").value("DIRECTOR_CARRERA"));

        verify(userRepository).save(any(User.class));
    }
}
