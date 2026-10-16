package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.application.input.ConvocatoriaFiltro;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcesoAcreditacionRepositoryPort {

    ProcesoAcreditacion save(ProcesoAcreditacion proceso);

    Optional<ProcesoAcreditacion> findById(UUID id);

    Optional<ProcesoAcreditacion> findActivoByCarreraId(UUID carreraId);

    List<ProcesoAcreditacion> findAllByFiltro(ConvocatoriaFiltro filtro);

    long countByAnio(int anio);
}
