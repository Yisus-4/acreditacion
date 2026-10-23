package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ProcesoAcreditacion {

    private final UUID id;
    private final UUID carreraId;
    private final String codigo;
    private final String titulo;
    private TipoProcesoAcreditacion tipoProceso;
    private EstadoProcesoAcreditacion estado;
    private final UUID creadoPor;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public static ProcesoAcreditacion iniciar(UUID carreraId,
                                              String codigo,
                                              String titulo,
                                              TipoProcesoAcreditacion tipoProceso,
                                              UUID creadoPor) {
        Instant now = Instant.now();
        return new ProcesoAcreditacion(
                UUID.randomUUID(),
                carreraId,
                codigo,
                titulo,
                tipoProceso,
                EstadoProcesoAcreditacion.INICIADO,
                creadoPor,
                now,
                now
        );
    }

    public static ProcesoAcreditacion rehydrate(UUID id,
                                                UUID carreraId,
                                                String codigo,
                                                String titulo,
                                                TipoProcesoAcreditacion tipoProceso,
                                                EstadoProcesoAcreditacion estado,
                                                UUID creadoPor,
                                                Instant creadoEn,
                                                Instant actualizadoEn) {
        return new ProcesoAcreditacion(
                id,
                carreraId,
                codigo,
                titulo,
                tipoProceso,
                estado,
                creadoPor,
                creadoEn,
                actualizadoEn
        );
    }

    public ProcesoAcreditacion(UUID id,
                               UUID carreraId,
                               String codigo,
                               String titulo,
                               TipoProcesoAcreditacion tipoProceso,
                               EstadoProcesoAcreditacion estado,
                               UUID creadoPor,
                               Instant creadoEn,
                               Instant actualizadoEn) {
        if (id == null) {
            throw new DomainValidationException("El ID del proceso es obligatorio");
        }
        if (carreraId == null) {
            throw new DomainValidationException("La carrera es obligatoria");
        }
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DomainValidationException("El código del proceso es obligatorio");
        }
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new DomainValidationException("El título del proceso es obligatorio");
        }
        if (tipoProceso == null) {
            throw new DomainValidationException("El tipo de proceso es obligatorio");
        }
        if (estado == null) {
            throw new DomainValidationException("El estado del proceso es obligatorio");
        }
        if (creadoPor == null) {
            throw new DomainValidationException("El usuario creador es obligatorio");
        }

        this.id = id;
        this.carreraId = carreraId;
        this.codigo = codigo.trim();
        this.titulo = titulo.trim();
        this.tipoProceso = tipoProceso;
        this.estado = estado;
        this.creadoPor = creadoPor;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public void pasarAAutoevaluacion() {
        if (this.estado != EstadoProcesoAcreditacion.INICIADO) {
            throw new DomainValidationException("Solo se puede pasar a autoevaluación desde el estado INICIADO");
        }
        this.estado = EstadoProcesoAcreditacion.EN_AUTOEVALUACION;
        this.actualizadoEn = Instant.now();
    }

    public void actualizarTipoProceso(TipoProcesoAcreditacion nuevoTipo) {
        if (nuevoTipo != null && this.tipoProceso != nuevoTipo) {
            this.tipoProceso = nuevoTipo;
            this.actualizadoEn = Instant.now();
        }
    }

    public void finalizar() {
        if (this.estado != EstadoProcesoAcreditacion.EN_AUTOEVALUACION) {
            throw new DomainValidationException("Solo se puede finalizar un proceso en autoevaluación");
        }
        this.estado = EstadoProcesoAcreditacion.FINALIZADO;
        this.actualizadoEn = Instant.now();
    }

    public void cancelar() {
        if (this.estado == EstadoProcesoAcreditacion.FINALIZADO || this.estado == EstadoProcesoAcreditacion.CANCELADO) {
            throw new DomainValidationException("No se puede cancelar un proceso finalizado o ya cancelado");
        }
        this.estado = EstadoProcesoAcreditacion.CANCELADO;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCarreraId() {
        return carreraId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public TipoProcesoAcreditacion getTipoProceso() {
        return tipoProceso;
    }

    public EstadoProcesoAcreditacion getEstado() {
        return estado;
    }

    public UUID getCreadoPor() {
        return creadoPor;
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
        ProcesoAcreditacion that = (ProcesoAcreditacion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
