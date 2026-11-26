package com.undec.acreditacion.infrastructure.designacion.persistence;

import com.undec.acreditacion.domain.entities.TipoEventoSolicitudDesignacion;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "solicitud_designacion_cambio")
public class SolicitudDesignacionCambioJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudDesignacionJpaEntity solicitud;

    @Column(name = "designacion_id")
    private UUID designacionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoSolicitudDesignacion tipoEvento;

    @Column(name = "campo_modificado", length = 100)
    private String campoModificado;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "usuario_nombre", length = 320)
    private String usuarioNombre;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private Instant fechaCambio = Instant.now();

    public SolicitudDesignacionCambioJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SolicitudDesignacionJpaEntity getSolicitud() { return solicitud; }
    public void setSolicitud(SolicitudDesignacionJpaEntity solicitud) { this.solicitud = solicitud; }

    public UUID getDesignacionId() { return designacionId; }
    public void setDesignacionId(UUID designacionId) { this.designacionId = designacionId; }

    public TipoEventoSolicitudDesignacion getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoSolicitudDesignacion tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getCampoModificado() { return campoModificado; }
    public void setCampoModificado(String campoModificado) { this.campoModificado = campoModificado; }

    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }

    public String getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(String valorNuevo) { this.valorNuevo = valorNuevo; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public Instant getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(Instant fechaCambio) { this.fechaCambio = fechaCambio; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolicitudDesignacionCambioJpaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
