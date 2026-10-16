package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ListarSedesLocalizacionesUseCase;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ListarSedesLocalizacionesService implements ListarSedesLocalizacionesUseCase {

    private final SedeLocalizacionRepositoryPort sedeLocalizacionRepository;

    public ListarSedesLocalizacionesService(SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        this.sedeLocalizacionRepository = Objects.requireNonNull(sedeLocalizacionRepository, "sedeLocalizacionRepository must not be null");
    }

    @Override
    public List<SedeLocalizacion> listarTodas() {
        return sedeLocalizacionRepository.findAll();
    }

    @Override
    public List<SedeLocalizacion> listarPorInstitucion(UUID institucionId) {
        if (institucionId == null) {
            return sedeLocalizacionRepository.findAll();
        }
        return sedeLocalizacionRepository.findByInstitucionId(institucionId);
    }
}
