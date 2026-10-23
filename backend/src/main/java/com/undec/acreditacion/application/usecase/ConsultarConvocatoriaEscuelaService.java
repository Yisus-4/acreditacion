package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarConvocatoriaEscuelaUseCase;
import com.undec.acreditacion.application.input.ConvocatoriaCarreraItemDto;
import com.undec.acreditacion.application.input.ConvocatoriaEscuelaDetalleDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ConsultarConvocatoriaEscuelaService implements ConsultarConvocatoriaEscuelaUseCase {

    private final ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public ConsultarConvocatoriaEscuelaService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            CarreraRepositoryPort carreraRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository) {
        this.convocatoriaRepository = Objects.requireNonNull(convocatoriaRepository);
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
    }

    @Override
    public ConvocatoriaEscuelaDetalleDto obtenerPorId(UUID id) {
        Convocatoria c = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la convocatoria con ID: " + id));
        return mapearDetalleDto(c);
    }

    @Override
    public List<ConvocatoriaEscuelaDetalleDto> listarTodas() {
        return convocatoriaRepository.findAll().stream()
                .map(this::mapearDetalleDto)
                .toList();
    }

    private ConvocatoriaEscuelaDetalleDto mapearDetalleDto(Convocatoria c) {
        Optional<UnidadAcademica> escuelaOpt = unidadAcademicaRepository.findById(c.getUnidadAcademicaId());
        String escuelaNombre = escuelaOpt.map(UnidadAcademica::getNombre).orElse("Escuela Desconocida");
        String escuelaSigla = escuelaOpt.map(UnidadAcademica::getSigla).orElse("");

        List<ConvocatoriaCarreraItemDto> carrerasDto = new ArrayList<>();
        for (ConvocatoriaCarrera item : c.getCarreras()) {
            Optional<Carrera> carreraOpt = carreraRepository.findById(item.getCarreraId());
            String estadoProceso = null;
            if (item.getProcesoId() != null) {
                estadoProceso = procesoRepository.findById(item.getProcesoId())
                        .map(p -> p.getEstado().name())
                        .orElse(null);
            }

            carrerasDto.add(new ConvocatoriaCarreraItemDto(
                    item.getId(),
                    item.getCarreraId(),
                    carreraOpt.map(Carrera::getNombre).orElse("Carrera"),
                    carreraOpt.map(Carrera::getSigla).orElse(""),
                    carreraOpt.map(ca -> ca.getModalidad().name()).orElse("PRESENCIAL"),
                    carreraOpt.map(Carrera::getNivel).orElse(Carrera.NIVEL_GRADO),
                    carreraOpt.map(Carrera::getAreaDisciplinaria).orElse(""),
                    item.isSeleccionadaPorAea(),
                    item.isEsObligatoria(),
                    item.isSeleccionadaPorDe(),
                    item.getProcesoId(),
                    estadoProceso
            ));
        }

        return new ConvocatoriaEscuelaDetalleDto(
                c.getId(),
                c.getUnidadAcademicaId(),
                escuelaNombre,
                escuelaSigla,
                c.getCodigo(),
                c.getTitulo(),
                c.getTipoProceso(),
                c.getNroResolucionConeau(),
                c.getFechaInicio(),
                c.getFechaCierre(),
                c.getEstado(),
                c.getObservaciones(),
                c.getCreadoPor(),
                c.getConfirmadoPorDe(),
                c.getConfirmadoPorDeEn(),
                c.getCreadoEn(),
                c.getActualizadoEn(),
                carrerasDto
        );
    }
}
