package com.undec.acreditacion.infrastructure.config;

import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapRunnerTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordHasher passwordHasher;
    private AdminBootstrapRunner runner;

    private static final String ADMIN_EMAIL = "admin@undec.edu.ar";
    private static final String ADMIN_PASSWORD = "secret_password";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordHasher = mock(PasswordHasher.class);

        runner = new AdminBootstrapRunner(
                userRepository,
                roleRepository,
                passwordHasher,
                ADMIN_EMAIL,
                ADMIN_PASSWORD
        );
    }

    @Test
    @DisplayName("creates initial admin user when none exists and role is found")
    void createsAdminWhenNoneExists() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        Role adminRole = Role.system(UUID.randomUUID(), "AEA", "Área de Evaluación y Acreditación", "Desc");
        when(roleRepository.findByCode("AEA")).thenReturn(Optional.of(adminRole));
        when(passwordHasher.hash(ADMIN_PASSWORD)).thenReturn("hashed_secret");

        runner.run();

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("skips creation if admin already exists")
    void skipsWhenAdminExists() {
        User existing = User.register("admin", ADMIN_EMAIL, "hash");
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existing));

        runner.run();

        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("skips creation if AEA role is missing from DB")
    void skipsWhenRoleNotFound() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(roleRepository.findByCode("AEA")).thenReturn(Optional.empty());

        runner.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
