package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroCaDto;

import java.time.LocalDate;
import java.util.UUID;

public interface SolicitarConformacionCaUseCase {
    RegistroCaDto solicitar(UUID procesoId, UUID usuarioAeaId);

    default RegistroCaDto solicitar(UUID procesoId, UUID usuarioAeaId, Integer diasPlazo) {
        return solicitar(procesoId, usuarioAeaId);
    }

    default RegistroCaDto extenderPlazoFecha(UUID procesoId, UUID usuarioAeaId, LocalDate nuevaFechaLimite) {
        return solicitar(procesoId, usuarioAeaId);
    }
}
