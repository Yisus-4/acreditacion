package com.undec.acreditacion.infrastructure.user.dto;

import java.util.List;
import java.util.UUID;

public record UpdateUserRequest(
        String username,
        String email,
        List<String> roleCodes,
        String newPassword,
        UUID unidadAcademicaId,
        UUID departamentoAcademicoId
) {
    public UpdateUserRequest(String username, String email, List<String> roleCodes, String newPassword) {
        this(username, email, roleCodes, newPassword, null, null);
    }

    public UpdateUserRequest(String username, String email, List<String> roleCodes, String newPassword, UUID unidadAcademicaId) {
        this(username, email, roleCodes, newPassword, unidadAcademicaId, null);
    }
}
