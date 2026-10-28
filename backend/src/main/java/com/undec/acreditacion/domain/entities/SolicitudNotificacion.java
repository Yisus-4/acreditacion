package com.undec.acreditacion.domain.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SolicitudNotificacion {

    public static final String TIPO_ALTA_MIEMBRO_CATALOGO = "ALTA_MIEMBRO_CATALOGO";
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ACEPTADA = "ACEPTADA";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";
    public static final String ROL_DESTINATARIO_AEA = "AEA";

    private UUID id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private String datosPayload;
    private UUID procesoId;
    private String carreraNombre;
    private UUID solicitadoPor;
    private String solicitadoPorNombre;
    private String destinatarioRol;
    private String estado;
    private boolean leida;
    private UUID resueltoPor;
    private Instant resueltoEn;
    private Instant creadoEn;

    public SolicitudNotificacion(
            UUID id,
            String tipo,
            String titulo,
            String mensaje,
            String datosPayload,
            UUID procesoId,
            String carreraNombre,
            UUID solicitadoPor,
            String solicitadoPorNombre,
            String destinatarioRol,
            String estado,
            boolean leida,
            UUID resueltoPor,
            Instant resueltoEn,
            Instant creadoEn
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.datosPayload = datosPayload;
        this.procesoId = procesoId;
        this.carreraNombre = carreraNombre;
        this.solicitadoPor = solicitadoPor;
        this.solicitadoPorNombre = solicitadoPorNombre;
        this.destinatarioRol = (destinatarioRol != null && !destinatarioRol.isBlank()) ? destinatarioRol : ROL_DESTINATARIO_AEA;
        this.estado = (estado != null && !estado.isBlank()) ? estado : ESTADO_PENDIENTE;
        this.leida = leida;
        this.resueltoPor = resueltoPor;
        this.resueltoEn = resueltoEn;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
    }

    public void aceptar(UUID resolutorId) {
        this.estado = ESTADO_ACEPTADA;
        this.leida = true;
        this.resueltoPor = resolutorId;
        this.resueltoEn = Instant.now();
    }

    public void rechazar(UUID resolutorId) {
        this.estado = ESTADO_RECHAZADA;
        this.leida = true;
        this.resueltoPor = resolutorId;
        this.resueltoEn = Instant.now();
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getDatosPayload() {
        return datosPayload;
    }

    public void setDatosPayload(String datosPayload) {
        this.datosPayload = datosPayload;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public String getCarreraNombre() {
        return carreraNombre;
    }

    public void setCarreraNombre(String carreraNombre) {
        this.carreraNombre = carreraNombre;
    }

    public UUID getSolicitadoPor() {
        return solicitadoPor;
    }

    public void setSolicitadoPor(UUID solicitadoPor) {
        this.solicitadoPor = solicitadoPor;
    }

    public String getSolicitadoPorNombre() {
        return solicitadoPorNombre;
    }

    public void setSolicitadoPorNombre(String solicitadoPorNombre) {
        this.solicitadoPorNombre = solicitadoPorNombre;
    }

    public String getDestinatarioRol() {
        return destinatarioRol;
    }

    public void setDestinatarioRol(String destinatarioRol) {
        this.destinatarioRol = destinatarioRol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public UUID getResueltoPor() {
        return resueltoPor;
    }

    public void setResueltoPor(UUID resueltoPor) {
        this.resueltoPor = resueltoPor;
    }

    public Instant getResueltoEn() {
        return resueltoEn;
    }

    public void setResueltoEn(Instant resueltoEn) {
        this.resueltoEn = resueltoEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolicitudNotificacion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
