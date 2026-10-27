package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RegistroConformacionCa {

    private UUID id;
    private UUID procesoId;
    private EstadoRegistroCa estado;
    private boolean coincideComisionCurricular;
    private Instant solicitadoEn;
    private UUID solicitadoPor;
    private LocalDate fechaLimiteDc;
    private Instant remitidoEn;
    private UUID remitidoPor;
    private Instant aprobadoEn;
    private UUID aprobadoPor;
    private String observacionesRevision;
    private String resolucionStoragePath;
    private String resolucionNombreOriginal;
    private Long resolucionTamanioBytes;
    private String registroGeneradoStoragePath;
    private Instant creadoEn;
    private Instant actualizadoEn;
    private final List<MiembroComisionAutoevaluacion> miembros = new ArrayList<>();

    public RegistroConformacionCa(UUID id, UUID procesoId) {
        if (procesoId == null) {
            throw new DomainValidationException("El proceso de acreditación es obligatorio");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.procesoId = procesoId;
        this.estado = EstadoRegistroCa.PENDIENTE_SOLICITUD;
        this.coincideComisionCurricular = false;
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public RegistroConformacionCa(
            UUID id,
            UUID procesoId,
            EstadoRegistroCa estado,
            boolean coincideComisionCurricular,
            Instant solicitadoEn,
            UUID solicitadoPor,
            LocalDate fechaLimiteDc,
            Instant remitidoEn,
            UUID remitidoPor,
            Instant aprobadoEn,
            UUID aprobadoPor,
            String observacionesRevision,
            String resolucionStoragePath,
            String resolucionNombreOriginal,
            Long resolucionTamanioBytes,
            String registroGeneradoStoragePath,
            Instant creadoEn,
            Instant actualizadoEn,
            List<MiembroComisionAutoevaluacion> miembros
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.procesoId = Objects.requireNonNull(procesoId, "El proceso de acreditación es obligatorio");
        this.estado = estado != null ? estado : EstadoRegistroCa.PENDIENTE_SOLICITUD;
        this.coincideComisionCurricular = coincideComisionCurricular;
        this.solicitadoEn = solicitadoEn;
        this.solicitadoPor = solicitadoPor;
        this.fechaLimiteDc = fechaLimiteDc;
        this.remitidoEn = remitidoEn;
        this.remitidoPor = remitidoPor;
        this.aprobadoEn = aprobadoEn;
        this.aprobadoPor = aprobadoPor;
        this.observacionesRevision = observacionesRevision;
        this.resolucionStoragePath = resolucionStoragePath;
        this.resolucionNombreOriginal = resolucionNombreOriginal;
        this.resolucionTamanioBytes = resolucionTamanioBytes;
        this.registroGeneradoStoragePath = registroGeneradoStoragePath;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : Instant.now();
        if (miembros != null) {
            this.miembros.addAll(miembros);
        }
    }

    /**
     * AEA solicita a la Dirección de Carrera (DC) la conformación de la CA.
     * Inicia el cómputo del plazo legal no mayor a 15 días corridos.
     */
    public void solicitar(UUID usuarioAeaId) {
        if (this.estado != EstadoRegistroCa.PENDIENTE_SOLICITUD && this.estado != EstadoRegistroCa.OBSERVADA) {
            throw new DomainValidationException("Solo se puede solicitar la conformación de CA si está pendiente o fue observada");
        }
        this.estado = EstadoRegistroCa.SOLICITADA_A_DC;
        this.solicitadoEn = Instant.now();
        this.solicitadoPor = usuarioAeaId;
        this.fechaLimiteDc = LocalDate.now().plusDays(15);
        this.actualizadoEn = Instant.now();
    }

    /**
     * Guarda borrador de la nómina y configuración de la CA.
     */
    public void guardarBorrador(boolean coincideCurricular, List<MiembroComisionAutoevaluacion> nuevosMiembros) {
        if (this.estado == EstadoRegistroCa.APROBADA) {
            throw new DomainValidationException("No se pueden modificar miembros de una comisión de autoevaluación ya aprobada");
        }
        this.coincideComisionCurricular = coincideCurricular;
        if (this.estado == EstadoRegistroCa.SOLICITADA_A_DC) {
            this.estado = EstadoRegistroCa.EN_CARGA_BORRADOR;
        }
        reemplazarMiembros(nuevosMiembros);
        this.actualizadoEn = Instant.now();
    }

    /**
     * Remite el registro formal a AEA para su auditoría y aprobación.
     */
    public void remitir(
            UUID usuarioDcId,
            boolean coincideCurricular,
            List<MiembroComisionAutoevaluacion> nuevosMiembros,
            String resolucionPath,
            String resolucionNombre,
            Long resolucionBytes
    ) {
        if (this.estado == EstadoRegistroCa.APROBADA) {
            throw new DomainValidationException("El registro ya ha sido aprobado previamente");
        }
        this.coincideComisionCurricular = coincideCurricular;
        reemplazarMiembros(nuevosMiembros);

        if (this.miembros.isEmpty()) {
            throw new DomainValidationException("Debe ingresar al menos un miembro para conformar la Comisión de Autoevaluación");
        }

        boolean tieneCoordinador = this.miembros.stream()
                .anyMatch(m -> m.getClaustroRol() == RolClaustroCa.COORDINADOR);
        if (!tieneCoordinador) {
            throw new DomainValidationException("La Comisión de Autoevaluación debe contar con al menos un miembro con rol COORDINADOR");
        }

        for (MiembroComisionAutoevaluacion m : this.miembros) {
            m.validarRequisitosParaRemision();
        }

        if (resolucionPath != null && !resolucionPath.isBlank()) {
            this.resolucionStoragePath = resolucionPath;
            this.resolucionNombreOriginal = resolucionNombre;
            this.resolucionTamanioBytes = resolucionBytes;
        } else if (this.resolucionStoragePath == null || this.resolucionStoragePath.isBlank()) {
            throw new DomainValidationException("Es obligatorio adjuntar la Resolución de designación para remitir la conformación de la CA");
        }

        this.estado = EstadoRegistroCa.REMITIDA_A_AEA;
        this.remitidoEn = Instant.now();
        this.remitidoPor = usuarioDcId;
        this.observacionesRevision = null;
        this.actualizadoEn = Instant.now();
    }

    /**
     * AEA audita y aprueba el registro formal de conformación.
     */
    public void aprobar(UUID usuarioAeaId, String pathRegistroGenerado) {
        if (this.estado != EstadoRegistroCa.REMITIDA_A_AEA) {
            throw new DomainValidationException("Solo se puede aprobar un registro de CA que haya sido remitido a AEA");
        }
        this.estado = EstadoRegistroCa.APROBADA;
        this.aprobadoEn = Instant.now();
        this.aprobadoPor = usuarioAeaId;
        this.registroGeneradoStoragePath = pathRegistroGenerado;
        this.observacionesRevision = null;
        this.actualizadoEn = Instant.now();
    }

    /**
     * AEA observa el registro y lo devuelve a la DC para subsanación.
     */
    public void observar(UUID usuarioAeaId, String motivo) {
        if (this.estado != EstadoRegistroCa.REMITIDA_A_AEA) {
            throw new DomainValidationException("Solo se puede observar un registro de CA que haya sido remitido a AEA");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DomainValidationException("Debe especificar el motivo u observaciones de la corrección solicitada");
        }
        this.estado = EstadoRegistroCa.OBSERVADA;
        this.observacionesRevision = motivo.trim();
        this.actualizadoEn = Instant.now();
    }

    private void reemplazarMiembros(List<MiembroComisionAutoevaluacion> nuevosMiembros) {
        this.miembros.clear();
        if (nuevosMiembros != null) {
            int orden = 1;
            for (MiembroComisionAutoevaluacion m : nuevosMiembros) {
                m.setRegistroCaId(this.id);
                m.setNumeroOrden(orden++);
                this.miembros.add(m);
            }
        }
    }

    public void actualizarResolucionAdjunta(String storagePath, String nombreOriginal, Long tamanioBytes) {
        this.resolucionStoragePath = storagePath;
        this.resolucionNombreOriginal = nombreOriginal;
        this.resolucionTamanioBytes = tamanioBytes;
        this.actualizadoEn = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public EstadoRegistroCa getEstado() {
        return estado;
    }

    public boolean isCoincideComisionCurricular() {
        return coincideComisionCurricular;
    }

    public Instant getSolicitadoEn() {
        return solicitadoEn;
    }

    public UUID getSolicitadoPor() {
        return solicitadoPor;
    }

    public LocalDate getFechaLimiteDc() {
        return fechaLimiteDc;
    }

    public Instant getRemitidoEn() {
        return remitidoEn;
    }

    public UUID getRemitidoPor() {
        return remitidoPor;
    }

    public Instant getAprobadoEn() {
        return aprobadoEn;
    }

    public UUID getAprobadoPor() {
        return aprobadoPor;
    }

    public String getObservacionesRevision() {
        return observacionesRevision;
    }

    public String getResolucionStoragePath() {
        return resolucionStoragePath;
    }

    public String getResolucionNombreOriginal() {
        return resolucionNombreOriginal;
    }

    public Long getResolucionTamanioBytes() {
        return resolucionTamanioBytes;
    }

    public String getRegistroGeneradoStoragePath() {
        return registroGeneradoStoragePath;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public List<MiembroComisionAutoevaluacion> getMiembros() {
        return Collections.unmodifiableList(miembros);
    }
}
