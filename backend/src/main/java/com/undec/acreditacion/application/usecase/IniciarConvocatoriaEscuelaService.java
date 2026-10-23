package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.IniciarConvocatoriaEscuelaCommand;
import com.undec.acreditacion.application.input.IniciarConvocatoriaEscuelaUseCase;
import com.undec.acreditacion.application.input.ItemCarreraSeleccionCommand;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.Convocatoria;
import com.undec.acreditacion.domain.entities.ConvocatoriaCarrera;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class IniciarConvocatoriaEscuelaService implements IniciarConvocatoriaEscuelaUseCase {

    private final ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final CarreraRepositoryPort carreraRepository;

    public IniciarConvocatoriaEscuelaService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            CarreraRepositoryPort carreraRepository) {
        this.convocatoriaRepository = Objects.requireNonNull(convocatoriaRepository);
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
    }

    @Override
    public Convocatoria iniciar(IniciarConvocatoriaEscuelaCommand command) {
        if (command == null) {
            throw new DomainValidationException("El comando de convocatoria es requerido");
        }
        if (command.unidadAcademicaId() == null) {
            throw new DomainValidationException("Debe seleccionar una Escuela / Unidad Académica");
        }

        UnidadAcademica escuela = unidadAcademicaRepository.findById(command.unidadAcademicaId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la escuela con ID: " + command.unidadAcademicaId()));

        if (!escuela.isActivo()) {
            throw new DomainValidationException("La escuela seleccionada no se encuentra activa");
        }

        if (command.carreras() == null || command.carreras().isEmpty()) {
            throw new DomainValidationException("Debe seleccionar al menos una carrera para la convocatoria");
        }

        List<ItemCarreraSeleccionCommand> seleccionadas = command.carreras().stream()
                .filter(ItemCarreraSeleccionCommand::seleccionada)
                .toList();

        if (seleccionadas.isEmpty()) {
            throw new DomainValidationException("Debe seleccionar al menos una carrera para la convocatoria");
        }

        boolean tieneObligatoria = seleccionadas.stream().anyMatch(ItemCarreraSeleccionCommand::esObligatoria);
        if (!tieneObligatoria) {
            throw new DomainValidationException("Al menos una carrera debe seleccionarse como obligatoria para la acreditación");
        }

        UUID convocatoriaId = UUID.randomUUID();
        List<ConvocatoriaCarrera> items = new ArrayList<>();

        for (ItemCarreraSeleccionCommand item : seleccionadas) {
            Carrera carrera = carreraRepository.findById(item.carreraId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera con ID: " + item.carreraId()));

            if (!carrera.getUnidadAcademicaId().equals(escuela.getId())) {
                throw new DomainValidationException("La carrera '" + carrera.getNombre() + "' no pertenece a la escuela seleccionada");
            }
            if (!carrera.isActivo()) {
                throw new DomainValidationException("La carrera '" + carrera.getNombre() + "' no se encuentra activa");
            }

            items.add(ConvocatoriaCarrera.crear(
                    convocatoriaId,
                    carrera.getId(),
                    true,
                    item.esObligatoria()
            ));
        }

        int anio = command.fechaInicio().getYear();
        long correlativo = convocatoriaRepository.countByAnio(anio) + 1;
        String sigla = escuela.getSigla() != null && !escuela.getSigla().isBlank()
                ? escuela.getSigla().trim().toUpperCase()
                : "ESC";
        String codigoConvocatoria = String.format("CONV-%d-%s-%02d", anio, sigla, correlativo);
        String tituloConvocatoria = "Convocatoria de Acreditación - " + escuela.getNombre() + " (" + anio + ")";

        Convocatoria convocatoria = new Convocatoria(
                convocatoriaId,
                escuela.getId(),
                codigoConvocatoria,
                tituloConvocatoria,
                command.tipoProceso(),
                command.nroResolucionConeau(),
                command.fechaInicio(),
                command.fechaCierre(),
                com.undec.acreditacion.domain.entities.EstadoConvocatoria.PENDIENTE_DIRECTOR_ESCUELA,
                command.observaciones(),
                command.creadoPor(),
                null,
                null,
                null,
                null,
                items
        );

        return convocatoriaRepository.save(convocatoria);
    }
}
