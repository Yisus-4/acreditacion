package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.EstadoItemDesignacion;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "solicitud_docente_designacion")
public class SolicitudDocenteDesignacionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudDesignacionJpaEntity solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id", nullable = false)
    private CuerpoAcademicoMaestroJpaEntity docente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_docente_id", nullable = false)
    private CargoDocenteJpaEntity cargoDocente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dedicacion_docente_id", nullable = false)
    private DedicacionDocenteJpaEntity dedicacionDocente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_designacion_id", nullable = false)
    private TipoDesignacionDocenteJpaEntity tipoDesignacion;

    @Column(name = "horas_dedicacion", nullable = false)
    private Integer horasDedicacion;

    @Column(name = "nro_designacion_mapuche", length = 80)
    private String nroDesignacionMapuche;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_item", nullable = false, length = 25)
    private EstadoItemDesignacion estadoItem = EstadoItemDesignacion.NUEVO;

    @Column(name = "designacion_origen_id")
    private UUID designacionOrigenId;

    @Column(name = "horas_clases", nullable = false)
    private Integer horasClases = 0;

    @Column(name = "horas_investigacion", nullable = false)
    private Integer horasInvestigacion = 0;

    @Column(name = "horas_extension", nullable = false)
    private Integer horasExtension = 0;

    @Column(name = "horas_gestion", nullable = false)
    private Integer horasGestion = 0;

    @Column(name = "horas_direccion_tesis", nullable = false)
    private Integer horasDireccionTesis = 0;

    @Column(name = "horas_otras", nullable = false)
    private Integer horasOtras = 0;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public SolicitudDocenteDesignacionJpaEntity() {
    }

    public int totalHorasDistribuidas() {
        return safe(horasClases) + safe(horasInvestigacion) + safe(horasExtension)
                + safe(horasGestion) + safe(horasDireccionTesis) + safe(horasOtras);
    }

    private static int safe(Integer v) {
        return v != null ? v : 0;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SolicitudDesignacionJpaEntity getSolicitud() { return solicitud; }
    public void setSolicitud(SolicitudDesignacionJpaEntity solicitud) { this.solicitud = solicitud; }

    public CuerpoAcademicoMaestroJpaEntity getDocente() { return docente; }
    public void setDocente(CuerpoAcademicoMaestroJpaEntity docente) { this.docente = docente; }

    public CargoDocenteJpaEntity getCargoDocente() { return cargoDocente; }
    public void setCargoDocente(CargoDocenteJpaEntity cargoDocente) { this.cargoDocente = cargoDocente; }

    public DedicacionDocenteJpaEntity getDedicacionDocente() { return dedicacionDocente; }
    public void setDedicacionDocente(DedicacionDocenteJpaEntity dedicacionDocente) { this.dedicacionDocente = dedicacionDocente; }

    public TipoDesignacionDocenteJpaEntity getTipoDesignacion() { return tipoDesignacion; }
    public void setTipoDesignacion(TipoDesignacionDocenteJpaEntity tipoDesignacion) { this.tipoDesignacion = tipoDesignacion; }

    public Integer getHorasDedicacion() { return horasDedicacion; }
    public void setHorasDedicacion(Integer horasDedicacion) { this.horasDedicacion = horasDedicacion; }

    public String getNroDesignacionMapuche() { return nroDesignacionMapuche; }
    public void setNroDesignacionMapuche(String nroDesignacionMapuche) { this.nroDesignacionMapuche = nroDesignacionMapuche; }

    public EstadoItemDesignacion getEstadoItem() { return estadoItem; }
    public void setEstadoItem(EstadoItemDesignacion estadoItem) { this.estadoItem = estadoItem; }

    public UUID getDesignacionOrigenId() { return designacionOrigenId; }
    public void setDesignacionOrigenId(UUID designacionOrigenId) { this.designacionOrigenId = designacionOrigenId; }

    public Integer getHorasClases() { return horasClases; }
    public void setHorasClases(Integer horasClases) { this.horasClases = horasClases; }

    public Integer getHorasInvestigacion() { return horasInvestigacion; }
    public void setHorasInvestigacion(Integer horasInvestigacion) { this.horasInvestigacion = horasInvestigacion; }

    public Integer getHorasExtension() { return horasExtension; }
    public void setHorasExtension(Integer horasExtension) { this.horasExtension = horasExtension; }

    public Integer getHorasGestion() { return horasGestion; }
    public void setHorasGestion(Integer horasGestion) { this.horasGestion = horasGestion; }

    public Integer getHorasDireccionTesis() { return horasDireccionTesis; }
    public void setHorasDireccionTesis(Integer horasDireccionTesis) { this.horasDireccionTesis = horasDireccionTesis; }

    public Integer getHorasOtras() { return horasOtras; }
    public void setHorasOtras(Integer horasOtras) { this.horasOtras = horasOtras; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }

    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolicitudDocenteDesignacionJpaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
