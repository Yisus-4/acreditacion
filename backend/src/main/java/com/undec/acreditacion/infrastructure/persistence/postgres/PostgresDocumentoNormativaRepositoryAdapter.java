package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.DocumentoNormativa;
import com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresDocumentoNormativaRepositoryAdapter implements DocumentoNormativaRepositoryPort {

    private final SpringDataDocumentoNormativaRepository repository;
    private final NormativaPersistenceMapper mapper;

    public PostgresDocumentoNormativaRepositoryAdapter(
        SpringDataDocumentoNormativaRepository repository,
        NormativaPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public DocumentoNormativa save(DocumentoNormativa documento) {
        DocumentoNormativaJpaEntity entity = mapper.toJpa(documento);
        DocumentoNormativaJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<DocumentoNormativa> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DocumentoNormativa> findBySolicitudNormativaId(UUID solicitudId) {
        return repository.findBySolicitudNormativaIdOrderByCreadoEnAsc(solicitudId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<DocumentoNormativa> findByProcesoId(UUID procesoId) {
        return repository.findByProcesoIdOrderByCreadoEnAsc(procesoId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void delete(DocumentoNormativa documento) {
        if (documento != null && documento.getId() != null) {
            repository.deleteById(documento.getId());
        }
    }

    @Override
    public void deleteById(UUID id) {
        if (id != null) {
            repository.deleteById(id);
        }
    }
}
