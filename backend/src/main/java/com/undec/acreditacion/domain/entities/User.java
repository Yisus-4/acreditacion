package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class User {

    private final UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private final Set<Role> roles = new LinkedHashSet<>();
    private boolean active;
    private final boolean systemUser;
    private UUID unidadAcademicaId;
    private UUID departamentoAcademicoId;

    public static User register(String username, String email, String passwordHash) {
        return new User(UUID.randomUUID(), username, email, passwordHash, false);
    }

    public static User register(String username, String email, String passwordHash, UUID unidadAcademicaId) {
        return register(username, email, passwordHash, unidadAcademicaId, null);
    }

    public static User register(String username, String email, String passwordHash,
                                UUID unidadAcademicaId, UUID departamentoAcademicoId) {
        User user = new User(UUID.randomUUID(), username, email, passwordHash, false);
        user.unidadAcademicaId = unidadAcademicaId;
        user.departamentoAcademicoId = departamentoAcademicoId;
        return user;
    }

    public static User rehydrate(UUID id, String username, String email, String passwordHash,
                                 boolean active, Set<Role> roles) {
        return rehydrate(id, username, email, passwordHash, active, false, null, null, roles);
    }

    public static User rehydrate(UUID id, String username, String email, String passwordHash,
                                 boolean active, boolean systemUser, Set<Role> roles) {
        return rehydrate(id, username, email, passwordHash, active, systemUser, null, null, roles);
    }

    public static User rehydrate(UUID id, String username, String email, String passwordHash,
                                 boolean active, boolean systemUser, UUID unidadAcademicaId, Set<Role> roles) {
        return rehydrate(id, username, email, passwordHash, active, systemUser, unidadAcademicaId, null, roles);
    }

    public static User rehydrate(UUID id, String username, String email, String passwordHash,
                                 boolean active, boolean systemUser,
                                 UUID unidadAcademicaId, UUID departamentoAcademicoId,
                                 Set<Role> roles) {
        if (roles == null) {
            throw new DomainValidationException("User roles are required");
        }
        User user = new User(id, username, email, passwordHash, systemUser);
        user.active = active;
        user.unidadAcademicaId = unidadAcademicaId;
        user.departamentoAcademicoId = departamentoAcademicoId;
        for (Role role : roles) {
            if (role == null) {
                throw new DomainValidationException("User roles cannot contain null values");
            }
            if (!user.roles.add(role)) {
                throw new DomainValidationException("User roles cannot contain duplicates");
            }
        }
        return user;
    }

    public User(UUID id, String username, String email, String passwordHash) {
        this(id, username, email, passwordHash, false);
    }

    public User(UUID id, String username, String email, String passwordHash, boolean systemUser) {
        this.id = Objects.requireNonNull(id, "User id is required");
        this.username = Permission.requireText(username, "Username");
        this.email = requireEmail(email);
        this.passwordHash = Permission.requireText(passwordHash, "Password hash");
        this.active = true;
        this.systemUser = systemUser;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSystemUser() {
        return systemUser;
    }

    public UUID getUnidadAcademicaId() {
        return unidadAcademicaId;
    }

    public void assignUnidadAcademica(UUID unidadAcademicaId) {
        this.unidadAcademicaId = unidadAcademicaId;
    }

    public UUID getDepartamentoAcademicoId() {
        return departamentoAcademicoId;
    }

    public void assignDepartamentoAcademico(UUID departamentoAcademicoId) {
        this.departamentoAcademicoId = departamentoAcademicoId;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void updateProfile(String newUsername, String newEmail) {
        this.username = Permission.requireText(newUsername, "Username");
        this.email = requireEmail(newEmail);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Permission.requireText(newPasswordHash, "Password hash");
    }

    public void syncRoles(Set<Role> newRoles) {
        if (newRoles == null) {
            throw new DomainValidationException("User roles are required");
        }
        this.roles.clear();
        for (Role role : newRoles) {
            if (role == null) {
                throw new DomainValidationException("User roles cannot contain null values");
            }
            this.roles.add(role);
        }
    }

    public void assignRole(Role role) {
        if (!active) {
            throw new DomainValidationException("Inactive user cannot receive roles");
        }
        if (role == null) {
            throw new DomainValidationException("Role is required");
        }
        if (!roles.add(role)) {
            throw new DomainValidationException("Role is already assigned to user");
        }
    }

    public void removeRole(Role role) {
        if (!roles.remove(role)) {
            throw new DomainValidationException("Role is not assigned to user");
        }
    }

    public void deactivate() {
        if (systemUser) {
            throw new DomainValidationException("System user cannot be deactivated");
        }
        active = false;
    }

    public void activate() {
        active = true;
    }

    private static String requireEmail(String value) {
        String email = Permission.requireText(value, "Email");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new DomainValidationException("Email has an invalid format");
        }
        return email;
    }
}
