package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class UsuarioPermisoAtenea {

    private UUID id;
    private UUID registroId;
    private GrupoUsuarioAtenea grupoUsuario;
    private String nombre;
    private String apellido;
    private String dniCuil;
    private String correo;
    private String areaDependencia;
    private String cargoFuncion;
    private TipoAccesoAtenea tipoAcceso;
    private String seccionAlcance;
    private boolean requiereCuentaNuevaConeauGlobal;
    private boolean cuentaConeauGlobalCreada;
    private boolean habilitadoEnConeau;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public UsuarioPermisoAtenea() {
        this.id = UUID.randomUUID();
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public static UsuarioPermisoAtenea crear(
            GrupoUsuarioAtenea grupoUsuario,
            String nombre,
            String apellido,
            String dniCuil,
            String correo,
            String areaDependencia,
            String cargoFuncion,
            TipoAccesoAtenea tipoAcceso,
            String seccionAlcance,
            boolean requiereCuentaNuevaConeauGlobal,
            boolean cuentaConeauGlobalCreada,
            boolean habilitadoEnConeau
    ) {
        if (grupoUsuario == null) {
            throw new DomainValidationException("El grupo de usuario es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainValidationException("El nombre del usuario es obligatorio");
        }
        if (apellido == null || apellido.isBlank()) {
            throw new DomainValidationException("El apellido del usuario es obligatorio");
        }
        if (dniCuil == null || dniCuil.isBlank()) {
            throw new DomainValidationException("El DNI/CUIL es obligatorio");
        }
        if (correo == null || correo.isBlank()) {
            throw new DomainValidationException("El correo es obligatorio");
        }

        UsuarioPermisoAtenea u = new UsuarioPermisoAtenea();
        u.id = UUID.randomUUID();
        u.grupoUsuario = grupoUsuario;
        u.nombre = nombre.trim();
        u.apellido = apellido.trim();
        u.dniCuil = dniCuil.trim();
        u.correo = correo.trim().toLowerCase();
        u.areaDependencia = areaDependencia != null ? areaDependencia.trim() : null;
        u.cargoFuncion = cargoFuncion != null ? cargoFuncion.trim() : null;
        u.tipoAcceso = tipoAcceso != null ? tipoAcceso : TipoAccesoAtenea.CARGA;
        u.seccionAlcance = seccionAlcance != null ? seccionAlcance.trim() : null;
        u.requiereCuentaNuevaConeauGlobal = requiereCuentaNuevaConeauGlobal;
        u.cuentaConeauGlobalCreada = cuentaConeauGlobalCreada;
        u.habilitadoEnConeau = habilitadoEnConeau;
        u.creadoEn = Instant.now();
        u.actualizadoEn = Instant.now();
        return u;
    }

    public static UsuarioPermisoAtenea rehydrate(
            UUID id,
            UUID registroId,
            GrupoUsuarioAtenea grupoUsuario,
            String nombre,
            String apellido,
            String dniCuil,
            String correo,
            String areaDependencia,
            String cargoFuncion,
            TipoAccesoAtenea tipoAcceso,
            String seccionAlcance,
            boolean requiereCuentaNuevaConeauGlobal,
            boolean cuentaConeauGlobalCreada,
            boolean habilitadoEnConeau,
            Instant creadoEn,
            Instant actualizadoEn
    ) {
        UsuarioPermisoAtenea u = new UsuarioPermisoAtenea();
        u.id = id;
        u.registroId = registroId;
        u.grupoUsuario = grupoUsuario;
        u.nombre = nombre;
        u.apellido = apellido;
        u.dniCuil = dniCuil;
        u.correo = correo;
        u.areaDependencia = areaDependencia;
        u.cargoFuncion = cargoFuncion;
        u.tipoAcceso = tipoAcceso;
        u.seccionAlcance = seccionAlcance;
        u.requiereCuentaNuevaConeauGlobal = requiereCuentaNuevaConeauGlobal;
        u.cuentaConeauGlobalCreada = cuentaConeauGlobalCreada;
        u.habilitadoEnConeau = habilitadoEnConeau;
        u.creadoEn = creadoEn;
        u.actualizadoEn = actualizadoEn;
        return u;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRegistroId() {
        return registroId;
    }

    public void setRegistroId(UUID registroId) {
        this.registroId = registroId;
    }

    public GrupoUsuarioAtenea getGrupoUsuario() {
        return grupoUsuario;
    }

    public void setGrupoUsuario(GrupoUsuarioAtenea grupoUsuario) {
        this.grupoUsuario = grupoUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombreCompleto() {
        return (apellido != null ? apellido + ", " : "") + (nombre != null ? nombre : "");
    }

    public String getDniCuil() {
        return dniCuil;
    }

    public void setDniCuil(String dniCuil) {
        this.dniCuil = dniCuil;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getAreaDependencia() {
        return areaDependencia;
    }

    public void setAreaDependencia(String areaDependencia) {
        this.areaDependencia = areaDependencia;
    }

    public String getCargoFuncion() {
        return cargoFuncion;
    }

    public void setCargoFuncion(String cargoFuncion) {
        this.cargoFuncion = cargoFuncion;
    }

    public TipoAccesoAtenea getTipoAcceso() {
        return tipoAcceso;
    }

    public void setTipoAcceso(TipoAccesoAtenea tipoAcceso) {
        this.tipoAcceso = tipoAcceso;
    }

    public String getSeccionAlcance() {
        return seccionAlcance;
    }

    public void setSeccionAlcance(String seccionAlcance) {
        this.seccionAlcance = seccionAlcance;
    }

    public boolean isRequiereCuentaNuevaConeauGlobal() {
        return requiereCuentaNuevaConeauGlobal;
    }

    public void setRequiereCuentaNuevaConeauGlobal(boolean requiereCuentaNuevaConeauGlobal) {
        this.requiereCuentaNuevaConeauGlobal = requiereCuentaNuevaConeauGlobal;
    }

    public boolean isCuentaConeauGlobalCreada() {
        return cuentaConeauGlobalCreada;
    }

    public void setCuentaConeauGlobalCreada(boolean cuentaConeauGlobalCreada) {
        this.cuentaConeauGlobalCreada = cuentaConeauGlobalCreada;
    }

    public boolean isHabilitadoEnConeau() {
        return habilitadoEnConeau;
    }

    public void setHabilitadoEnConeau(boolean habilitadoEnConeau) {
        this.habilitadoEnConeau = habilitadoEnConeau;
    }

    public Instant getCreadoEn() {
        return creadoEn;
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
        if (!(o instanceof UsuarioPermisoAtenea that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
