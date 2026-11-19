package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.UpdateUserCommand;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateUserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordHasher passwordHasher;
    private UpdateUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        service = new UpdateUserService(userRepository, roleRepository, passwordHasher);
    }

    @Test
    @DisplayName("Updates profile, password, and syncs roles successfully")
    void updatesUserSuccessfully() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.rehydrate(userId, "old_user", "old@undec.edu.ar", "old_hash", true, false, Set.of());
        Role role = Role.system(UUID.randomUUID(), "AEA", "AEA", "Desc");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.existsByEmail("new@undec.edu.ar")).thenReturn(false);
        when(passwordHasher.hash("new_secret_pwd")).thenReturn("new_hash");
        when(roleRepository.findByCode("AEA")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(
                userId,
                "new_user",
                "new@undec.edu.ar",
                List.of("AEA"),
                "new_secret_pwd"
        );

        User result = service.updateUser(command);

        assertEquals("new_user", result.getUsername());
        assertEquals("new@undec.edu.ar", result.getEmail());
        assertEquals("new_hash", result.getPasswordHash());
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains(role));
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Throws IllegalArgumentException if new username already exists")
    void throwsWhenUsernameAlreadyExists() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.rehydrate(userId, "user1", "user1@undec.edu.ar", "hash", true, false, Set.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        UpdateUserCommand command = new UpdateUserCommand(userId, "existing_user", "user1@undec.edu.ar", null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateUser(command));
        assertTrue(ex.getMessage().contains("Username already in use"));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException if new email already exists")
    void throwsWhenEmailAlreadyExists() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.rehydrate(userId, "user1", "user1@undec.edu.ar", "hash", true, false, Set.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("other@undec.edu.ar")).thenReturn(true);

        UpdateUserCommand command = new UpdateUserCommand(userId, "user1", "other@undec.edu.ar", null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateUser(command));
        assertTrue(ex.getMessage().contains("Email already in use"));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when user does not exist")
    void throwsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateUserCommand command = new UpdateUserCommand(userId, "user1", "user1@undec.edu.ar", null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateUser(command));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException when role code does not exist")
    void throwsWhenRoleNotFound() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.rehydrate(userId, "user1", "user1@undec.edu.ar", "hash", true, false, Set.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByCode("NON_EXISTENT")).thenReturn(Optional.empty());

        UpdateUserCommand command = new UpdateUserCommand(userId, "user1", "user1@undec.edu.ar", List.of("NON_EXISTENT"), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateUser(command));
        assertTrue(ex.getMessage().contains("Role not found"));
    }

    @Test
    @DisplayName("Updates unidadAcademicaId successfully")
    void updatesUnidadAcademicaSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID escuelaId = UUID.randomUUID();
        User existingUser = User.rehydrate(userId, "user1", "user1@undec.edu.ar", "hash", true, false, Set.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(
                userId,
                "user1",
                "user1@undec.edu.ar",
                List.of(),
                null,
                escuelaId
        );

        User result = service.updateUser(command);
        assertEquals(escuelaId, result.getUnidadAcademicaId());
    }
}
