package com.undec.acreditacion.infrastructure.user.dto;

import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean active,
        boolean systemUser,
        List<String> roleCodes,
        UUID unidadAcademicaId,
        UUID departamentoAcademicoId
) {
    public UserResponse(UUID id, String username, String email, boolean active, boolean systemUser,
                        List<String> roleCodes, UUID unidadAcademicaId) {
        this(id, username, email, active, systemUser, roleCodes, unidadAcademicaId, null);
    }

    public static UserResponse fromDomain(User user) {
        List<String> codes = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).toList()
                : List.of();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                user.isSystemUser(),
                codes,
                user.getUnidadAcademicaId(),
                user.getDepartamentoAcademicoId()
        );
    }
}
