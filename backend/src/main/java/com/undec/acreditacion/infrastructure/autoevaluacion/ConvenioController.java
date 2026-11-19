package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ConvenioInstitucionalDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ConvenioInstitucionalJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataConvenioInstitucionalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/convenios")
public class ConvenioController {

    private final SpringDataConvenioInstitucionalRepository convenioRepository;

    public ConvenioController(SpringDataConvenioInstitucionalRepository convenioRepository) {
        this.convenioRepository = convenioRepository;
    }

    @GetMapping
    public ResponseEntity<List<ConvenioInstitucionalDto>> listar(
            @RequestParam(value = "vigente", required = false) Boolean vigente,
            @RequestParam(value = "q", required = false) String query
    ) {
        List<ConvenioInstitucionalJpaEntity> entities;
        if (query != null && !query.isBlank()) {
            entities = convenioRepository.buscarPorTexto(query.trim());
        } else if (vigente != null) {
            entities = convenioRepository.findByVigenteOrderByContraparteAsc(vigente);
        } else {
            entities = convenioRepository.findAllByOrderByFechaCelebracionDesc();
        }

        List<ConvenioInstitucionalDto> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConvenioInstitucionalDto> obtenerPorId(@PathVariable UUID id) {
        ConvenioInstitucionalJpaEntity entity = convenioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el convenio con ID: " + id));
        return ResponseEntity.ok(toDto(entity));
    }

    @PostMapping
    @PreAuthorize("hasRole('AEA') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<ConvenioInstitucionalDto> crear(@RequestBody ConvenioInstitucionalDto dto) {
        if (dto.contraparte() == null || dto.contraparte().isBlank()) {
            throw new DomainValidationException("La contraparte del convenio es obligatoria");
        }

        ConvenioInstitucionalJpaEntity entity = new ConvenioInstitucionalJpaEntity();
        entity.setId(dto.id() != null ? dto.id() : UUID.randomUUID());
        mapearDatos(entity, dto);
        Instant now = Instant.now();
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        ConvenioInstitucionalJpaEntity guardado = convenioRepository.save(entity);
        return ResponseEntity.ok(toDto(guardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('AEA') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<ConvenioInstitucionalDto> actualizar(@PathVariable UUID id, @RequestBody ConvenioInstitucionalDto dto) {
        ConvenioInstitucionalJpaEntity entity = convenioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el convenio con ID: " + id));

        if (dto.contraparte() == null || dto.contraparte().isBlank()) {
            throw new DomainValidationException("La contraparte del convenio es obligatoria");
        }

        mapearDatos(entity, dto);
        entity.setActualizadoEn(Instant.now());

        ConvenioInstitucionalJpaEntity actualizado = convenioRepository.save(entity);
        return ResponseEntity.ok(toDto(actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AEA') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        if (!convenioRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se encontró el convenio con ID: " + id);
        }
        convenioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void mapearDatos(ConvenioInstitucionalJpaEntity entity, ConvenioInstitucionalDto dto) {
        entity.setExpediente(dto.expediente());
        entity.setResolucionRectoral(dto.resolucionRectoral());
        entity.setContraparte(dto.contraparte().trim());
        entity.setFechaCelebracion(dto.fechaCelebracion());
        entity.setDuracion(dto.duracion());
        entity.setFechaCese(dto.fechaCese());
        entity.setObjeto(dto.objeto());
        entity.setVigente(dto.vigente() != null ? dto.vigente() : true);
        entity.setEstadoVigencia(dto.estadoVigencia() != null ? dto.estadoVigencia() : (entity.getVigente() ? "VIGENTE" : "NO_VIGENTE"));
        entity.setEstadoEjecucion(dto.estadoEjecucion());
        entity.setFinanciamiento(dto.financiamiento());
        entity.setResponsableUndec(dto.responsableUndec());
        entity.setObjetivosGenerales(dto.objetivosGenerales());
        entity.setObjetivosEspecificos(dto.objetivosEspecificos());
        entity.setResultadosEsperados(dto.resultadosEsperados());
        entity.setCarrerasBeneficiadas(dto.carrerasBeneficiadas());
        entity.setTipoConvenio(dto.tipoConvenio());
        entity.setArchivoId(dto.archivoId());
    }

    private ConvenioInstitucionalDto toDto(ConvenioInstitucionalJpaEntity entity) {
        return new ConvenioInstitucionalDto(
                entity.getId(),
                entity.getExpediente(),
                entity.getResolucionRectoral(),
                entity.getContraparte(),
                entity.getFechaCelebracion(),
                entity.getDuracion(),
                entity.getFechaCese(),
                entity.getObjeto(),
                entity.getVigente(),
                entity.getEstadoVigencia(),
                entity.getEstadoEjecucion(),
                entity.getFinanciamiento(),
                entity.getResponsableUndec(),
                entity.getObjetivosGenerales(),
                entity.getObjetivosEspecificos(),
                entity.getResultadosEsperados(),
                entity.getCarrerasBeneficiadas(),
                entity.getTipoConvenio(),
                entity.getArchivoId(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }
}
