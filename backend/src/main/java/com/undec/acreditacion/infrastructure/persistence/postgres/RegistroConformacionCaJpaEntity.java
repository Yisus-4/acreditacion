package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.EstadoRegistroCa;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "registro_conformacion_ca")
public class RegistroConformacionCaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id", nullable = false, unique = true)
    private UUID procesoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    private EstadoRegistroCa estado;

    @Column(name = "coincide_comision_curricular", nullable = false)
    private boolean coincideComisionCurricular;

    @Column(name = "solicitado_en")
    private Instant solicitadoEn;

    @Column(name = "solicitado_por")
    private UUID solicitadoPor;

    @Column(name = "fecha_limite_dc")
    private LocalDate fechaLimiteDc;

    @Column(name = "remitido_en")
    private Instant remitidoEn;

    @Column(name = "remitido_por")
    private UUID remitidoPor;

    @Column(name = "aprobado_en")
    private Instant aprobadoEn;

    @Column(name = "aprobado_por")
    private UUID aprobadoPor;

    @Column(name = "observaciones_revision", columnDefinition = "TEXT")
    private String observacionesRevision;

    @Column(name = "resolucion_storage_path", length = 500)
    private String resolucionStoragePath;

    @Column(name = "resolucion_nombre_original", length = 255)
    private String resolucionNombreOriginal;

    @Column(name = "resolucion_tamanio_bytes")
    private Long resolucionTamanioBytes;

    @Column(name = "registro_generado_storage_path", length = 500)
    private String registroGeneradoStoragePath;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @OneToMany(mappedBy = "registroCa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroOrden ASC")
    private List<MiembroComisionAutoevaluacionJpaEntity> miembros = new ArrayList<>();

    public RegistroConformacionCaJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public EstadoRegistroCa getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegistroCa estado) {
        this.estado = estado;
    }

    public boolean isCoincideComisionCurricular() {
        return coincideComisionCurricular;
    }

    public void setCoincideComisionCurricular(boolean coincideComisionCurricular) {
        this.coincideComisionCurricular = coincideComisionCurricular;
    }

    public Instant getSolicitadoEn() {
        return solicitadoEn;
    }

    public void setSolicitadoEn(Instant solicitadoEn) {
        this.solicitadoEn = solicitadoEn;
    }

    public UUID getSolicitadoPor() {
        return solicitadoPor;
    }

    public void setSolicitadoPor(UUID solicitadoPor) {
        this.solicitadoPor = solicitadoPor;
    }

    public LocalDate getFechaLimiteDc() {
        return fechaLimiteDc;
    }

    public void setFechaLimiteDc(LocalDate fechaLimiteDc) {
        this.fechaLimiteDc = fechaLimiteDc;
    }

    public Instant getRemitidoEn() {
        return remitidoEn;
    }

    public void setRemitidoEn(Instant remitidoEn) {
        this.remitidoEn = remitidoEn;
    }

    public UUID getRemitidoPor() {
        return remitidoPor;
    }

    public void setRemitidoPor(UUID remitidoPor) {
        this.remitidoPor = remitidoPor;
    }

    public Instant getAprobadoEn() {
        return aprobadoEn;
    }

    public void setAprobadoEn(Instant aprobadoEn) {
        this.aprobadoEn = aprobadoEn;
    }

    public UUID getAprobadoPor() {
        return aprobadoPor;
    }

    public void setAprobadoPor(UUID aprobadoPor) {
        this.aprobadoPor = aprobadoPor;
    }

    public String getObservacionesRevision() {
        return observacionesRevision;
    }

    public void setObservacionesRevision(String observacionesRevision) {
        this.observacionesRevision = observacionesRevision;
    }

    public String getResolucionStoragePath() {
        return resolucionStoragePath;
    }

    public void setResolucionStoragePath(String resolucionStoragePath) {
        this.resolucionStoragePath = resolucionStoragePath;
    }

    public String getResolucionNombreOriginal() {
        return resolucionNombreOriginal;
    }

    public void setResolucionNombreOriginal(String resolucionNombreOriginal) {
        this.resolucionNombreOriginal = resolucionNombreOriginal;
    }

    public Long getResolucionTamanioBytes() {
        return resolucionTamanioBytes;
    }

    public void setResolucionTamanioBytes(Long resolucionTamanioBytes) {
        this.resolucionTamanioBytes = resolucionTamanioBytes;
    }

    public String getRegistroGeneradoStoragePath() {
        return registroGeneradoStoragePath;
    }

    public void setRegistroGeneradoStoragePath(String registroGeneradoStoragePath) {
        this.registroGeneradoStoragePath = registroGeneradoStoragePath;
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

    public List<MiembroComisionAutoevaluacionJpaEntity> getMiembros() {
        return miembros;
    }

    public void setMiembros(List<MiembroComisionAutoevaluacionJpaEntity> miembros) {
        this.miembros = miembros;
    }
}
