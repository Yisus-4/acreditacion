package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class ActividadPlanTrabajo {

    public static final String CODIGO_ACT_01 = "ACT-01-INFORMAR-PROCESO";
    public static final String NOMBRE_ACT_01 = "Informar sobre el proceso de acreditación";
    public static final String CODIGO_ACT_02 = "ACT-02-ELABORAR-PLAN-TRABAJO";
    public static final String NOMBRE_ACT_02 = "Elaborar Plan de Trabajo";
    public static final String CODIGO_ACT_03 = "1.3";
    public static final String NOMBRE_ACT_03 = "Gestionar normativa de la carrera que se presenta a acreditación";

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
    private HojaPlanTrabajo hoja;
    private LocalDate fechaInicioEstimada;
    private LocalDate fechaFinEstimada;
    private String responsable;
    private PrioridadActividad prioridad;
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
                HojaPlanTrabajo.INFO,
                null,
                null,
                null,
                PrioridadActividad.MEDIA,
                now,
                now
        );
    }

    public static ActividadPlanTrabajo crearActividadElaborarPlan(UUID planTrabajoId) {
        Instant now = Instant.now();
        return new ActividadPlanTrabajo(
                UUID.randomUUID(),
                planTrabajoId,
                2,
                CODIGO_ACT_02,
                NOMBRE_ACT_02,
                EstadoActividad.PENDIENTE,
                null,
                null,
                null,
                null,
                HojaPlanTrabajo.INFO,
                null,
                null,
                null,
                PrioridadActividad.ALTA,
                now,
                now
        );
    }

    public static ActividadPlanTrabajo crearActividadNormativa(UUID planTrabajoId) {
        Instant now = Instant.now();
        return new ActividadPlanTrabajo(
                UUID.randomUUID(),
                planTrabajoId,
                3,
                CODIGO_ACT_03,
                NOMBRE_ACT_03,
                EstadoActividad.PENDIENTE,
                null,
                null,
                null,
                null,
                HojaPlanTrabajo.INFO,
                null,
                null,
                "Área de Evaluación y Acreditación (AEA) / Dirección de Carrera",
                PrioridadActividad.ALTA,
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
                HojaPlanTrabajo.INFO,
                null,
                null,
                null,
                PrioridadActividad.MEDIA,
                now,
                now
        );
    }

    public static ActividadPlanTrabajo crearActividadCronograma(UUID planTrabajoId,
                                                               int numeroOrden,
                                                               String codigoActividad,
                                                               String nombre,
                                                               HojaPlanTrabajo hoja,
                                                               LocalDate fechaInicioEstimada,
                                                               LocalDate fechaFinEstimada,
                                                               String responsable,
                                                               PrioridadActividad prioridad) {
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
                hoja != null ? hoja : HojaPlanTrabajo.INFO,
                fechaInicioEstimada,
                fechaFinEstimada,
                responsable,
                prioridad != null ? prioridad : PrioridadActividad.MEDIA,
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
                HojaPlanTrabajo.INFO,
                null,
                null,
                null,
                PrioridadActividad.MEDIA,
                creadoEn,
                actualizadoEn
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
                                                 HojaPlanTrabajo hoja,
                                                 LocalDate fechaInicioEstimada,
                                                 LocalDate fechaFinEstimada,
                                                 String responsable,
                                                 PrioridadActividad prioridad,
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
                hoja,
                fechaInicioEstimada,
                fechaFinEstimada,
                responsable,
                prioridad,
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
                                HojaPlanTrabajo hoja,
                                LocalDate fechaInicioEstimada,
                                LocalDate fechaFinEstimada,
                                String responsable,
                                PrioridadActividad prioridad,
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
        if (fechaInicioEstimada != null && fechaFinEstimada != null && fechaFinEstimada.isBefore(fechaInicioEstimada)) {
            throw new DomainValidationException("La fecha fin estimada no puede ser anterior a la fecha de inicio estimada");
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
        this.hoja = hoja != null ? hoja : HojaPlanTrabajo.INFO;
        this.fechaInicioEstimada = fechaInicioEstimada;
        this.fechaFinEstimada = fechaFinEstimada;
        this.responsable = responsable != null ? responsable.trim() : null;
        this.prioridad = prioridad != null ? prioridad : PrioridadActividad.MEDIA;
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

    public void completar(UUID usuarioAeaId) {
        if (this.estado == EstadoActividad.COMPLETADA) {
            throw new DomainValidationException("La actividad ya ha sido completada previamente");
        }
        if (usuarioAeaId == null) {
            throw new DomainValidationException("El usuario de AEA responsable es obligatorio");
        }

        this.completadaPor = usuarioAeaId;
        Instant now = Instant.now();
        this.completadaEn = now;
        this.actualizadoEn = now;
        this.estado = EstadoActividad.COMPLETADA;
    }

    public void cambiarEstado(EstadoActividad nuevoEstado) {
        if (nuevoEstado != null) {
            this.estado = nuevoEstado;
            this.actualizadoEn = Instant.now();
        }
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

    public HojaPlanTrabajo getHoja() {
        return hoja;
    }

    public LocalDate getFechaInicioEstimada() {
        return fechaInicioEstimada;
    }

    public LocalDate getFechaFinEstimada() {
        return fechaFinEstimada;
    }

    public String getResponsable() {
        return responsable;
    }

    public PrioridadActividad getPrioridad() {
        return prioridad;
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
