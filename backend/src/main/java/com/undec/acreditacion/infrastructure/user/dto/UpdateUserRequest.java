package com.undec.acreditacion.infrastructure.user.dto;

import java.util.List;
import java.util.UUID;

public record UpdateUserRequest(
        String username,
        String email,
        List<String> roleCodes,
        String newPassword,
        UUID unidadAcademicaId
) {
    public UpdateUserRequest(String username, String email, List<String> roleCodes, String newPassword) {
        this(username, email, roleCodes, newPassword, null);
    }
}
