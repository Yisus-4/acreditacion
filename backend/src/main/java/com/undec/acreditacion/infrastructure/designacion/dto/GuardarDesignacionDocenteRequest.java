package com.undec.acreditacion.infrastructure.designacion.dto;

import java.util.UUID;

public record GuardarDesignacionDocenteRequest(
        UUID docenteId,
        UUID cargoDocenteId,
        UUID dedicacionDocenteId,
        UUID tipoDesignacionId,
        String nroDesignacionMapuche,
        String observaciones,
        String motivoCambio,
        UUID designacionReemplazadaId
) {
    public GuardarDesignacionDocenteRequest(
            UUID docenteId,
            UUID cargoDocenteId,
            UUID dedicacionDocenteId,
            UUID tipoDesignacionId,
            String nroDesignacionMapuche,
            String observaciones
    ) {
        this(docenteId, cargoDocenteId, dedicacionDocenteId, tipoDesignacionId, nroDesignacionMapuche, observaciones, null, null);
    }

    public GuardarDesignacionDocenteRequest(
            UUID docenteId,
            UUID cargoDocenteId,
            UUID dedicacionDocenteId,
            UUID tipoDesignacionId,
            String nroDesignacionMapuche,
            String observaciones,
            String motivoCambio
    ) {
        this(docenteId, cargoDocenteId, dedicacionDocenteId, tipoDesignacionId, nroDesignacionMapuche, observaciones, motivoCambio, null);
    }
}
