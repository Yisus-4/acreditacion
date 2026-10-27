package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroCaDto;

import java.util.UUID;

public interface ObservarRegistroCaUseCase {
    RegistroCaDto observar(UUID procesoId, UUID usuarioAeaId, String motivo);
}
