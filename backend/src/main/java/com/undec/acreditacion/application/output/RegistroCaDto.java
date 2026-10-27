package com.undec.acreditacion.application.output;

import com.undec.acreditacion.domain.entities.EstadoRegistroCa;
import com.undec.acreditacion.domain.entities.RolClaustroCa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegistroCaDto(
        UUID id,
        UUID procesoId,
        EstadoRegistroCa estado,
        boolean coincideComisionCurricular,
        Instant solicitadoEn,
        UUID solicitadoPor,
        LocalDate fechaLimiteDc,
        Long diasRestantes,
        String semaforoTexto,
        Instant remitidoEn,
        UUID remitidoPor,
        Instant aprobadoEn,
        UUID aprobadoPor,
        String observacionesRevision,
        String resolucionStoragePath,
        String resolucionNombreOriginal,
        Long resolucionTamanioBytes,
        String registroGeneradoStoragePath,
        Instant creadoEn,
        Instant actualizadoEn,
        List<MiembroCaDto> miembros
) {
    public record MiembroCaDto(
            UUID id,
            UUID registroCaId,
            String apellido,
            String primerNombre,
            String segundoNombre,
            String dniCuil,
            String correo,
            String telefono,
            RolClaustroCa claustroRol,
            boolean esDocente,
            boolean cuentaConeauGlobalCreada,
            boolean cuentaCvarVinculada,
            int numeroOrden
    ) {}
}
