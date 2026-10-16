package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ActualizarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.ActualizarSedeLocalizacionUseCase;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;

import java.util.Objects;

public final class ActualizarSedeLocalizacionService implements ActualizarSedeLocalizacionUseCase {

    private final SedeLocalizacionRepositoryPort sedeLocalizacionRepository;

    public ActualizarSedeLocalizacionService(SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        this.sedeLocalizacionRepository = Objects.requireNonNull(sedeLocalizacionRepository, "El repositorio de sedes es obligatorio");
    }

    @Override
    public SedeLocalizacion actualizar(ActualizarSedeLocalizacionCommand command) {
        Objects.requireNonNull(command, "El comando de actualización es obligatorio");
        Objects.requireNonNull(command.id(), "El ID de la sede es obligatorio");

        SedeLocalizacion sede = sedeLocalizacionRepository.findById(command.id())
                .orElseThrow(() -> new DomainValidationException("No se encontró la sede con ID: " + command.id()));

        sede.actualizar(
                command.nombre(),
                command.calle(),
                command.numero(),
                command.piso(),
                command.codigoPostal(),
                command.provinciaId(),
                command.departamentoId(),
                command.localidadId(),
                command.esPropia()
        );

        return sedeLocalizacionRepository.save(sede);
    }
}
