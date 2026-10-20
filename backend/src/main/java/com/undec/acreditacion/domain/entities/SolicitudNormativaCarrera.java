package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DocumentosFaltantesException;
import com.undec.acreditacion.domain.exceptions.EstadoInvalidoException;
import com.undec.acreditacion.domain.exceptions.NormativaValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SolicitudNormativaCarrera {

    private final UUID id;
    private final UUID procesoId;
    private final UUID actividadId;
    private EstadoSolicitudNormativa estado;
    private Instant fechaSolicitud;
    private UUID solicitadoPor;
    private LocalDate fechaLimiteEntrega;
    private int diasHabilesPlazo;
    private Instant fechaEnvio;
    private UUID enviadoPor;
    private CumplimientoPlazo cumplimientoPlazo;
    private String observacionesSolicitud;
    private String observacionesRevision;
    private Instant fechaRevision;
    private UUID revisadoPor;
    private final Instant creadoEn;
    private Instant actualizadoEn;

    public SolicitudNormativaCarrera(
        UUID id,
        UUID procesoId,
        UUID actividadId,
        EstadoSolicitudNormativa estado,
        Instant fechaSolicitud,
        UUID solicitadoPor,
        LocalDate fechaLimiteEntrega,
        int diasHabilesPlazo,
        Instant fechaEnvio,
        UUID enviadoPor,
        CumplimientoPlazo cumplimientoPlazo,
        String observacionesSolicitud,
        String observacionesRevision,
        Instant fechaRevision,
        UUID revisadoPor,
        Instant creadoEn,
        Instant actualizadoEn
    ) {
        this.id = Objects.requireNonNull(id, "El ID de la solicitud es obligatorio");
        this.procesoId = Objects.requireNonNull(procesoId, "El ID del proceso es obligatorio");
        this.actividadId = Objects.requireNonNull(actividadId, "El ID de la actividad es obligatorio");
        this.estado = estado != null ? estado : EstadoSolicitudNormativa.PENDIENTE_SOLICITUD;
        this.fechaSolicitud = fechaSolicitud;
        this.solicitadoPor = solicitadoPor;
        this.fechaLimiteEntrega = fechaLimiteEntrega;
        this.diasHabilesPlazo = diasHabilesPlazo > 0 ? diasHabilesPlazo : 5;
        this.fechaEnvio = fechaEnvio;
        this.enviadoPor = enviadoPor;
        this.cumplimientoPlazo = cumplimientoPlazo;
        this.observacionesSolicitud = observacionesSolicitud;
        this.observacionesRevision = observacionesRevision;
        this.fechaRevision = fechaRevision;
        this.revisadoPor = revisadoPor;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;
    }

    public static SolicitudNormativaCarrera iniciar(UUID procesoId, UUID actividadId) {
        Instant now = Instant.now();
        return new SolicitudNormativaCarrera(
            UUID.randomUUID(),
            procesoId,
            actividadId,
            EstadoSolicitudNormativa.PENDIENTE_SOLICITUD,
            null,
            null,
            null,
            5,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            now,
            now
        );
    }

    public void solicitar(UUID usuarioAeaId, String observaciones, int diasHabiles, LocalDate fechaInicioCalculo) {
        if (this.estado != EstadoSolicitudNormativa.PENDIENTE_SOLICITUD) {
            throw new EstadoInvalidoException("La solicitud ya fue emitida previamente (estado: " + this.estado + ")");
        }
        Objects.requireNonNull(usuarioAeaId, "El identificador del usuario solicitante es obligatorio");
        this.solicitadoPor = usuarioAeaId;
        this.fechaSolicitud = Instant.now();
        this.diasHabilesPlazo = diasHabiles > 0 ? diasHabiles : 5;
        LocalDate inicio = fechaInicioCalculo != null ? fechaInicioCalculo : LocalDate.now();
        this.fechaLimiteEntrega = CalculadorDiasHabiles.calcularFechaLimite(inicio, this.diasHabilesPlazo);
        this.observacionesSolicitud = observaciones != null ? observaciones.trim() : null;
        this.estado = EstadoSolicitudNormativa.SOLICITADA;
        this.actualizadoEn = Instant.now();
    }

    public void iniciarCargaBorrador() {
        if (this.estado == EstadoSolicitudNormativa.EN_CARGA_BORRADOR) {
            return;
        }
        if (this.estado != EstadoSolicitudNormativa.SOLICITADA && this.estado != EstadoSolicitudNormativa.OBSERVADA) {
            throw new EstadoInvalidoException("No se puede iniciar carga en borrador cuando la solicitud está en estado: " + this.estado);
        }
        this.estado = EstadoSolicitudNormativa.EN_CARGA_BORRADOR;
        this.actualizadoEn = Instant.now();
    }

    public void enviarDefinitivo(
        UUID usuarioDcId,
        LocalDate fechaActual,
        List<DocumentoNormativa> documentos,
        boolean esNueva,
        boolean conHistorialConeau
    ) {
        if (this.estado != EstadoSolicitudNormativa.EN_CARGA_BORRADOR 
            && this.estado != EstadoSolicitudNormativa.OBSERVADA 
            && this.estado != EstadoSolicitudNormativa.SOLICITADA) {
            throw new EstadoInvalidoException("No se puede enviar la solicitud definitiva en estado: " + this.estado);
        }

        Objects.requireNonNull(usuarioDcId, "El usuario que envía la documentación es obligatorio");
        LocalDate fecha = fechaActual != null ? fechaActual : LocalDate.now();

        List<TipoDocumentoNormativa> tiposCargados = documentos != null
            ? documentos.stream().map(DocumentoNormativa::getTipoDocumento).toList()
            : Collections.emptyList();

        List<TipoDocumentoNormativa> faltantes = MatrizNormativa.obtenerFaltantes(tiposCargados, esNueva, conHistorialConeau);
        if (!faltantes.isEmpty()) {
            throw new DocumentosFaltantesException(faltantes);
        }

        if (documentos != null) {
            for (DocumentoNormativa doc : documentos) {
                doc.marcarComoDefinitivo();
            }
        }

        this.estado = EstadoSolicitudNormativa.ENVIADA_A_REVISION;
        this.enviadoPor = usuarioDcId;
        this.fechaEnvio = Instant.now();

        if (this.fechaLimiteEntrega != null && fecha.isAfter(this.fechaLimiteEntrega)) {
            this.cumplimientoPlazo = CumplimientoPlazo.FUERA_DE_TERMINO;
        } else {
            this.cumplimientoPlazo = CumplimientoPlazo.EN_TERMINO;
        }
        this.actualizadoEn = Instant.now();
    }

    public void observar(UUID usuarioAeaId, String motivoRevision) {
        if (this.estado != EstadoSolicitudNormativa.ENVIADA_A_REVISION) {
            throw new EstadoInvalidoException("Solo se puede observar una solicitud en estado ENVIADA_A_REVISION (actual: " + this.estado + ")");
        }
        if (motivoRevision == null || motivoRevision.trim().length() < 10) {
            throw new NormativaValidationException("Las observaciones de revisión deben contener al menos 10 caracteres");
        }
        this.estado = EstadoSolicitudNormativa.OBSERVADA;
        this.revisadoPor = Objects.requireNonNull(usuarioAeaId, "El usuario auditor es obligatorio");
        this.observacionesRevision = motivoRevision.trim();
        this.fechaRevision = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public void aprobar(UUID usuarioAeaId, String observacionesRevision) {
        if (this.estado != EstadoSolicitudNormativa.ENVIADA_A_REVISION) {
            throw new EstadoInvalidoException("Solo se puede aprobar una solicitud en estado ENVIADA_A_REVISION (actual: " + this.estado + ")");
        }
        this.estado = EstadoSolicitudNormativa.COMPLETADA;
        this.revisadoPor = Objects.requireNonNull(usuarioAeaId, "El usuario auditor es obligatorio");
        this.observacionesRevision = observacionesRevision != null ? observacionesRevision.trim() : null;
        this.fechaRevision = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public BadgePlazoInfo calcularBadgePlazo(LocalDate fechaActual) {
        LocalDate hoy = fechaActual != null ? fechaActual : LocalDate.now();

        return switch (this.estado) {
            case PENDIENTE_SOLICITUD -> new BadgePlazoInfo("PENDIENTE", 0, "Pendiente de solicitud", "badge-secondary");
            case COMPLETADA -> BadgePlazoInfo.completado();
            case ENVIADA_A_REVISION -> {
                if (this.cumplimientoPlazo == CumplimientoPlazo.EN_TERMINO) {
                    yield BadgePlazoInfo.entregadoEnTermino();
                } else {
                    yield BadgePlazoInfo.entregadoFueraDeTermino();
                }
            }
            case OBSERVADA -> {
                if (this.fechaLimiteEntrega != null) {
                    int dias = CalculadorDiasHabiles.calcularDiasHabilesRestantes(hoy, this.fechaLimiteEntrega);
                    yield BadgePlazoInfo.enSubsanacion(dias);
                }
                yield new BadgePlazoInfo("EN_SUBSANACION", 0, "En subsanación", "badge-warning");
            }
            case SOLICITADA, EN_CARGA_BORRADOR -> {
                if (this.fechaLimiteEntrega == null) {
                    yield BadgePlazoInfo.enTermino(this.diasHabilesPlazo);
                }
                int dias = CalculadorDiasHabiles.calcularDiasHabilesRestantes(hoy, this.fechaLimiteEntrega);
                if (dias >= 0) {
                    yield BadgePlazoInfo.enTermino(dias);
                } else {
                    yield BadgePlazoInfo.fueraDeTermino(dias);
                }
            }
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public UUID getActividadId() {
        return actividadId;
    }

    public EstadoSolicitudNormativa getEstado() {
        return estado;
    }

    public Instant getFechaSolicitud() {
        return fechaSolicitud;
    }

    public UUID getSolicitadoPor() {
        return solicitadoPor;
    }

    public LocalDate getFechaLimiteEntrega() {
        return fechaLimiteEntrega;
    }

    public int getDiasHabilesPlazo() {
        return diasHabilesPlazo;
    }

    public Instant getFechaEnvio() {
        return fechaEnvio;
    }

    public UUID getEnviadoPor() {
        return enviadoPor;
    }

    public CumplimientoPlazo getCumplimientoPlazo() {
        return cumplimientoPlazo;
    }

    public String getObservacionesSolicitud() {
        return observacionesSolicitud;
    }

    public String getObservacionesRevision() {
        return observacionesRevision;
    }

    public Instant getFechaRevision() {
        return fechaRevision;
    }

    public UUID getRevisadoPor() {
        return revisadoPor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
