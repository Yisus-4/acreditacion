package com.undec.acreditacion.infrastructure.designacion.dto;

import java.util.UUID;

public record GuardarDesignacionDocenteRequest(
        UUID docenteId,
        UUID cargoDocenteId,
        UUID dedicacionDocenteId,
        UUID tipoDesignacionId,
        String nroDesignacionMapuche,
        String observaciones
) {}
