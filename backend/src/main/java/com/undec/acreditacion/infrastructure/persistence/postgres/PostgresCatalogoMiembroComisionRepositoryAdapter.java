package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.CatalogoMiembroComision;
import com.undec.acreditacion.domain.ports.CatalogoMiembroComisionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresCatalogoMiembroComisionRepositoryAdapter implements CatalogoMiembroComisionRepositoryPort {

    private final SpringDataCatalogoMiembroComisionRepository springDataRepository;

    public PostgresCatalogoMiembroComisionRepositoryAdapter(SpringDataCatalogoMiembroComisionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<CatalogoMiembroComision> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CatalogoMiembroComision> findByDocumento(String documento) {
        return springDataRepository.findByDocumento(documento).map(this::toDomain);
    }

    @Override
    public Optional<CatalogoMiembroComision> findByCuitPasaporte(String cuitPasaporte) {
        return springDataRepository.findByCuitPasaporte(cuitPasaporte).map(this::toDomain);
    }

    @Override
    public List<CatalogoMiembroComision> buscar(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return springDataRepository.findAll().stream().map(this::toDomain).toList();
        }
        return springDataRepository.buscar(busqueda.trim()).stream().map(this::toDomain).toList();
    }

    @Override
    public CatalogoMiembroComision save(CatalogoMiembroComision miembro) {
        CatalogoMiembroComisionJpaEntity entity = toEntity(miembro);
        CatalogoMiembroComisionJpaEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    private CatalogoMiembroComision toDomain(CatalogoMiembroComisionJpaEntity entity) {
        if (entity == null) return null;
        return new CatalogoMiembroComision(
                entity.getId(),
                entity.getApellido(),
                entity.getNombreCompleto(),
                entity.getDocumento(),
                entity.getCuitPasaporte(),
                entity.getCorreoElectronico(),
                entity.getCorreoGmail(),
                entity.getCargoFuncion(),
                entity.getCreadoEn()
        );
    }

    private CatalogoMiembroComisionJpaEntity toEntity(CatalogoMiembroComision domain) {
        if (domain == null) return null;
        CatalogoMiembroComisionJpaEntity entity = new CatalogoMiembroComisionJpaEntity();
        entity.setId(domain.getId());
        entity.setApellido(domain.getApellido());
        entity.setNombreCompleto(domain.getNombreCompleto());
        entity.setDocumento(domain.getDocumento());
        entity.setCuitPasaporte(domain.getCuitPasaporte());
        entity.setCorreoElectronico(domain.getCorreoElectronico());
        entity.setCorreoGmail(domain.getCorreoGmail());
        entity.setCargoFuncion(domain.getCargoFuncion());
        entity.setCreadoEn(domain.getCreadoEn());
        return entity;
    }
}
