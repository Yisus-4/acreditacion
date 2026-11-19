package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleTest {

    @Test
    void managesPermissionsWithoutExposingMutableCollection() {
        Role role = role();
        Permission permission = permission();

        role.addPermission(permission);

        assertEquals(1, role.getPermissions().size());
        assertThrows(UnsupportedOperationException.class, () -> role.getPermissions().clear());
        assertThrows(DomainValidationException.class, () -> role.addPermission(permission));
    }

    @Test
    void systemRoleIsActiveAndCannotBeDeactivated() {
        Role role = Role.system(UUID.randomUUID(), "AEA", "Área de Evaluación y Acreditación", "Manages the system");

        assertTrue(role.isActive());
        assertTrue(role.isSystemRole());
        assertThrows(DomainValidationException.class, role::deactivate);
        assertTrue(role.isActive());
    }

    @Test
    void customFactoryCreatesNonSystemRole() {
        Role role = Role.custom(UUID.randomUUID(), "REVIEWER", "Reviewer", "Reviews submissions");

        assertTrue(role.isActive());
        assertFalse(role.isSystemRole());
    }

    @Test
    void customRoleCanBeDeactivatedAndActivated() {
        Role role = Role.custom(UUID.randomUUID(), "REVIEWER", "Reviewer", "Reviews submissions");

        role.deactivate();
        assertFalse(role.isActive());

        role.activate();
        assertTrue(role.isActive());
        assertFalse(role.isSystemRole());
    }

    private static Role role() {
        return Role.custom(UUID.randomUUID(), "REVIEWER", "Reviewer", "Reviews submissions");
    }

    private static Permission permission() {
        return new Permission(UUID.randomUUID(), "REVIEW_SUBMISSION", "Review submission", "Review submissions");
    }
}
