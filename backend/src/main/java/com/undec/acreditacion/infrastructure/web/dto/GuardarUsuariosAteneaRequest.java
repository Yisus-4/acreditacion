package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.application.input.GuardarItemMatrizPermisoInputDto;
import com.undec.acreditacion.application.input.GuardarUsuarioPermisoAteneaInputDto;

import java.util.List;

public record GuardarUsuariosAteneaRequest(
        List<GuardarUsuarioPermisoAteneaInputDto> usuarios,
        List<GuardarItemMatrizPermisoInputDto> matrizPermisos
) {}
