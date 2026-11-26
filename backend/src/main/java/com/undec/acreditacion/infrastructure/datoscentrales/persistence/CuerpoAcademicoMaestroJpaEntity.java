package com.undec.acreditacion.infrastructure.datoscentrales.persistence;

import com.undec.acreditacion.infrastructure.designacion.persistence.DepartamentoAcademicoJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cuerpo_academico_maestro")
public class CuerpoAcademicoMaestroJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tipo_documento", length = 20, nullable = false)
    private String tipoDocumento = "DNI";

    @Column(name = "dni", length = 20, nullable = false, unique = true)
    private String dni;

    @Column(name = "cuit", length = 25)
    private String cuit;

    @Column(name = "legajo", length = 50)
    private String legajo;

    @Column(name = "nro_designacion_mapuche", length = 80)
    private String nroDesignacionMapuche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_academico_id", nullable = false)
    private DepartamentoAcademicoJpaEntity departamentoAcademico;

    @Column(name = "apellido", length = 100, nullable = false)
    private String apellido;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "email_institucional", length = 150)
    private String emailInstitucional;

    @Column(name = "email_personal", length = 150)
    private String emailPersonal;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "titulo_grado", length = 255)
    private String tituloGrado;

    @Column(name = "maximo_titulo_posgrado", length = 255)
    private String maximoTituloPosgrado;

    @Column(name = "categoria_docente", length = 50, nullable = false)
    private String categoriaDocente;

    @Column(name = "caracter_designacion", length = 50, nullable = false)
    private String caracterDesignacion;

    @Column(name = "dedicacion_docente", length = 50, nullable = false)
    private String dedicacionDocente;

    @Column(name = "horas_semanales", nullable = false)
    private Integer horasSemanales = 10;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    @OneToMany(mappedBy = "docente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CuerpoAcademicoActividadMaestroJpaEntity> asignaciones = new ArrayList<>();

    public CuerpoAcademicoMaestroJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getLegajo() {
        return legajo;
    }

    public void setLegajo(String legajo) {
        this.legajo = legajo;
    }

    public String getNroDesignacionMapuche() {
        return nroDesignacionMapuche;
    }

    public void setNroDesignacionMapuche(String nroDesignacionMapuche) {
        this.nroDesignacionMapuche = nroDesignacionMapuche;
    }

    public DepartamentoAcademicoJpaEntity getDepartamentoAcademico() {
        return departamentoAcademico;
    }

    public void setDepartamentoAcademico(DepartamentoAcademicoJpaEntity departamentoAcademico) {
        this.departamentoAcademico = departamentoAcademico;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmailInstitucional() {
        return emailInstitucional;
    }

    public void setEmailInstitucional(String emailInstitucional) {
        this.emailInstitucional = emailInstitucional;
    }

    public String getEmailPersonal() {
        return emailPersonal;
    }

    public void setEmailPersonal(String emailPersonal) {
        this.emailPersonal = emailPersonal;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTituloGrado() {
        return tituloGrado;
    }

    public void setTituloGrado(String tituloGrado) {
        this.tituloGrado = tituloGrado;
    }

    public String getMaximoTituloPosgrado() {
        return maximoTituloPosgrado;
    }

    public void setMaximoTituloPosgrado(String maximoTituloPosgrado) {
        this.maximoTituloPosgrado = maximoTituloPosgrado;
    }

    public String getCategoriaDocente() {
        return categoriaDocente;
    }

    public void setCategoriaDocente(String categoriaDocente) {
        this.categoriaDocente = categoriaDocente;
    }

    public String getCaracterDesignacion() {
        return caracterDesignacion;
    }

    public void setCaracterDesignacion(String caracterDesignacion) {
        this.caracterDesignacion = caracterDesignacion;
    }

    public String getDedicacionDocente() {
        return dedicacionDocente;
    }

    public void setDedicacionDocente(String dedicacionDocente) {
        this.dedicacionDocente = dedicacionDocente;
    }

    public Integer getHorasSemanales() {
        return horasSemanales;
    }

    public void setHorasSemanales(Integer horasSemanales) {
        this.horasSemanales = horasSemanales;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
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

    public List<CuerpoAcademicoActividadMaestroJpaEntity> getAsignaciones() {
        return asignaciones;
    }

    public void setAsignaciones(List<CuerpoAcademicoActividadMaestroJpaEntity> asignaciones) {
        this.asignaciones = asignaciones;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuerpoAcademicoMaestroJpaEntity that = (CuerpoAcademicoMaestroJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
