package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @Test
    void startsActiveAndAssignsEachRoleOnlyOnce() {
        User user = User.register("user", "user@example.org", "encoded-password");
        Role role = Role.system(UUID.randomUUID(), "AEA", "Área de Evaluación y Acreditación", "Manages the system");

        user.assignRole(role);

        assertTrue(user.isActive());
        assertEquals(1, user.getRoles().size());
        assertThrows(DomainValidationException.class, () -> user.assignRole(role));
    }

    @Test
    void registerCreatesAnActiveUserWithoutRoles() {
        User user = User.register("user", "user@example.org", "encoded-password");

        assertTrue(user.isActive());
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    void deactivatedUserCannotReceiveRoles() {
        User user = user();
        user.deactivate();

        assertFalse(user.isActive());
        assertThrows(DomainValidationException.class,
                () -> user.assignRole(Role.custom(UUID.randomUUID(), "EDITOR", "Editor", "Edits content")));
    }

    @Test
    void activationRestoresTheAbilityToReceiveRoles() {
        User user = user();
        user.deactivate();

        user.activate();
        user.assignRole(Role.custom(UUID.randomUUID(), "EDITOR", "Editor", "Edits content"));

        assertTrue(user.isActive());
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void validatesEmail() {
        assertThrows(DomainValidationException.class,
                () -> User.register("user", "invalid-email", "hash"));
    }

    @Test
    void rehydratesPersistedStateWithoutForcingUserToBeActive() {
        UUID id = UUID.randomUUID();
        Role role = Role.custom(UUID.randomUUID(), "EDITOR", "Editor", "Edits content");

        User user = User.rehydrate(id, "user", "user@example.org", "encoded-password", false,
                 Set.of(role));

        assertEquals(id, user.getId());
        assertFalse(user.isActive());
        assertEquals(java.util.Set.of(role), user.getRoles());
        assertThrows(DomainValidationException.class, () -> user.assignRole(role));
    }

    @Test
    void rehydrateRequiresRolesToMakePersistedStateExplicit() {
        assertThrows(DomainValidationException.class,
                () -> User.rehydrate(UUID.randomUUID(), "user", "user@example.org", "hash", true, null));
    }

    @Test
    void updatesProfileWithValidValues() {
        User user = user();
        user.updateProfile("new_username", "new_email@example.org");

        assertEquals("new_username", user.getUsername());
        assertEquals("new_email@example.org", user.getEmail());
    }

    @Test
    void rejectsInvalidEmailOnUpdateProfile() {
        User user = user();
        assertThrows(DomainValidationException.class, () -> user.updateProfile("new_username", "invalid"));
    }

    @Test
    void updatesPasswordHash() {
        User user = user();
        user.changePassword("new_hash_123");

        assertEquals("new_hash_123", user.getPasswordHash());
    }

    @Test
    void syncsRolesSuccessfully() {
        User user = user();
        Role role1 = Role.system(UUID.randomUUID(), "ROLE_1", "Role 1", "Desc 1");
        Role role2 = Role.custom(UUID.randomUUID(), "ROLE_2", "Role 2", "Desc 2");

        user.syncRoles(Set.of(role1, role2));

        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(role1));
        assertTrue(user.getRoles().contains(role2));
    }

    @Test
    void rejectsNullOrNullElementsInSyncRoles() {
        User user = user();
        assertThrows(DomainValidationException.class, () -> user.syncRoles(null));

        Set<Role> rolesWithNull = new java.util.HashSet<>();
        rolesWithNull.add(null);
        assertThrows(DomainValidationException.class, () -> user.syncRoles(rolesWithNull));
    }

    @Test
    void systemUserCannotBeDeactivated() {
        User systemUser = User.rehydrate(UUID.randomUUID(), "admin", "admin@example.org", "hash", true, true, Set.of());
        assertTrue(systemUser.isSystemUser());
        assertTrue(systemUser.isActive());

        DomainValidationException ex = assertThrows(DomainValidationException.class, systemUser::deactivate);
        assertEquals("System user cannot be deactivated", ex.getMessage());
        assertTrue(systemUser.isActive());
    }

    private static User user() {
        return User.register("user", "user@example.org", "encoded-password");
    }
}
