package com.undec.acreditacion.infrastructure.designacion.dto;

import com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion;

import java.util.UUID;

public record CrearSolicitudDesignacionRequest(
        UUID departamentoAcademicoId,
        UUID periodoAcademicoId,
        TipoSolicitudDesignacion tipoSolicitud,
        String observacionesDepartamento
) {}
