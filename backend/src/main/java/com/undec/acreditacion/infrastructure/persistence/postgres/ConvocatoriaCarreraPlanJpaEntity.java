package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "convocatoria_carrera_plan")
public class ConvocatoriaCarreraPlanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convocatoria_carrera_id", nullable = false)
    private ConvocatoriaCarreraJpaEntity convocatoriaCarrera;

    @Column(name = "carrera_id", nullable = false)
    private UUID carreraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_estudio_maestro_id", nullable = false)
    private PlanEstudioFt001JpaEntity planEstudioMaestro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_estudio_clon_id")
    private PlanEstudioFt001JpaEntity planEstudioClon;

    @Column(name = "sugerido_por_aea", nullable = false)
    private boolean sugeridoPorAea = true;

    @Column(name = "seleccionado_por_de", nullable = false)
    private boolean seleccionadoPorDe = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public ConvocatoriaCarreraPlanJpaEntity() {
    }

    public ConvocatoriaCarreraPlanJpaEntity(UUID id, ConvocatoriaCarreraJpaEntity convocatoriaCarrera, UUID carreraId,
                                            PlanEstudioFt001JpaEntity planEstudioMaestro, PlanEstudioFt001JpaEntity planEstudioClon,
                                            boolean sugeridoPorAea, boolean seleccionadoPorDe) {
        this.id = id;
        this.convocatoriaCarrera = convocatoriaCarrera;
        this.carreraId = carreraId;
        this.planEstudioMaestro = planEstudioMaestro;
        this.planEstudioClon = planEstudioClon;
        this.sugeridoPorAea = sugeridoPorAea;
        this.seleccionadoPorDe = seleccionadoPorDe;
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConvocatoriaCarreraJpaEntity getConvocatoriaCarrera() {
        return convocatoriaCarrera;
    }

    public void setConvocatoriaCarrera(ConvocatoriaCarreraJpaEntity convocatoriaCarrera) {
        this.convocatoriaCarrera = convocatoriaCarrera;
    }

    public UUID getCarreraId() {
        return carreraId;
    }

    public void setCarreraId(UUID carreraId) {
        this.carreraId = carreraId;
    }

    public PlanEstudioFt001JpaEntity getPlanEstudioMaestro() {
        return planEstudioMaestro;
    }

    public void setPlanEstudioMaestro(PlanEstudioFt001JpaEntity planEstudioMaestro) {
        this.planEstudioMaestro = planEstudioMaestro;
    }

    public PlanEstudioFt001JpaEntity getPlanEstudioClon() {
        return planEstudioClon;
    }

    public void setPlanEstudioClon(PlanEstudioFt001JpaEntity planEstudioClon) {
        this.planEstudioClon = planEstudioClon;
    }

    public boolean isSugeridoPorAea() {
        return sugeridoPorAea;
    }

    public void setSugeridoPorAea(boolean sugeridoPorAea) {
        this.sugeridoPorAea = sugeridoPorAea;
    }

    public boolean isSeleccionadoPorDe() {
        return seleccionadoPorDe;
    }

    public void setSeleccionadoPorDe(boolean seleccionadoPorDe) {
        this.seleccionadoPorDe = seleccionadoPorDe;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConvocatoriaCarreraPlanJpaEntity that = (ConvocatoriaCarreraPlanJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
