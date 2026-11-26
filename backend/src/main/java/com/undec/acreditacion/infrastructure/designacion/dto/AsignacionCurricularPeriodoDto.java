package com.undec.acreditacion.infrastructure.designacion.dto;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.domain.entities.ModalidadParticipacionDocente;

import java.util.UUID;

public record AsignacionCurricularPeriodoDto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        String carreraSigla,
        UUID planEstudioId,
        String planEstudioNombre,
        EstadoVigenciaPlan planEstadoVigencia,
        UUID actividadCurricularId,
        String actividadCodigo,
        String actividadNombre,
        EstadoDictadoActividad actividadEstadoDictado,
        Integer actividadHorasSemanales,
        ModalidadParticipacionDocente modalidadParticipacion,
        Integer horasAsignadas,
        String observaciones
) {}
