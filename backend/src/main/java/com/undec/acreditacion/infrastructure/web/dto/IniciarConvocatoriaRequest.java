package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record IniciarConvocatoriaRequest(
        UUID carreraId,
        UUID unidadAcademicaId,
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        TipoProcesoAcreditacion tipoProceso,
        String observaciones,
        List<CarreraSeleccionItemRequest> carreras
) {
    public IniciarConvocatoriaRequest(UUID carreraId,
                                    String nroResolucionConeau,
                                    LocalDate fechaInicio,
                                    LocalDate fechaCierre,
                                    TipoProcesoAcreditacion tipoProceso,
                                    String observaciones) {
        this(carreraId, null, nroResolucionConeau, fechaInicio, fechaCierre, tipoProceso, observaciones, Collections.emptyList());
    }
}
