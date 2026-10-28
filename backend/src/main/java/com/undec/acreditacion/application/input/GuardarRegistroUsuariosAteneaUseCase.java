package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;

import java.util.List;
import java.util.UUID;

public interface GuardarRegistroUsuariosAteneaUseCase {
    RegistroUsuariosAteneaDto guardar(UUID procesoId, UUID usuarioId, List<GuardarUsuarioPermisoAteneaInputDto> usuarios);
    RegistroUsuariosAteneaDto guardar(UUID procesoId, UUID usuarioId, List<GuardarUsuarioPermisoAteneaInputDto> usuarios, List<GuardarItemMatrizPermisoInputDto> matrizPermisos);
}
