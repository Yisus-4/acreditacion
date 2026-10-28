package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.GrupoUsuarioAtenea;
import com.undec.acreditacion.domain.entities.TipoAccesoAtenea;

import java.util.UUID;

public record GuardarUsuarioPermisoAteneaInputDto(
        UUID id,
        GrupoUsuarioAtenea grupoUsuario,
        String nombre,
        String apellido,
        String dniCuil,
        String correo,
        String areaDependencia,
        String cargoFuncion,
        TipoAccesoAtenea tipoAcceso,
        String seccionAlcance,
        boolean requiereCuentaNuevaConeauGlobal,
        boolean cuentaConeauGlobalCreada,
        boolean habilitadoEnConeau
) {}
