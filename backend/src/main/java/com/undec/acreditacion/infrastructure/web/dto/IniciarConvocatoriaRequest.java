package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;

import java.time.LocalDate;
import java.util.UUID;

public record IniciarConvocatoriaRequest(
        UUID carreraId,
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        TipoProcesoAcreditacion tipoProceso,
        String observaciones
) {
}
