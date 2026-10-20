package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class RegistroConvocatoria {

    private final UUID id;
    private final UUID procesoId;
    private final String nroResolucionConeau;
    private final LocalDate fechaInicio;
    private final LocalDate fechaCierre;
    private final String observaciones;
    private String rmEstandares;
    private FaseEvaluacion faseEvaluacion;
    private LocalDate fechaLimitePresentacionConeau;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public static RegistroConvocatoria crear(UUID procesoId,
                                             String nroResolucionConeau,
                                             LocalDate fechaInicio,
                                             LocalDate fechaCierre,
                                             String observaciones) {
        Instant now = Instant.now();
        return new RegistroConvocatoria(
                UUID.randomUUID(),
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                null,
                FaseEvaluacion.PRIMERA_FASE,
                null,
                now,
                now
        );
    }

    public static RegistroConvocatoria crear(UUID procesoId,
                                             String nroResolucionConeau,
                                             LocalDate fechaInicio,
                                             LocalDate fechaCierre,
                                             String observaciones,
                                             String rmEstandares,
                                             FaseEvaluacion faseEvaluacion,
                                             LocalDate fechaLimitePresentacionConeau) {
        Instant now = Instant.now();
        return new RegistroConvocatoria(
                UUID.randomUUID(),
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                now,
                now
        );
    }

    public static RegistroConvocatoria rehydrate(UUID id,
                                                 UUID procesoId,
                                                 String nroResolucionConeau,
                                                 LocalDate fechaInicio,
                                                 LocalDate fechaCierre,
                                                 String observaciones,
                                                 Instant creadoEn,
                                                 Instant actualizadoEn) {
        return new RegistroConvocatoria(
                id,
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                null,
                FaseEvaluacion.PRIMERA_FASE,
                null,
                creadoEn,
                actualizadoEn
        );
    }

    public static RegistroConvocatoria rehydrate(UUID id,
                                                 UUID procesoId,
                                                 String nroResolucionConeau,
                                                 LocalDate fechaInicio,
                                                 LocalDate fechaCierre,
                                                 String observaciones,
                                                 String rmEstandares,
                                                 FaseEvaluacion faseEvaluacion,
                                                 LocalDate fechaLimitePresentacionConeau,
                                                 Instant creadoEn,
                                                 Instant actualizadoEn) {
        return new RegistroConvocatoria(
                id,
                procesoId,
                nroResolucionConeau,
                fechaInicio,
                fechaCierre,
                observaciones,
                rmEstandares,
                faseEvaluacion,
                fechaLimitePresentacionConeau,
                creadoEn,
                actualizadoEn
        );
    }

    public RegistroConvocatoria(UUID id,
                                UUID procesoId,
                                String nroResolucionConeau,
                                LocalDate fechaInicio,
                                LocalDate fechaCierre,
                                String observaciones,
                                String rmEstandares,
                                FaseEvaluacion faseEvaluacion,
                                LocalDate fechaLimitePresentacionConeau,
                                Instant creadoEn,
                                Instant actualizadoEn) {
        if (id == null) {
            throw new DomainValidationException("El ID del registro de convocatoria es obligatorio");
        }
        if (procesoId == null) {
            throw new DomainValidationException("El ID del proceso es obligatorio");
        }
        if (nroResolucionConeau == null || nroResolucionConeau.trim().isEmpty()) {
            throw new DomainValidationException("El número de resolución CONEAU es obligatorio");
        }
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now();
        }
        if (fechaCierre == null) {
            throw new DomainValidationException("La fecha de cierre de la convocatoria es obligatoria");
        }
        if (fechaCierre.isBefore(fechaInicio)) {
            throw new DomainValidationException("La fecha de cierre CONEAU no puede ser anterior a la fecha de inicio");
        }
        if (fechaLimitePresentacionConeau != null && fechaLimitePresentacionConeau.isBefore(fechaCierre)) {
            throw new DomainValidationException("La fecha límite de presentación CONEAU no puede ser anterior a la fecha de cierre de la convocatoria");
        }

        this.id = id;
        this.procesoId = procesoId;
        this.nroResolucionConeau = nroResolucionConeau.trim();
        this.fechaInicio = fechaInicio;
        this.fechaCierre = fechaCierre;
        this.observaciones = observaciones != null ? observaciones.trim() : null;
        this.rmEstandares = rmEstandares != null && !rmEstandares.trim().isEmpty() ? rmEstandares.trim() : null;
        this.faseEvaluacion = faseEvaluacion != null ? faseEvaluacion : FaseEvaluacion.PRIMERA_FASE;
        this.fechaLimitePresentacionConeau = fechaLimitePresentacionConeau;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public void completarDatosConeau(String rmEstandares, FaseEvaluacion faseEvaluacion, LocalDate fechaLimiteConeau) {
        if (faseEvaluacion == null) {
            throw new DomainValidationException("La fase de evaluación es obligatoria");
        }
        if (fechaLimiteConeau == null) {
            throw new DomainValidationException("La fecha límite de presentación CONEAU es obligatoria al completar los datos CONEAU");
        }
        if (fechaLimiteConeau.isBefore(this.fechaCierre)) {
            throw new DomainValidationException("La fecha límite de presentación CONEAU no puede ser anterior a la fecha de cierre de la convocatoria");
        }

        this.rmEstandares = rmEstandares != null && !rmEstandares.trim().isEmpty() ? rmEstandares.trim() : null;
        this.faseEvaluacion = faseEvaluacion;
        this.fechaLimitePresentacionConeau = fechaLimiteConeau;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public String getNroResolucionConeau() {
        return nroResolucionConeau;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public String getRmEstandares() {
        return rmEstandares;
    }

    public FaseEvaluacion getFaseEvaluacion() {
        return faseEvaluacion;
    }

    public LocalDate getFechaLimitePresentacionConeau() {
        return fechaLimitePresentacionConeau;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistroConvocatoria that = (RegistroConvocatoria) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
