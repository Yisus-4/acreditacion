package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.GuardarItemMatrizPermisoInputDto;
import com.undec.acreditacion.application.input.GuardarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.input.GuardarUsuarioPermisoAteneaInputDto;
import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.domain.entities.ItemMatrizPermisoAtenea;
import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.UsuarioPermisoAtenea;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.RegistroUsuariosAteneaRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuardarRegistroUsuariosAteneaService implements GuardarRegistroUsuariosAteneaUseCase {

    private final RegistroUsuariosAteneaRepositoryPort repository;

    public GuardarRegistroUsuariosAteneaService(RegistroUsuariosAteneaRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public RegistroUsuariosAteneaDto guardar(UUID procesoId, UUID usuarioId, List<GuardarUsuarioPermisoAteneaInputDto> usuariosInput) {
        return guardar(procesoId, usuarioId, usuariosInput, null);
    }

    @Override
    public RegistroUsuariosAteneaDto guardar(
            UUID procesoId,
            UUID usuarioId,
            List<GuardarUsuarioPermisoAteneaInputDto> usuariosInput,
            List<GuardarItemMatrizPermisoInputDto> matrizInput
    ) {
        RegistroUsuariosAtenea registro = repository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró registro de usuarios de ATENEA para el proceso: " + procesoId));

        if (usuariosInput != null) {
            List<UsuarioPermisoAtenea> lista = new ArrayList<>();
            for (GuardarUsuarioPermisoAteneaInputDto in : usuariosInput) {
                UsuarioPermisoAtenea u = UsuarioPermisoAtenea.crear(
                        in.grupoUsuario(),
                        in.nombre(),
                        in.apellido(),
                        in.dniCuil(),
                        in.correo(),
                        in.areaDependencia(),
                        in.cargoFuncion(),
                        in.tipoAcceso(),
                        in.seccionAlcance(),
                        in.requiereCuentaNuevaConeauGlobal(),
                        in.cuentaConeauGlobalCreada(),
                        in.habilitadoEnConeau()
                );
                if (in.id() != null) {
                    u.setId(in.id());
                }
                lista.add(u);
            }
            registro.actualizarUsuarios(lista);
        }

        if (matrizInput != null && !matrizInput.isEmpty()) {
            List<ItemMatrizPermisoAtenea> itemsMatriz = new ArrayList<>();
            for (GuardarItemMatrizPermisoInputDto min : matrizInput) {
                ItemMatrizPermisoAtenea item = ItemMatrizPermisoAtenea.crear(
                        min.procesoActividad(),
                        min.item(),
                        min.seccion(),
                        min.tipoPermiso(),
                        min.permisoSgiAea(),
                        min.permisoCa(),
                        min.orden()
                );
                if (min.id() != null) {
                    item.setId(min.id());
                }
                itemsMatriz.add(item);
            }
            registro.actualizarMatrizPermisos(itemsMatriz);
        } else {
            registro.inicializarMatrizPorDefectoSiVacia();
        }

        RegistroUsuariosAtenea guardado = repository.save(registro);
        return RegistroUsuariosAteneaMapper.toDto(guardado);
    }
}
