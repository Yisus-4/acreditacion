package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;

import java.util.ArrayList;
import java.util.List;

public final class RegistroCaPersistenceMapper {

    private RegistroCaPersistenceMapper() {}

    public static RegistroConformacionCa toDomain(RegistroConformacionCaJpaEntity entity) {
        if (entity == null) return null;

        List<MiembroComisionAutoevaluacion> miembros = entity.getMiembros().stream()
                .map(m -> new MiembroComisionAutoevaluacion(
                        m.getId(),
                        entity.getId(),
                        m.getApellido(),
                        m.getPrimerNombre(),
                        m.getSegundoNombre(),
                        m.getDniCuil(),
                        m.getCuitPasaporte(),
                        m.getCorreo(),
                        m.getCorreoGmail(),
                        m.getCargoFuncion(),
                        m.getTelefono(),
                        m.getClaustroRol(),
                        m.isEsDocente(),
                        m.isCuentaConeauGlobalCreada(),
                        m.isCuentaCvarVinculada(),
                        m.isPendienteAltaSistema(),
                        m.getNumeroOrden()
                ))
                .toList();

        return new RegistroConformacionCa(
                entity.getId(),
                entity.getProcesoId(),
                entity.getEstado(),
                entity.isCoincideComisionCurricular(),
                entity.getSolicitadoEn(),
                entity.getSolicitadoPor(),
                entity.getFechaLimiteDc(),
                entity.getRemitidoEn(),
                entity.getRemitidoPor(),
                entity.getAprobadoEn(),
                entity.getAprobadoPor(),
                entity.getObservacionesRevision(),
                entity.getResolucionStoragePath(),
                entity.getResolucionNombreOriginal(),
                entity.getResolucionTamanioBytes(),
                entity.getRegistroGeneradoStoragePath(),
                entity.getCreadoEn(),
                entity.getActualizadoEn(),
                miembros
        );
    }

    public static RegistroConformacionCaJpaEntity toEntity(RegistroConformacionCa domain) {
        if (domain == null) return null;

        RegistroConformacionCaJpaEntity entity = new RegistroConformacionCaJpaEntity();
        entity.setId(domain.getId());
        entity.setProcesoId(domain.getProcesoId());
        entity.setEstado(domain.getEstado());
        entity.setCoincideComisionCurricular(domain.isCoincideComisionCurricular());
        entity.setSolicitadoEn(domain.getSolicitadoEn());
        entity.setSolicitadoPor(domain.getSolicitadoPor());
        entity.setFechaLimiteDc(domain.getFechaLimiteDc());
        entity.setRemitidoEn(domain.getRemitidoEn());
        entity.setRemitidoPor(domain.getRemitidoPor());
        entity.setAprobadoEn(domain.getAprobadoEn());
        entity.setAprobadoPor(domain.getAprobadoPor());
        entity.setObservacionesRevision(domain.getObservacionesRevision());
        entity.setResolucionStoragePath(domain.getResolucionStoragePath());
        entity.setResolucionNombreOriginal(domain.getResolucionNombreOriginal());
        entity.setResolucionTamanioBytes(domain.getResolucionTamanioBytes());
        entity.setRegistroGeneradoStoragePath(domain.getRegistroGeneradoStoragePath());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());

        List<MiembroComisionAutoevaluacionJpaEntity> miembrosEntity = new ArrayList<>();
        for (MiembroComisionAutoevaluacion m : domain.getMiembros()) {
            MiembroComisionAutoevaluacionJpaEntity me = new MiembroComisionAutoevaluacionJpaEntity();
            me.setId(m.getId());
            me.setRegistroCa(entity);
            me.setApellido(m.getApellido());
            me.setPrimerNombre(m.getPrimerNombre());
            me.setSegundoNombre(m.getSegundoNombre());
            me.setDniCuil(m.getDniCuil());
            me.setCuitPasaporte(m.getCuitPasaporte());
            me.setCorreo(m.getCorreo());
            me.setCorreoGmail(m.getCorreoGmail());
            me.setCargoFuncion(m.getCargoFuncion());
            me.setTelefono(m.getTelefono());
            me.setClaustroRol(m.getClaustroRol());
            me.setEsDocente(m.isEsDocente());
            me.setCuentaConeauGlobalCreada(m.isCuentaConeauGlobalCreada());
            me.setCuentaCvarVinculada(m.isCuentaCvarVinculada());
            me.setPendienteAltaSistema(m.isPendienteAltaSistema());
            me.setNumeroOrden(m.getNumeroOrden());
            me.setCreadoEn(domain.getActualizadoEn() != null ? domain.getActualizadoEn() : java.time.Instant.now());
            miembrosEntity.add(me);
        }
        entity.setMiembros(miembrosEntity);

        return entity;
    }
}
