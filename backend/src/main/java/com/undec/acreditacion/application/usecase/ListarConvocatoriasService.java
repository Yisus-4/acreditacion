package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConvocatoriaFiltro;
import com.undec.acreditacion.application.input.ListarConvocatoriasUseCase;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;

import java.util.List;
import java.util.Objects;

public class ListarConvocatoriasService implements ListarConvocatoriasUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public ListarConvocatoriasService(ProcesoAcreditacionRepositoryPort procesoRepository) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
    }

    @Override
    public List<ProcesoAcreditacion> listar(ConvocatoriaFiltro filtro) {
        return procesoRepository.findAllByFiltro(filtro != null ? filtro : ConvocatoriaFiltro.vacio());
    }
}
