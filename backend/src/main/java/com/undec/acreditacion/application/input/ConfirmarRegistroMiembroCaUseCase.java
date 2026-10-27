package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroCaDto;

import java.util.UUID;

public interface ConfirmarRegistroMiembroCaUseCase {

    RegistroCaDto confirmarRegistro(UUID procesoId, UUID miembroId, UUID usuarioId);
}
