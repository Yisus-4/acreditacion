package com.undec.acreditacion.infrastructure.auth.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(UserSessionView user, String token, long issuedAt) {

    public record UserSessionView(
            UUID id,
            String email,
            String displayName,
            List<String> roleCodes,
            UUID unidadAcademicaId
    ) {
    }
}
