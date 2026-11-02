package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GuardarRegistroUsuariosAteneaUseCase {
    RegistroUsuariosAteneaDto guardar(UUID procesoId, UUID usuarioId, List<GuardarUsuarioPermisoAteneaInputDto> usuarios);
    RegistroUsuariosAteneaDto guardar(UUID procesoId, UUID usuarioId, List<GuardarUsuarioPermisoAteneaInputDto> usuarios, List<GuardarItemMatrizPermisoInputDto> matrizPermisos);

    default RegistroUsuariosAteneaDto actualizarPlazo(UUID procesoId, int diasHabiles) {
        throw new UnsupportedOperationException("No implementado");
    }

    default RegistroUsuariosAteneaDto extenderPlazoFecha(UUID procesoId, LocalDate nuevaFechaLimite) {
        throw new UnsupportedOperationException("No implementado");
    }
}
