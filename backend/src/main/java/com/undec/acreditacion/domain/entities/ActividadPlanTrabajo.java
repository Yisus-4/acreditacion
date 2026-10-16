package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ActividadPlanTrabajo {

    public static final String CODIGO_ACT_01 = "ACT-01-INFORMAR-PROCESO";
    public static final String NOMBRE_ACT_01 = "Informar sobre el proceso de acreditación";

    private final UUID id;
    private final UUID planTrabajoId;
    private final int numeroOrden;
    private final String codigoActividad;
    private final String nombre;
    private EstadoActividad estado;
    private Instant fechaReunionCoordinacion;
    private String sintesisAcuerdos;
    private Instant completadaEn;
    private UUID completadaPor;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public static ActividadPlanTrabajo crearActividadInicial(UUID planTrabajoId) {
        Instant now = Instant.now();
        return new ActividadPlanTrabajo(
                UUID.randomUUID(),
                planTrabajoId,
                1,
                CODIGO_ACT_01,
                NOMBRE_ACT_01,
                EstadoActividad.PENDIENTE,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public static ActividadPlanTrabajo crear(UUID planTrabajoId,
                                            int numeroOrden,
                                            String codigoActividad,
                                            String nombre) {
        Instant now = Instant.now();
        return new ActividadPlanTrabajo(
                UUID.randomUUID(),
                planTrabajoId,
                numeroOrden,
                codigoActividad,
                nombre,
                EstadoActividad.PENDIENTE,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public static ActividadPlanTrabajo rehydrate(UUID id,
                                                 UUID planTrabajoId,
                                                 int numeroOrden,
                                                 String codigoActividad,
                                                 String nombre,
                                                 EstadoActividad estado,
                                                 Instant fechaReunionCoordinacion,
                                                 String sintesisAcuerdos,
                                                 Instant completadaEn,
                                                 UUID completadaPor,
                                                 Instant creadoEn,
                                                 Instant actualizadoEn) {
        return new ActividadPlanTrabajo(
                id,
                planTrabajoId,
                numeroOrden,
                codigoActividad,
                nombre,
                estado,
                fechaReunionCoordinacion,
                sintesisAcuerdos,
                completadaEn,
                completadaPor,
                creadoEn,
                actualizadoEn
        );
    }

    public ActividadPlanTrabajo(UUID id,
                                UUID planTrabajoId,
                                int numeroOrden,
                                String codigoActividad,
                                String nombre,
                                EstadoActividad estado,
                                Instant fechaReunionCoordinacion,
                                String sintesisAcuerdos,
                                Instant completadaEn,
                                UUID completadaPor,
                                Instant creadoEn,
                                Instant actualizadoEn) {
        if (id == null) {
            throw new DomainValidationException("El ID de la actividad es obligatorio");
        }
        if (planTrabajoId == null) {
            throw new DomainValidationException("El ID del plan de trabajo es obligatorio");
        }
        if (numeroOrden <= 0) {
            throw new DomainValidationException("El número de orden debe ser mayor a cero");
        }
        if (codigoActividad == null || codigoActividad.trim().isEmpty()) {
            throw new DomainValidationException("El código de actividad es obligatorio");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new DomainValidationException("El nombre de la actividad es obligatorio");
        }
        if (estado == null) {
            throw new DomainValidationException("El estado de la actividad es obligatorio");
        }

        this.id = id;
        this.planTrabajoId = planTrabajoId;
        this.numeroOrden = numeroOrden;
        this.codigoActividad = codigoActividad.trim();
        this.nombre = nombre.trim();
        this.estado = estado;
        this.fechaReunionCoordinacion = fechaReunionCoordinacion;
        this.sintesisAcuerdos = sintesisAcuerdos;
        this.completadaEn = completadaEn;
        this.completadaPor = completadaPor;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public void registrarReunionYCompletar(Instant fechaReunion, String sintesis, UUID usuarioAeaId) {
        if (this.estado == EstadoActividad.COMPLETADA) {
            throw new DomainValidationException("La actividad ya ha sido completada previamente");
        }
        if (fechaReunion == null) {
            throw new DomainValidationException("La fecha de reunión de coordinación es obligatoria");
        }
        if (sintesis == null || sintesis.trim().length() < 10) {
            throw new DomainValidationException("La síntesis de acuerdos debe contener al menos 10 caracteres");
        }
        if (usuarioAeaId == null) {
            throw new DomainValidationException("El usuario de AEA responsable es obligatorio");
        }

        this.fechaReunionCoordinacion = fechaReunion;
        this.sintesisAcuerdos = sintesis.trim();
        this.completadaPor = usuarioAeaId;
        Instant now = Instant.now();
        this.completadaEn = now;
        this.actualizadoEn = now;
        this.estado = EstadoActividad.COMPLETADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlanTrabajoId() {
        return planTrabajoId;
    }

    public int getNumeroOrden() {
        return numeroOrden;
    }

    public String getCodigoActividad() {
        return codigoActividad;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoActividad getEstado() {
        return estado;
    }

    public Instant getFechaReunionCoordinacion() {
        return fechaReunionCoordinacion;
    }

    public String getSintesisAcuerdos() {
        return sintesisAcuerdos;
    }

    public Instant getCompletadaEn() {
        return completadaEn;
    }

    public UUID getCompletadaPor() {
        return completadaPor;
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
        ActividadPlanTrabajo that = (ActividadPlanTrabajo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
