package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.ItemMatrizPermisoAtenea;
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

        List<ItemMatrizPermisoAtenea> matrizDomain = new ArrayList<>();
        if (entity.getMatrizPermisos() != null) {
            for (ItemMatrizPermisoAteneaJpaEntity me : entity.getMatrizPermisos()) {
                matrizDomain.add(ItemMatrizPermisoAtenea.rehydrate(
                        me.getId(),
                        entity.getId(),
                        me.getProcesoActividad(),
                        me.getItem(),
                        me.getSeccion(),
                        me.getTipoPermiso(),
                        me.isPermisoSgiAea(),
                        me.isPermisoCa(),
                        me.getOrden(),
                        me.getCreadoEn(),
                        me.getActualizadoEn()
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
                usuariosDomain,
                matrizDomain
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

        List<ItemMatrizPermisoAteneaJpaEntity> matrizEntities = new ArrayList<>();
        if (domain.getMatrizPermisos() != null) {
            for (ItemMatrizPermisoAtenea md : domain.getMatrizPermisos()) {
                ItemMatrizPermisoAteneaJpaEntity me = new ItemMatrizPermisoAteneaJpaEntity();
                me.setId(md.getId());
                me.setRegistro(entity);
                me.setProcesoActividad(md.getProcesoActividad());
                me.setItem(md.getItem());
                me.setSeccion(md.getSeccion());
                me.setTipoPermiso(md.getTipoPermiso());
                me.setPermisoSgiAea(md.isPermisoSgiAea());
                me.setPermisoCa(md.isPermisoCa());
                me.setOrden(md.getOrden());
                me.setCreadoEn(md.getCreadoEn());
                me.setActualizadoEn(md.getActualizadoEn());
                matrizEntities.add(me);
            }
        }
        entity.setMatrizPermisos(matrizEntities);

        return entity;
    }
}
