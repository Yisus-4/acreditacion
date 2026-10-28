package com.undec.acreditacion.application.output;

import com.undec.acreditacion.domain.entities.EstadoRegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.GrupoUsuarioAtenea;
import com.undec.acreditacion.domain.entities.TipoAccesoAtenea;
import com.undec.acreditacion.domain.entities.TipoPermisoMatrizAtenea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegistroUsuariosAteneaDto(
        UUID id,
        UUID procesoId,
        EstadoRegistroUsuariosAtenea estado,
        LocalDate fechaLimite,
        int diasRestantes,
        String semaforoTexto,
        Instant generadoEn,
        Instant aprobadoEn,
        UUID creadoPor,
        String rutaDocumento,
        boolean finalizado,
        List<UsuarioPermisoAteneaDto> usuarios,
        List<ItemMatrizPermisoAteneaDto> matrizPermisos
) {
    public record UsuarioPermisoAteneaDto(
            UUID id,
            GrupoUsuarioAtenea grupoUsuario,
            String nombre,
            String apellido,
            String nombreCompleto,
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

    public record ItemMatrizPermisoAteneaDto(
            UUID id,
            String procesoActividad,
            String item,
            String seccion,
            TipoPermisoMatrizAtenea tipoPermiso,
            String tipoPermisoDescripcion,
            boolean permisoSgiAea,
            boolean permisoCa,
            int orden
    ) {}
}
