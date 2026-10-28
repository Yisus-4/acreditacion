package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.UsuarioPermisoAtenea;

import java.util.ArrayList;
import java.util.List;

public final class RegistroUsuariosAteneaPersistenceMapper {

    private RegistroUsuariosAteneaPersistenceMapper() {}

    public static RegistroUsuariosAtenea toDomain(RegistroUsuariosAteneaJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<UsuarioPermisoAtenea> usuariosDomain = new ArrayList<>();
        if (entity.getUsuarios() != null) {
            for (UsuarioPermisoAteneaJpaEntity ue : entity.getUsuarios()) {
                usuariosDomain.add(UsuarioPermisoAtenea.rehydrate(
                        ue.getId(),
                        entity.getId(),
                        ue.getGrupoUsuario(),
                        ue.getNombre(),
                        ue.getApellido(),
                        ue.getDniCuil(),
                        ue.getCorreo(),
                        ue.getAreaDependencia(),
                        ue.getCargoFuncion(),
                        ue.getTipoAcceso(),
                        ue.getSeccionAlcance(),
                        ue.isRequiereCuentaNuevaConeauGlobal(),
                        ue.isCuentaConeauGlobalCreada(),
                        ue.isHabilitadoEnConeau(),
                        ue.getCreadoEn(),
                        ue.getActualizadoEn()
                ));
            }
        }

        return RegistroUsuariosAtenea.rehydrate(
                entity.getId(),
                entity.getProcesoId(),
                entity.getEstado(),
                entity.getFechaLimite(),
                entity.getGeneradoEn(),
                entity.getAprobadoEn(),
                entity.getCreadoPor(),
                entity.getRutaDocumento(),
                entity.getCreadoEn(),
                entity.getActualizadoEn(),
                usuariosDomain
        );
    }

    public static RegistroUsuariosAteneaJpaEntity toEntity(RegistroUsuariosAtenea domain) {
        if (domain == null) {
            return null;
        }

        RegistroUsuariosAteneaJpaEntity entity = new RegistroUsuariosAteneaJpaEntity();
        entity.setId(domain.getId());
        entity.setProcesoId(domain.getProcesoId());
        entity.setEstado(domain.getEstado());
        entity.setFechaLimite(domain.getFechaLimite());
        entity.setGeneradoEn(domain.getGeneradoEn());
        entity.setAprobadoEn(domain.getAprobadoEn());
        entity.setCreadoPor(domain.getCreadoPor());
        entity.setRutaDocumento(domain.getRutaDocumento());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());

        List<UsuarioPermisoAteneaJpaEntity> usuariosEntities = new ArrayList<>();
        if (domain.getUsuarios() != null) {
            for (UsuarioPermisoAtenea ud : domain.getUsuarios()) {
                UsuarioPermisoAteneaJpaEntity ue = new UsuarioPermisoAteneaJpaEntity();
                ue.setId(ud.getId());
                ue.setRegistro(entity);
                ue.setGrupoUsuario(ud.getGrupoUsuario());
                ue.setNombre(ud.getNombre());
                ue.setApellido(ud.getApellido());
                ue.setDniCuil(ud.getDniCuil());
                ue.setCorreo(ud.getCorreo());
                ue.setAreaDependencia(ud.getAreaDependencia());
                ue.setCargoFuncion(ud.getCargoFuncion());
                ue.setTipoAcceso(ud.getTipoAcceso());
                ue.setSeccionAlcance(ud.getSeccionAlcance());
                ue.setRequiereCuentaNuevaConeauGlobal(ud.isRequiereCuentaNuevaConeauGlobal());
                ue.setCuentaConeauGlobalCreada(ud.isCuentaConeauGlobalCreada());
                ue.setHabilitadoEnConeau(ud.isHabilitadoEnConeau());
                ue.setCreadoEn(ud.getCreadoEn());
                ue.setActualizadoEn(ud.getActualizadoEn());
                usuariosEntities.add(ue);
            }
        }
        entity.setUsuarios(usuariosEntities);

        return entity;
    }
}
