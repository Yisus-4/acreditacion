package com.undec.acreditacion.application.input;

import java.util.List;
import java.util.UUID;

public record UpdateUserCommand(
        UUID userId,
        String username,
        String email,
        List<String> roleCodes,
        String optionalNewPassword,
        UUID unidadAcademicaId,
        UUID departamentoAcademicoId
) {
    public UpdateUserCommand(UUID userId, String username, String email, List<String> roleCodes, String optionalNewPassword) {
        this(userId, username, email, roleCodes, optionalNewPassword, null, null);
    }

    public UpdateUserCommand(UUID userId, String username, String email, List<String> roleCodes,
                             String optionalNewPassword, UUID unidadAcademicaId) {
        this(userId, username, email, roleCodes, optionalNewPassword, unidadAcademicaId, null);
    }
}
