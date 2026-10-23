package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convocatoria_carrera")
public class ConvocatoriaCarreraJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convocatoria_id", nullable = false)
    private ConvocatoriaJpaEntity convocatoria;

    @Column(name = "carrera_id", nullable = false)
    private UUID carreraId;

    @Column(name = "seleccionada_por_aea", nullable = false)
    private boolean seleccionadaPorAea;

    @Column(name = "es_obligatoria", nullable = false)
    private boolean esObligatoria;

    @Column(name = "seleccionada_por_de", nullable = false)
    private boolean seleccionadaPorDe;

    @Column(name = "proceso_id")
    private UUID procesoId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public ConvocatoriaCarreraJpaEntity() {
    }

    public ConvocatoriaCarreraJpaEntity(UUID id, ConvocatoriaJpaEntity convocatoria, UUID carreraId,
                                        boolean seleccionadaPorAea, boolean esObligatoria,
                                        boolean seleccionadaPorDe, UUID procesoId,
                                        Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.convocatoria = convocatoria;
        this.carreraId = carreraId;
        this.seleccionadaPorAea = seleccionadaPorAea;
        this.esObligatoria = esObligatoria;
        this.seleccionadaPorDe = seleccionadaPorDe;
        this.procesoId = procesoId;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConvocatoriaJpaEntity getConvocatoria() {
        return convocatoria;
    }

    public void setConvocatoria(ConvocatoriaJpaEntity convocatoria) {
        this.convocatoria = convocatoria;
    }

    public UUID getCarreraId() {
        return carreraId;
    }

    public void setCarreraId(UUID carreraId) {
        this.carreraId = carreraId;
    }

    public boolean isSeleccionadaPorAea() {
        return seleccionadaPorAea;
    }

    public void setSeleccionadaPorAea(boolean seleccionadaPorAea) {
        this.seleccionadaPorAea = seleccionadaPorAea;
    }

    public boolean isEsObligatoria() {
        return esObligatoria;
    }

    public void setEsObligatoria(boolean esObligatoria) {
        this.esObligatoria = esObligatoria;
    }

    public boolean isSeleccionadaPorDe() {
        return seleccionadaPorDe;
    }

    public void setSeleccionadaPorDe(boolean seleccionadaPorDe) {
        this.seleccionadaPorDe = seleccionadaPorDe;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
