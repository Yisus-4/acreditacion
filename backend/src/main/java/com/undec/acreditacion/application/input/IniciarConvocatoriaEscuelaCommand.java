package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record IniciarConvocatoriaEscuelaCommand(
        UUID unidadAcademicaId,
        TipoProcesoAcreditacion tipoProceso,
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        String observaciones,
        List<ItemCarreraSeleccionCommand> carreras,
        UUID creadoPor
) {
}
