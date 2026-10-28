package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.GrupoUsuarioAtenea;
import com.undec.acreditacion.domain.entities.TipoAccesoAtenea;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios_permisos_atenea")
public class UsuarioPermisoAteneaJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_id", nullable = false)
    private RegistroUsuariosAteneaJpaEntity registro;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_usuario", nullable = false, length = 50)
    private GrupoUsuarioAtenea grupoUsuario;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "dni_cuil", nullable = false, length = 30)
    private String dniCuil;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "area_dependencia", length = 150)
    private String areaDependencia;

    @Column(name = "cargo_funcion", length = 150)
    private String cargoFuncion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acceso", nullable = false, length = 50)
    private TipoAccesoAtenea tipoAcceso;

    @Column(name = "seccion_alcance", length = 255)
    private String seccionAlcance;

    @Column(name = "requiere_cuenta_nueva_coneau_global", nullable = false)
    private boolean requiereCuentaNuevaConeauGlobal;

    @Column(name = "cuenta_coneau_global_creada", nullable = false)
    private boolean cuentaConeauGlobalCreada;

    @Column(name = "habilitado_en_coneau", nullable = false)
    private boolean habilitadoEnConeau;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public UsuarioPermisoAteneaJpaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RegistroUsuariosAteneaJpaEntity getRegistro() {
        return registro;
    }

    public void setRegistro(RegistroUsuariosAteneaJpaEntity registro) {
        this.registro = registro;
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
