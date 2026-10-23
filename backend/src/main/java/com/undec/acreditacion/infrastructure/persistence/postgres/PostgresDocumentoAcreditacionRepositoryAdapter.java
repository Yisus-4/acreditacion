package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PostgresDocumentoAcreditacionRepositoryAdapter implements DocumentoAcreditacionRepositoryPort {

    private final SpringDataDocumentoAcreditacionRepository springDataRepository;
    private final DocumentoAcreditacionPersistenceMapper mapper;

    public PostgresDocumentoAcreditacionRepositoryAdapter(
            SpringDataDocumentoAcreditacionRepository springDataRepository,
            DocumentoAcreditacionPersistenceMapper mapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public DocumentoAcreditacion save(DocumentoAcreditacion doc) {
        DocumentoAcreditacionJpaEntity entity = mapper.toJpa(doc);
        DocumentoAcreditacionJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<DocumentoAcreditacion> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DocumentoAcreditacion> findByActividadId(UUID actividadId) {
        return springDataRepository.findByActividadId(actividadId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }
}
