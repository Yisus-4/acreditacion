package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.RegistrarUnidadAcademicaCommand;
import com.undec.acreditacion.application.input.RegistrarUnidadAcademicaUseCase;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;

import java.util.Objects;

public class RegistrarUnidadAcademicaService implements RegistrarUnidadAcademicaUseCase {

    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final InstitucionRepositoryPort institucionRepository;

    public RegistrarUnidadAcademicaService(UnidadAcademicaRepositoryPort unidadAcademicaRepository,
                                           InstitucionRepositoryPort institucionRepository) {
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository, "unidadAcademicaRepository must not be null");
        this.institucionRepository = Objects.requireNonNull(institucionRepository, "institucionRepository must not be null");
    }

    @Override
    public UnidadAcademica registrar(RegistrarUnidadAcademicaCommand command) {
        if (command == null) {
            throw new DomainValidationException("El comando de registro no puede ser nulo");
        }
        if (command.institucionId() == null) {
            throw new DomainValidationException("El id de la institución es obligatorio");
        }
        if (institucionRepository.findById(command.institucionId()).isEmpty()) {
            throw new DomainValidationException("La institución especificada no existe");
        }
        if (command.nombre() != null && unidadAcademicaRepository.existsByInstitucionIdAndNombre(command.institucionId(), command.nombre().trim())) {
            throw new DomainValidationException("Ya existe una unidad académica con ese nombre en la institución");
        }

        UnidadAcademica unidadAcademica = UnidadAcademica.registrar(
                command.institucionId(),
                command.nombre(),
                command.sigla(),
                command.descripcion()
        );

        return unidadAcademicaRepository.save(unidadAcademica);
    }
}
