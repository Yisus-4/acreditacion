package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;

import java.util.UUID;

public interface FinalizarRegistroUsuariosAteneaUseCase {
    RegistroUsuariosAteneaDto finalizar(UUID procesoId, UUID usuarioAeaId);
}
