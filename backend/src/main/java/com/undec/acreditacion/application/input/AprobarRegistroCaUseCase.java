package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroCaDto;

import java.util.UUID;

public interface AprobarRegistroCaUseCase {
    RegistroCaDto aprobar(UUID procesoId, UUID usuarioAeaId);
}
