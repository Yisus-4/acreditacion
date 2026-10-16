package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.RegistrarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.RegistrarSedeLocalizacionUseCase;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;

import java.util.Objects;

public class RegistrarSedeLocalizacionService implements RegistrarSedeLocalizacionUseCase {

    private final SedeLocalizacionRepositoryPort sedeLocalizacionRepository;
    private final InstitucionRepositoryPort institucionRepository;

    public RegistrarSedeLocalizacionService(SedeLocalizacionRepositoryPort sedeLocalizacionRepository,
                                            InstitucionRepositoryPort institucionRepository) {
        this.sedeLocalizacionRepository = Objects.requireNonNull(sedeLocalizacionRepository, "sedeLocalizacionRepository must not be null");
        this.institucionRepository = Objects.requireNonNull(institucionRepository, "institucionRepository must not be null");
    }

    @Override
    public SedeLocalizacion registrar(RegistrarSedeLocalizacionCommand command) {
        if (command == null) {
            throw new DomainValidationException("El comando de registro no puede ser nulo");
        }
        if (command.institucionId() == null) {
            throw new DomainValidationException("El id de la institución es obligatorio");
        }
        if (institucionRepository.findById(command.institucionId()).isEmpty()) {
            throw new DomainValidationException("La institución especificada no existe");
        }

        SedeLocalizacion sede = SedeLocalizacion.registrar(
                command.institucionId(),
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
