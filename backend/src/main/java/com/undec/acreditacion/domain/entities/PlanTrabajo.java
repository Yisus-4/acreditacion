package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PlanTrabajo {

    private final UUID id;
    private final UUID procesoId;
    private final String codigo;
    private final TipoPlanTrabajo tipo;
    private EstadoPlanTrabajo estado;
    private final Instant creadoEn;
    private Instant actualizadoEn;
    private final List<ActividadPlanTrabajo> actividades = new ArrayList<>();

    public static PlanTrabajo inicializar(UUID procesoId, String codigo, TipoPlanTrabajo tipo) {
        UUID planId = UUID.randomUUID();
        List<ActividadPlanTrabajo> inicial = new ArrayList<>();
        inicial.add(ActividadPlanTrabajo.crearActividadInicial(planId));
        Instant now = Instant.now();
        return new PlanTrabajo(
                planId,
                procesoId,
                codigo,
                tipo,
                EstadoPlanTrabajo.ACTIVO,
                now,
                now,
                inicial
        );
    }

    public static PlanTrabajo rehydrate(UUID id,
                                        UUID procesoId,
                                        String codigo,
                                        TipoPlanTrabajo tipo,
                                        EstadoPlanTrabajo estado,
                                        Instant creadoEn,
                                        Instant actualizadoEn,
                                        List<ActividadPlanTrabajo> actividades) {
        return new PlanTrabajo(
                id,
                procesoId,
                codigo,
                tipo,
                estado,
                creadoEn,
                actualizadoEn,
                actividades
        );
    }

    public PlanTrabajo(UUID id,
                       UUID procesoId,
                       String codigo,
                       TipoPlanTrabajo tipo,
                       EstadoPlanTrabajo estado,
                       Instant creadoEn,
                       Instant actualizadoEn,
                       List<ActividadPlanTrabajo> actividades) {
        if (id == null) {
            throw new DomainValidationException("El ID del plan de trabajo es obligatorio");
        }
        if (procesoId == null) {
            throw new DomainValidationException("El ID del proceso es obligatorio");
        }
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DomainValidationException("El código del plan de trabajo es obligatorio");
        }
        if (tipo == null) {
            throw new DomainValidationException("El tipo de plan de trabajo es obligatorio");
        }
        if (estado == null) {
            throw new DomainValidationException("El estado del plan de trabajo es obligatorio");
        }

        this.id = id;
        this.procesoId = procesoId;
        this.codigo = codigo.trim();
        this.tipo = tipo;
        this.estado = estado;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;

        if (actividades != null) {
            this.actividades.addAll(actividades);
        }
    }

    public void anexarActividad(int numeroOrden, String codigoActividad, String nombre) {
        if (this.estado != EstadoPlanTrabajo.ACTIVO) {
            throw new DomainValidationException("No se pueden agregar actividades a un plan finalizado");
        }
        if (actividades.stream().anyMatch(a -> a.getNumeroOrden() == numeroOrden)) {
            throw new DomainValidationException("Ya existe una actividad con orden " + numeroOrden);
        }
        ActividadPlanTrabajo nueva = ActividadPlanTrabajo.crear(this.id, numeroOrden, codigoActividad, nombre);
        this.actividades.add(nueva);
        this.actualizadoEn = Instant.now();
    }

    public void finalizar() {
        this.estado = EstadoPlanTrabajo.FINALIZADO;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoPlanTrabajo getTipo() {
        return tipo;
    }

    public EstadoPlanTrabajo getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public List<ActividadPlanTrabajo> getActividades() {
        return Collections.unmodifiableList(actividades);
    }

    public double getPorcentajeProgreso() {
        if (actividades.isEmpty()) {
            return 0.0;
        }
        long completadas = actividades.stream()
                .filter(a -> a.getEstado() == EstadoActividad.COMPLETADA)
                .count();
        return (double) completadas / actividades.size() * 100.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanTrabajo that = (PlanTrabajo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
