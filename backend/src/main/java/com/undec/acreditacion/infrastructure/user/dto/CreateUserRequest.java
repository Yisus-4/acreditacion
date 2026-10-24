package com.undec.acreditacion.infrastructure.user.dto;

import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        String username,
        String email,
        String password,
        List<String> roleCodes,
        UUID unidadAcademicaId
) {
    public CreateUserRequest(String username, String email, String password, List<String> roleCodes) {
        this(username, email, password, roleCodes, null);
    }
}
