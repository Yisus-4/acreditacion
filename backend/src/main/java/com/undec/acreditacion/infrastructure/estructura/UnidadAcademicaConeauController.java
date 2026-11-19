package com.undec.acreditacion.infrastructure.estructura;

import com.undec.acreditacion.infrastructure.persistence.postgres.InstitucionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataInstitucionRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUnidadAcademicaConeauRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UnidadAcademicaConeauJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estructura/unidades-academicas-coneau")
public class UnidadAcademicaConeauController {

    private final SpringDataUnidadAcademicaConeauRepository uaRepository;
    private final SpringDataInstitucionRepository institucionRepository;

    public UnidadAcademicaConeauController(
            SpringDataUnidadAcademicaConeauRepository uaRepository,
            SpringDataInstitucionRepository institucionRepository
    ) {
        this.uaRepository = uaRepository;
        this.institucionRepository = institucionRepository;
    }

    public record UnidadAcademicaConeauResponse(
            UUID id,
            UUID institucionId,
            String institucionNombre,
            String denominacion,
            boolean activa,
            int cantidadSedes,
            Instant creadoEn,
            Instant actualizadoEn
    ) {}

    public record GuardarUnidadAcademicaConeauRequest(
            UUID institucionId,
            String denominacion,
            Boolean activa
    ) {}

    @GetMapping
    public ResponseEntity<List<UnidadAcademicaConeauResponse>> listar(
            @RequestParam(required = false) UUID institucionId,
            @RequestParam(defaultValue = "true") boolean incluirInactivas
    ) {
        List<UnidadAcademicaConeauJpaEntity> lista;
        if (institucionId != null) {
            lista = incluirInactivas
                    ? uaRepository.findByInstitucionId(institucionId)
                    : uaRepository.findByInstitucionIdAndActivaTrue(institucionId);
        } else {
            lista = uaRepository.findAll();
            if (!incluirInactivas) {
                lista = lista.stream().filter(UnidadAcademicaConeauJpaEntity::isActiva).collect(Collectors.toList());
            }
        }

        List<UnidadAcademicaConeauResponse> res = lista.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadAcademicaConeauResponse> obtenerPorId(@PathVariable UUID id) {
        UnidadAcademicaConeauJpaEntity entity = uaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad académica no encontrada"));
        return ResponseEntity.ok(toResponse(entity));
    }

    @PostMapping
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<UnidadAcademicaConeauResponse> crear(@RequestBody GuardarUnidadAcademicaConeauRequest req) {
        if (req.denominacion() == null || req.denominacion().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La denominación es obligatoria");
        }

        UUID instId = req.institucionId();
        if (instId == null) {
            InstitucionJpaEntity firstInst = institucionRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe ninguna institución"));
            instId = firstInst.getId();
        }

        final UUID finalInstId = instId;
        InstitucionJpaEntity institucion = institucionRepository.findById(finalInstId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Institución no encontrada"));

        UnidadAcademicaConeauJpaEntity nueva = new UnidadAcademicaConeauJpaEntity();
        nueva.setId(UUID.randomUUID());
        nueva.setInstitucion(institucion);
        nueva.setDenominacion(req.denominacion().trim());
        nueva.setActiva(req.activa() == null || req.activa());
        nueva.setCreadoEn(Instant.now());
        nueva.setActualizadoEn(Instant.now());

        UnidadAcademicaConeauJpaEntity guardada = uaRepository.save(nueva);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(guardada));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<UnidadAcademicaConeauResponse> actualizar(
            @PathVariable UUID id,
            @RequestBody GuardarUnidadAcademicaConeauRequest req
    ) {
        UnidadAcademicaConeauJpaEntity entity = uaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad académica no encontrada"));

        if (req.denominacion() != null && !req.denominacion().trim().isEmpty()) {
            entity.setDenominacion(req.denominacion().trim());
        }
        if (req.activa() != null) {
            entity.setActiva(req.activa());
        }
        entity.setActualizadoEn(Instant.now());

        UnidadAcademicaConeauJpaEntity guardada = uaRepository.save(entity);
        return ResponseEntity.ok(toResponse(guardada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        UnidadAcademicaConeauJpaEntity entity = uaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad académica no encontrada"));

        if (entity.getSedes() != null && !entity.getSedes().isEmpty()) {
            // Regla oficial CONEAU Global: si tiene sedes vinculadas, desactivar sin eliminar
            entity.setActiva(false);
            entity.setActualizadoEn(Instant.now());
            uaRepository.save(entity);
        } else {
            uaRepository.delete(entity);
        }
        return ResponseEntity.noContent().build();
    }

    private UnidadAcademicaConeauResponse toResponse(UnidadAcademicaConeauJpaEntity e) {
        int cantSedes = e.getSedes() != null ? e.getSedes().size() : 0;
        return new UnidadAcademicaConeauResponse(
                e.getId(),
                e.getInstitucion() != null ? e.getInstitucion().getId() : null,
                e.getInstitucion() != null ? e.getInstitucion().getNombre() : null,
                e.getDenominacion(),
                e.isActiva(),
                cantSedes,
                e.getCreadoEn(),
                e.getActualizadoEn()
        );
    }
}
