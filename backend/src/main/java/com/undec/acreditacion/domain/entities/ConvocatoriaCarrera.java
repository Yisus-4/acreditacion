package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ConvocatoriaCarrera {

    private final UUID id;
    private final UUID convocatoriaId;
    private final UUID carreraId;
    private boolean seleccionadaPorAea;
    private boolean esObligatoria;
    private boolean seleccionadaPorDe;
    private UUID procesoId;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public static ConvocatoriaCarrera crear(UUID convocatoriaId,
                                            UUID carreraId,
                                            boolean seleccionadaPorAea,
                                            boolean esObligatoria) {
        Instant now = Instant.now();
        return new ConvocatoriaCarrera(
                UUID.randomUUID(),
                convocatoriaId,
                carreraId,
                seleccionadaPorAea,
                esObligatoria,
                false,
                null,
                now,
                now
        );
    }

    public static ConvocatoriaCarrera rehydrate(UUID id,
                                               UUID convocatoriaId,
                                               UUID carreraId,
                                               boolean seleccionadaPorAea,
                                               boolean esObligatoria,
                                               boolean seleccionadaPorDe,
                                               UUID procesoId,
                                               Instant creadoEn,
                                               Instant actualizadoEn) {
        return new ConvocatoriaCarrera(
                id,
                convocatoriaId,
                carreraId,
                seleccionadaPorAea,
                esObligatoria,
                seleccionadaPorDe,
                procesoId,
                creadoEn,
                actualizadoEn
        );
    }

    public ConvocatoriaCarrera(UUID id,
                               UUID convocatoriaId,
                               UUID carreraId,
                               boolean seleccionadaPorAea,
                               boolean esObligatoria,
                               boolean seleccionadaPorDe,
                               UUID procesoId,
                               Instant creadoEn,
                               Instant actualizadoEn) {
        if (id == null) {
            throw new DomainValidationException("El ID del item de convocatoria es obligatorio");
        }
        if (convocatoriaId == null) {
            throw new DomainValidationException("El ID de la convocatoria es obligatorio");
        }
        if (carreraId == null) {
            throw new DomainValidationException("La carrera es obligatoria");
        }

        this.id = id;
        this.convocatoriaId = convocatoriaId;
        this.carreraId = carreraId;
        this.seleccionadaPorAea = seleccionadaPorAea;
        this.esObligatoria = esObligatoria;
        this.seleccionadaPorDe = seleccionadaPorDe;
        this.procesoId = procesoId;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public void confirmarPorDirectorEscuela(boolean seleccionada) {
        if (this.esObligatoria && !seleccionada) {
            throw new DomainValidationException("Una carrera marcada como obligatoria no puede ser excluida por la Dirección de Escuela");
        }
        this.seleccionadaPorDe = seleccionada;
        this.actualizadoEn = Instant.now();
    }

    public void vincularProceso(UUID procesoId) {
        this.procesoId = Objects.requireNonNull(procesoId, "El ID del proceso no puede ser nulo");
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getConvocatoriaId() {
        return convocatoriaId;
    }

    public UUID getCarreraId() {
        return carreraId;
    }

    public boolean isSeleccionadaPorAea() {
        return seleccionadaPorAea;
    }

    public boolean isEsObligatoria() {
        return esObligatoria;
    }

    public boolean isSeleccionadaPorDe() {
        return seleccionadaPorDe;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
