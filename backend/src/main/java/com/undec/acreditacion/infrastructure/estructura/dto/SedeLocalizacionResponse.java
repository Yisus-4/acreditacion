package com.undec.acreditacion.infrastructure.estructura.dto;

import com.undec.acreditacion.domain.entities.SedeLocalizacion;

import java.time.Instant;
import java.util.UUID;

public record SedeLocalizacionResponse(
        UUID id,
        UUID institucionId,
        String nombre,
        String calle,
        String numero,
        String piso,
        String codigoPostal,
        Long provinciaId,
        Long departamentoId,
        Long localidadId,
        boolean esPropia,
        boolean activo,
        String direccionCompleta,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public static SedeLocalizacionResponse fromDomain(SedeLocalizacion domain) {
        if (domain == null) return null;
        return new SedeLocalizacionResponse(
                domain.getId(),
                domain.getInstitucionId(),
                domain.getNombre(),
                domain.getCalle(),
                domain.getNumero(),
                domain.getPiso(),
                domain.getCodigoPostal(),
                domain.getProvinciaId(),
                domain.getDepartamentoId(),
                domain.getLocalidadId(),
                domain.isEsPropia(),
                domain.isActivo(),
                domain.getDireccionCompleta(),
                domain.getCreadoEn(),
                domain.getActualizadoEn()
        );
    }
}
