package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.GuardarItemMatrizPermisoInputDto;
import com.undec.acreditacion.application.input.GuardarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.input.GuardarUsuarioPermisoAteneaInputDto;
import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.ItemMatrizPermisoAtenea;
import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.UsuarioPermisoAtenea;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroUsuariosAteneaRepositoryPort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuardarRegistroUsuariosAteneaService implements GuardarRegistroUsuariosAteneaUseCase {

    private final RegistroUsuariosAteneaRepositoryPort repository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    public GuardarRegistroUsuariosAteneaService(RegistroUsuariosAteneaRepositoryPort repository) {
        this(repository, null);
    }

    public GuardarRegistroUsuariosAteneaService(
            RegistroUsuariosAteneaRepositoryPort repository,
            PlanTrabajoRepositoryPort planTrabajoRepository
    ) {
        this.repository = repository;
        this.planTrabajoRepository = planTrabajoRepository;
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

    @Override
    public RegistroUsuariosAteneaDto actualizarPlazo(UUID procesoId, int diasHabiles) {
        RegistroUsuariosAtenea registro = repository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró registro de usuarios de ATENEA para el proceso: " + procesoId));
        registro.actualizarPlazoDiasHabiles(diasHabiles);
        RegistroUsuariosAtenea guardado = repository.save(registro);
        return RegistroUsuariosAteneaMapper.toDto(guardado);
    }

    @Override
    public RegistroUsuariosAteneaDto extenderPlazoFecha(UUID procesoId, LocalDate nuevaFechaLimite) {
        RegistroUsuariosAtenea registro = repository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró registro de usuarios de ATENEA para el proceso: " + procesoId));
        registro.extenderPlazoFecha(nuevaFechaLimite);
        RegistroUsuariosAtenea guardado = repository.save(registro);

        if (planTrabajoRepository != null) {
            planTrabajoRepository.findByProcesoId(procesoId).ifPresent(plan -> {
                for (ActividadPlanTrabajo act : plan.getActividades()) {
                    String cod = act.getCodigoActividad() != null ? act.getCodigoActividad().trim() : "";
                    String nom = act.getNombre() != null ? act.getNombre().toLowerCase() : "";
                    if ("3.3".equals(cod) || nom.contains("atenea")) {
                        act.agregarPlazoExtendido(nuevaFechaLimite);
                        planTrabajoRepository.saveActividad(act);
                    }
                }
            });
        }

        return RegistroUsuariosAteneaMapper.toDto(guardado);
    }
}
