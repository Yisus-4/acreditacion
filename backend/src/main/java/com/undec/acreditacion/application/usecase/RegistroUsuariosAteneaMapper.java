package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.domain.entities.EstadoRegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.UsuarioPermisoAtenea;

import java.util.ArrayList;
import java.util.List;

public final class RegistroUsuariosAteneaMapper {

    private RegistroUsuariosAteneaMapper() {}

    public static RegistroUsuariosAteneaDto toDto(RegistroUsuariosAtenea domain) {
        if (domain == null) {
            return null;
        }

        List<RegistroUsuariosAteneaDto.UsuarioPermisoAteneaDto> usuariosDto = new ArrayList<>();
        if (domain.getUsuarios() != null) {
            for (UsuarioPermisoAtenea u : domain.getUsuarios()) {
                usuariosDto.add(new RegistroUsuariosAteneaDto.UsuarioPermisoAteneaDto(
                        u.getId(),
                        u.getGrupoUsuario(),
                        u.getNombre(),
                        u.getApellido(),
                        u.getNombreCompleto(),
                        u.getDniCuil(),
                        u.getCorreo(),
                        u.getAreaDependencia(),
                        u.getCargoFuncion(),
                        u.getTipoAcceso(),
                        u.getSeccionAlcance(),
                        u.isRequiereCuentaNuevaConeauGlobal(),
                        u.isCuentaConeauGlobalCreada(),
                        u.isHabilitadoEnConeau()
                ));
            }
        }

        return new RegistroUsuariosAteneaDto(
                domain.getId(),
                domain.getProcesoId(),
                domain.getEstado(),
                domain.getFechaLimite(),
                domain.getDiasRestantes(),
                domain.getSemaforoTexto(),
                domain.getGeneradoEn(),
                domain.getAprobadoEn(),
                domain.getCreadoPor(),
                domain.getRutaDocumento(),
                domain.getEstado() == EstadoRegistroUsuariosAtenea.FINALIZADO,
                usuariosDto
        );
    }
}
