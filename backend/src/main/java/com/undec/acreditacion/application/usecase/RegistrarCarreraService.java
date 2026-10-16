package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.RegistrarCarreraCommand;
import com.undec.acreditacion.application.input.RegistrarCarreraUseCase;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;

import java.util.Objects;

public class RegistrarCarreraService implements RegistrarCarreraUseCase {

    private final CarreraRepositoryPort carreraRepository;
    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final SedeLocalizacionRepositoryPort sedeLocalizacionRepository;

    public RegistrarCarreraService(CarreraRepositoryPort carreraRepository,
                                   UnidadAcademicaRepositoryPort unidadAcademicaRepository,
                                   SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        this.carreraRepository = Objects.requireNonNull(carreraRepository, "carreraRepository must not be null");
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository, "unidadAcademicaRepository must not be null");
        this.sedeLocalizacionRepository = Objects.requireNonNull(sedeLocalizacionRepository, "sedeLocalizacionRepository must not be null");
    }

    @Override
    public Carrera registrar(RegistrarCarreraCommand command) {
        if (command == null) {
            throw new DomainValidationException("El comando de registro no puede ser nulo");
        }
        if (command.unidadAcademicaId() == null) {
            throw new DomainValidationException("La unidad académica es obligatoria");
        }
        if (unidadAcademicaRepository.findById(command.unidadAcademicaId()).isEmpty()) {
            throw new DomainValidationException("La unidad académica especificada no existe");
        }
        if (command.nombre() != null && carreraRepository.existsByUnidadAcademicaIdAndNombre(command.unidadAcademicaId(), command.nombre().trim())) {
            throw new DomainValidationException("Ya existe una carrera con ese nombre en la unidad académica");
        }

        if (command.sedesIds() != null && !command.sedesIds().isEmpty()) {
            var sedesEncontradas = sedeLocalizacionRepository.findByIds(command.sedesIds());
            if (sedesEncontradas.size() != command.sedesIds().size()) {
                throw new DomainValidationException("Una o más sedes asociadas no existen");
            }
        }

        Carrera carrera = Carrera.registrar(
                command.unidadAcademicaId(),
                command.nombre(),
                command.sigla(),
                command.areaDisciplinaria(),
                command.modalidad(),
                command.estado(),
                command.tituloOtorgado(),
                command.duracionAnios(),
                command.descripcion(),
                command.sedesIds()
        );

        return carreraRepository.save(carrera);
    }
}
