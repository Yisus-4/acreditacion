package com.undec.acreditacion.application.input;

import java.util.List;
import java.util.UUID;

public record CreateUserCommand(
        String username,
        String email,
        String password,
        List<String> roleCodes,
        UUID unidadAcademicaId,
        UUID departamentoAcademicoId
) {
    public CreateUserCommand(String username, String email, String password, List<String> roleCodes) {
        this(username, email, password, roleCodes, null, null);
    }

    public CreateUserCommand(String username, String email, String password, List<String> roleCodes, UUID unidadAcademicaId) {
        this(username, email, password, roleCodes, unidadAcademicaId, null);
    }
}
