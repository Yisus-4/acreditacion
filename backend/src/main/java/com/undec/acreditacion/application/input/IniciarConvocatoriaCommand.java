package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.TipoProcesoAcreditacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record IniciarConvocatoriaCommand(
        UUID carreraId,
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        TipoProcesoAcreditacion tipoProceso,
        String observaciones,
        UUID creadoPor
) {
    public IniciarConvocatoriaCommand {
        if (carreraId == null) {
            throw new DomainValidationException("El ID de la carrera es obligatorio");
        }
        if (nroResolucionConeau == null || nroResolucionConeau.trim().isEmpty()) {
            throw new DomainValidationException("El número de resolución CONEAU es obligatorio");
        }
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now();
        } else if (fechaInicio.isBefore(LocalDate.now())) {
            throw new DomainValidationException("La fecha de inicio no puede ser anterior a la fecha actual");
        }
        if (fechaCierre == null) {
            throw new DomainValidationException("La fecha de cierre es obligatoria");
        }
        if (fechaCierre.isBefore(LocalDate.now())) {
            throw new DomainValidationException("La fecha de cierre no puede ser anterior a la fecha actual");
        }
        if (fechaCierre.isBefore(fechaInicio)) {
            throw new DomainValidationException("La fecha de cierre no puede ser anterior a la fecha de inicio");
        }
        if (tipoProceso == null) {
            throw new DomainValidationException("El tipo de proceso es obligatorio");
        }
        if (creadoPor == null) {
            throw new DomainValidationException("El usuario creador es obligatorio");
        }
        nroResolucionConeau = nroResolucionConeau.trim();
        observaciones = observaciones != null ? observaciones.trim() : null;
    }
}
