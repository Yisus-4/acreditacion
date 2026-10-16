package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ActualizarCarreraCommand;
import com.undec.acreditacion.application.input.ActualizarCarreraUseCase;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;

import java.util.List;
import java.util.Objects;

public final class ActualizarCarreraService implements ActualizarCarreraUseCase {

    private final CarreraRepositoryPort carreraRepository;
    private final SedeLocalizacionRepositoryPort sedeLocalizacionRepository;

    public ActualizarCarreraService(CarreraRepositoryPort carreraRepository,
                                   SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        this.carreraRepository = Objects.requireNonNull(carreraRepository, "El repositorio de carreras es obligatorio");
        this.sedeLocalizacionRepository = Objects.requireNonNull(sedeLocalizacionRepository, "El repositorio de sedes es obligatorio");
    }

    @Override
    public Carrera actualizar(ActualizarCarreraCommand command) {
        Objects.requireNonNull(command, "El comando de actualización es obligatorio");
        Objects.requireNonNull(command.id(), "El ID de la carrera es obligatorio");

        Carrera carrera = carreraRepository.findById(command.id())
                .orElseThrow(() -> new DomainValidationException("No se encontró la carrera con ID: " + command.id()));

        if (!carrera.getNombre().equalsIgnoreCase(command.nombre().trim())) {
            if (carreraRepository.existsByUnidadAcademicaIdAndNombre(carrera.getUnidadAcademicaId(), command.nombre().trim())) {
                throw new DomainValidationException("Ya existe una carrera con el nombre '" + command.nombre() + "' en la misma unidad académica");
            }
        }

        if (command.sedesIds() != null && !command.sedesIds().isEmpty()) {
            List<SedeLocalizacion> sedes = sedeLocalizacionRepository.findByIds(command.sedesIds());
            if (sedes.size() != command.sedesIds().size()) {
                throw new DomainValidationException("Una o más sedes especificadas no existen en el sistema");
            }
        }

        carrera.actualizar(
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

        if (command.activo() != null) {
            carrera.cambiarEstado(command.activo());
        }

        return carreraRepository.save(carrera);
    }
}
