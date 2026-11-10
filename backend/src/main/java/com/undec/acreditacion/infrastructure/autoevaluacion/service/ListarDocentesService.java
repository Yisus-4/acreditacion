package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.infrastructure.autoevaluacion.dto.DocenteResumenResponse;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUnidadAcademicaConeauRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUserRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UnidadAcademicaConeauJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.UserJpaEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListarDocentesService {

    private final SpringDataUserRepository userRepository;
    private final SpringDataUnidadAcademicaConeauRepository unidadAcademicaRepository;

    public ListarDocentesService(
            SpringDataUserRepository userRepository,
            SpringDataUnidadAcademicaConeauRepository unidadAcademicaRepository
    ) {
        this.userRepository = userRepository;
        this.unidadAcademicaRepository = unidadAcademicaRepository;
    }

    @Transactional(readOnly = true)
    public List<DocenteResumenResponse> listarDocentes(String query) {
        List<UserJpaEntity> docentes = userRepository.findDocentesActivos(query);

        List<UUID> uaIds = docentes.stream()
                .map(UserJpaEntity::getUnidadAcademicaId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> uaNombres = unidadAcademicaRepository.findAllById(uaIds).stream()
                .collect(Collectors.toMap(
                        UnidadAcademicaConeauJpaEntity::getId,
                        UnidadAcademicaConeauJpaEntity::getDenominacion,
                        (existing, replacement) -> existing
                ));

        return docentes.stream()
                .map(d -> {
                    String uaNombre = d.getUnidadAcademicaId() != null ?
                            uaNombres.getOrDefault(d.getUnidadAcademicaId(), null) : null;
                    if (uaNombre == null && d.getUnidadAcademicaId() != null) {
                        uaNombre = unidadAcademicaRepository.findById(d.getUnidadAcademicaId())
                                .map(UnidadAcademicaConeauJpaEntity::getDenominacion)
                                .orElse(null);
                    }
                    return DocenteResumenResponse.fromUser(d, uaNombre);
                })
                .collect(Collectors.toList());
    }
}
