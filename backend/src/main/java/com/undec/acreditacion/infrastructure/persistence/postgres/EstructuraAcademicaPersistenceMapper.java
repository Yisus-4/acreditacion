package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.EstadoCarrera;
import com.undec.acreditacion.domain.entities.ModalidadCarrera;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EstructuraAcademicaPersistenceMapper {

    public UnidadAcademica toDomain(UnidadAcademicaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return UnidadAcademica.rehydrate(
                entity.getId(),
                entity.getInstitucion() != null ? entity.getInstitucion().getId() : null,
                entity.getNombre(),
                entity.getSigla(),
                entity.getDescripcion(),
                entity.isActivo(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    public UnidadAcademicaJpaEntity toJpa(UnidadAcademica domain, InstitucionJpaEntity institucion) {
        if (domain == null) {
            return null;
        }
        UnidadAcademicaJpaEntity entity = new UnidadAcademicaJpaEntity();
        entity.setId(domain.getId());
        entity.setInstitucion(institucion);
        entity.setNombre(domain.getNombre());
        entity.setSigla(domain.getSigla());
        entity.setDescripcion(domain.getDescripcion());
        entity.setActivo(domain.isActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }

    public SedeLocalizacion toDomain(SedeLocalizacionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return SedeLocalizacion.rehydrate(
                entity.getId(),
                entity.getInstitucion() != null ? entity.getInstitucion().getId() : null,
                entity.getNombre(),
                entity.getCalle(),
                entity.getNumero(),
                entity.getPiso(),
                entity.getCodigoPostal(),
                entity.getProvincia() != null ? entity.getProvincia().getId() : null,
                entity.getDepartamentoGeografico() != null ? entity.getDepartamentoGeografico().getId() : null,
                entity.getLocalidad() != null ? entity.getLocalidad().getId() : null,
                entity.isEsPropia(),
                entity.isActivo(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    public SedeLocalizacionJpaEntity toJpa(SedeLocalizacion domain,
                                          InstitucionJpaEntity institucion,
                                          ProvinciaJpaEntity provincia,
                                          DepartamentoJpaEntity departamento,
                                          LocalidadJpaEntity localidad) {
        if (domain == null) {
            return null;
        }
        SedeLocalizacionJpaEntity entity = new SedeLocalizacionJpaEntity();
        entity.setId(domain.getId());
        entity.setInstitucion(institucion);
        entity.setNombre(domain.getNombre());
        entity.setCalle(domain.getCalle());
        entity.setNumero(domain.getNumero());
        entity.setPiso(domain.getPiso());
        entity.setCodigoPostal(domain.getCodigoPostal());
        entity.setProvincia(provincia);
        entity.setDepartamentoGeografico(departamento);
        entity.setLocalidad(localidad);
        entity.setEsPropia(domain.isEsPropia());
        entity.setActivo(domain.isActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        return entity;
    }

    public Carrera toDomain(CarreraJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        List<java.util.UUID> sedesIds = entity.getSedes() != null
                ? entity.getSedes().stream().map(SedeLocalizacionJpaEntity::getId).toList()
                : Collections.emptyList();

        return Carrera.rehydrate(
                entity.getId(),
                entity.getUnidadAcademica() != null ? entity.getUnidadAcademica().getId() : null,
                entity.getNombre(),
                entity.getSigla(),
                entity.getNivel(),
                entity.getAreaDisciplinaria(),
                ModalidadCarrera.valueOf(entity.getModalidad()),
                EstadoCarrera.valueOf(entity.getEstado()),
                entity.getTituloOtorgado(),
                entity.getDuracionAnios(),
                entity.getDescripcion(),
                entity.isActivo(),
                entity.getCreadoEn(),
                entity.getActualizadoEn(),
                sedesIds
        );
    }

    public CarreraJpaEntity toJpa(Carrera domain,
                                  UnidadAcademicaJpaEntity unidadAcademica,
                                  Set<SedeLocalizacionJpaEntity> sedes) {
        if (domain == null) {
            return null;
        }
        CarreraJpaEntity entity = new CarreraJpaEntity();
        entity.setId(domain.getId());
        entity.setUnidadAcademica(unidadAcademica);
        entity.setNombre(domain.getNombre());
        entity.setSigla(domain.getSigla());
        entity.setNivel(domain.getNivel());
        entity.setAreaDisciplinaria(domain.getAreaDisciplinaria());
        entity.setModalidad(domain.getModalidad().name());
        entity.setEstado(domain.getEstado().name());
        entity.setTituloOtorgado(domain.getTituloOtorgado());
        entity.setDuracionAnios(domain.getDuracionAnios());
        entity.setDescripcion(domain.getDescripcion());
        entity.setActivo(domain.isActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        entity.setActualizadoEn(domain.getActualizadoEn());
        if (sedes != null) {
            entity.setSedes(sedes);
        }
        return entity;
    }
}
