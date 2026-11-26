package com.undec.acreditacion.infrastructure.designacion.dto;

import com.undec.acreditacion.domain.entities.ModalidadParticipacionDocente;

import java.util.UUID;

public record AgregarAsignacionCurricularRequest(
        UUID carreraId,
        UUID planEstudioId,
        UUID actividadCurricularId,
        ModalidadParticipacionDocente modalidadParticipacion,
        Integer horasAsignadas,
        String observaciones,
        String motivoCambio
) {
    public AgregarAsignacionCurricularRequest(
            UUID carreraId,
            UUID planEstudioId,
            UUID actividadCurricularId,
            ModalidadParticipacionDocente modalidadParticipacion,
            Integer horasAsignadas,
            String observaciones
    ) {
        this(carreraId, planEstudioId, actividadCurricularId, modalidadParticipacion, horasAsignadas, observaciones, null);
    }
}
