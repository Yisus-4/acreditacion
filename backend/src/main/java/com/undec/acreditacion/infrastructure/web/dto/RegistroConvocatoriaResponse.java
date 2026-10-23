package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegistroConvocatoriaResponse(
        UUID id,
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        String observaciones,
        String rmEstandares,
        String areaDisciplinaria,
        String modalidadCarrera,
        String estadoCarrera,
        String faseEvaluacion,
        LocalDate fechaLimitePresentacionConeau,
        LocalDate fechaFormalizacionElectronica,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public RegistroConvocatoriaResponse(UUID id,
                                       String nroResolucionConeau,
                                       LocalDate fechaInicio,
                                       LocalDate fechaCierre,
                                       String observaciones,
                                       String rmEstandares,
                                       String areaDisciplinaria,
                                       String modalidadCarrera,
                                       String estadoCarrera,
                                       String faseEvaluacion,
                                       LocalDate fechaLimitePresentacionConeau,
                                       Instant creadoEn,
                                       Instant actualizadoEn) {
        this(
                id,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                areaDisciplinaria,
                modalidadCarrera,
                estadoCarrera,
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                null,
                creadoEn,
                actualizadoEn
        );
    }

    public RegistroConvocatoriaResponse(UUID id,
                                       String nroResolucionConeau,
                                       LocalDate fechaInicio,
                                       LocalDate fechaCierre,
                                       String observaciones,
                                       String rmEstandares,
                                       String faseEvaluacion,
                                       LocalDate fechaLimitePresentacionConeau,
                                       Instant creadoEn,
                                       Instant actualizadoEn) {
        this(
                id,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                null,
                "PRESENCIAL",
                "EN_FUNCIONAMIENTO",
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                null,
                creadoEn,
                actualizadoEn
        );
    }

    public RegistroConvocatoriaResponse(UUID id,
                                       String nroResolucionConeau,
                                       LocalDate fechaInicio,
                                       LocalDate fechaCierre,
                                       String observaciones,
                                       Instant creadoEn,
                                       Instant actualizadoEn) {
        this(
                id,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                null,
                null,
                "PRESENCIAL",
                "EN_FUNCIONAMIENTO",
                "PRIMERA_FASE",
                null,
                null,
                creadoEn,
                actualizadoEn
        );
    }
}
