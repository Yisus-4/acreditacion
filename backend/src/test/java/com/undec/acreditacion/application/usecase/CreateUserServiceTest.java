package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CreateUserCommand;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateUserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordHasher passwordHasher;
    private CreateUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        service = new CreateUserService(userRepository, roleRepository, passwordHasher);
    }

    @Test
    @DisplayName("Creates user with hashed password and assigned roles")
    void createsUserSuccessfully() {
        Role role = Role.system(UUID.randomUUID(), "AEA", "AEA", "Desc");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@undec.edu.ar")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn("hashed123");
        when(roleRepository.findByCode("AEA")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserCommand command = new CreateUserCommand(
                "newuser",
                "newuser@undec.edu.ar",
                "password123",
                List.of("AEA")
        );

        User created = service.createUser(command);

        assertNotNull(created);
        assertEquals("newuser", created.getUsername());
        assertEquals("newuser@undec.edu.ar", created.getEmail());
        assertEquals("hashed123", created.getPasswordHash());
        assertEquals(1, created.getRoles().size());
        assertTrue(created.isActive());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException if username exists")
    void throwsWhenUsernameExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);
        CreateUserCommand command = new CreateUserCommand("newuser", "newuser@undec.edu.ar", "pwd", null);

        assertThrows(IllegalArgumentException.class, () -> service.createUser(command));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException if email exists")
    void throwsWhenEmailExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@undec.edu.ar")).thenReturn(true);
        CreateUserCommand command = new CreateUserCommand("newuser", "newuser@undec.edu.ar", "pwd", null);

        assertThrows(IllegalArgumentException.class, () -> service.createUser(command));
    }
}
