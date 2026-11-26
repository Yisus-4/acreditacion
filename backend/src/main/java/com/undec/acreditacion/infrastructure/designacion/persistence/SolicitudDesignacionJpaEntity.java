package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.EstadoSolicitudDesignacion;
import com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "solicitud_designacion")
public class SolicitudDesignacionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_academico_id", nullable = false)
    private DepartamentoAcademicoJpaEntity departamentoAcademico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_academico_id", nullable = false)
    private PeriodoAcademicoJpaEntity periodoAcademico;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitud", nullable = false, length = 25)
    private TipoSolicitudDesignacion tipoSolicitud = TipoSolicitudDesignacion.ANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 25)
    private EstadoSolicitudDesignacion estado = EstadoSolicitudDesignacion.BORRADOR;

    @Column(name = "solicitud_origen_id")
    private UUID solicitudOrigenId;

    @Column(name = "observaciones_departamento", columnDefinition = "TEXT")
    private String observacionesDepartamento;

    @Column(name = "observaciones_sga", columnDefinition = "TEXT")
    private String observacionesSga;

    @Column(name = "fecha_emision")
    private Instant fechaEmision;

    @Column(name = "emitida_por_usuario_id")
    private UUID emitidaPorUsuarioId;

    @Column(name = "revisada_por_usuario_id")
    private UUID revisadaPorUsuarioId;

    @Column(name = "fecha_revision")
    private Instant fechaRevision;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public SolicitudDesignacionJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DepartamentoAcademicoJpaEntity getDepartamentoAcademico() { return departamentoAcademico; }
    public void setDepartamentoAcademico(DepartamentoAcademicoJpaEntity departamentoAcademico) { this.departamentoAcademico = departamentoAcademico; }

    public PeriodoAcademicoJpaEntity getPeriodoAcademico() { return periodoAcademico; }
    public void setPeriodoAcademico(PeriodoAcademicoJpaEntity periodoAcademico) { this.periodoAcademico = periodoAcademico; }

    public TipoSolicitudDesignacion getTipoSolicitud() { return tipoSolicitud; }
    public void setTipoSolicitud(TipoSolicitudDesignacion tipoSolicitud) { this.tipoSolicitud = tipoSolicitud; }

    public EstadoSolicitudDesignacion getEstado() { return estado; }
    public void setEstado(EstadoSolicitudDesignacion estado) { this.estado = estado; }

    public UUID getSolicitudOrigenId() { return solicitudOrigenId; }
    public void setSolicitudOrigenId(UUID solicitudOrigenId) { this.solicitudOrigenId = solicitudOrigenId; }

    public String getObservacionesDepartamento() { return observacionesDepartamento; }
    public void setObservacionesDepartamento(String observacionesDepartamento) { this.observacionesDepartamento = observacionesDepartamento; }

    public String getObservacionesSga() { return observacionesSga; }
    public void setObservacionesSga(String observacionesSga) { this.observacionesSga = observacionesSga; }

    public Instant getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(Instant fechaEmision) { this.fechaEmision = fechaEmision; }

    public UUID getEmitidaPorUsuarioId() { return emitidaPorUsuarioId; }
    public void setEmitidaPorUsuarioId(UUID emitidaPorUsuarioId) { this.emitidaPorUsuarioId = emitidaPorUsuarioId; }

    public UUID getRevisadaPorUsuarioId() { return revisadaPorUsuarioId; }
    public void setRevisadaPorUsuarioId(UUID revisadaPorUsuarioId) { this.revisadaPorUsuarioId = revisadaPorUsuarioId; }

    public Instant getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(Instant fechaRevision) { this.fechaRevision = fechaRevision; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }

    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolicitudDesignacionJpaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
