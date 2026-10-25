package com.undec.acreditacion.infrastructure.web.dto;

import java.time.LocalDate;

public record GuardarRg001Request(
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        LocalDate fechaLimitePresentacionConeau,
        String rmEstandares,
        String areaDisciplinaria,
        String modalidadCarrera,
        String estadoCarrera,
        String faseEvaluacion,
        String observaciones
) {
}
