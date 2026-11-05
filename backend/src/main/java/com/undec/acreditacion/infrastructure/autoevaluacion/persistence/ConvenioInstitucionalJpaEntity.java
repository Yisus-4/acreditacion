package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "convenio_institucional")
public class ConvenioInstitucionalJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "expediente", length = 100)
    private String expediente;

    @Column(name = "resolucion_rectoral", length = 150)
    private String resolucionRectoral;

    @Column(name = "contraparte", nullable = false, length = 255)
    private String contraparte;

    @Column(name = "fecha_celebracion")
    private LocalDate fechaCelebracion;

    @Column(name = "duracion", length = 150)
    private String duracion;

    @Column(name = "fecha_cese")
    private LocalDate fechaCese;

    @Column(name = "objeto", columnDefinition = "TEXT")
    private String objeto;

    @Column(name = "vigente", nullable = false)
    private Boolean vigente = true;

    @Column(name = "estado_vigencia", length = 50)
    private String estadoVigencia = "VIGENTE";

    @Column(name = "estado_ejecucion", length = 100)
    private String estadoEjecucion;

    @Column(name = "financiamiento", length = 150)
    private String financiamiento;

    @Column(name = "responsable_undec", length = 150)
    private String responsableUndec;

    @Column(name = "objetivos_generales", columnDefinition = "TEXT")
    private String objetivosGenerales;

    @Column(name = "objetivos_especificos", columnDefinition = "TEXT")
    private String objetivosEspecificos;

    @Column(name = "resultados_esperados", columnDefinition = "TEXT")
    private String resultadosEsperados;

    @Column(name = "carreras_beneficiadas", columnDefinition = "TEXT")
    private String carrerasBeneficiadas;

    @Column(name = "tipo_convenio", length = 100)
    private String tipoConvenio;

    @Column(name = "archivo_id")
    private UUID archivoId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public ConvenioInstitucionalJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExpediente() {
        return expediente;
    }

    public void setExpediente(String expediente) {
        this.expediente = expediente;
    }

    public String getResolucionRectoral() {
        return resolucionRectoral;
    }

    public void setResolucionRectoral(String resolucionRectoral) {
        this.resolucionRectoral = resolucionRectoral;
    }

    public String getContraparte() {
        return contraparte;
    }

    public void setContraparte(String contraparte) {
        this.contraparte = contraparte;
    }

    public LocalDate getFechaCelebracion() {
        return fechaCelebracion;
    }

    public void setFechaCelebracion(LocalDate fechaCelebracion) {
        this.fechaCelebracion = fechaCelebracion;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public LocalDate getFechaCese() {
        return fechaCese;
    }

    public void setFechaCese(LocalDate fechaCese) {
        this.fechaCese = fechaCese;
    }

    public String getObjeto() {
        return objeto;
    }

    public void setObjeto(String objeto) {
        this.objeto = objeto;
    }

    public Boolean getVigente() {
        return vigente;
    }

    public void setVigente(Boolean vigente) {
        this.vigente = vigente;
    }

    public String getEstadoVigencia() {
        return estadoVigencia;
    }

    public void setEstadoVigencia(String estadoVigencia) {
        this.estadoVigencia = estadoVigencia;
    }

    public String getEstadoEjecucion() {
        return estadoEjecucion;
    }

    public void setEstadoEjecucion(String estadoEjecucion) {
        this.estadoEjecucion = estadoEjecucion;
    }

    public String getFinanciamiento() {
        return financiamiento;
    }

    public void setFinanciamiento(String financiamiento) {
        this.financiamiento = financiamiento;
    }

    public String getResponsableUndec() {
        return responsableUndec;
    }

    public void setResponsableUndec(String responsableUndec) {
        this.responsableUndec = responsableUndec;
    }

    public String getObjetivosGenerales() {
        return objetivosGenerales;
    }

    public void setObjetivosGenerales(String objetivosGenerales) {
        this.objetivosGenerales = objetivosGenerales;
    }

    public String getObjetivosEspecificos() {
        return objetivosEspecificos;
    }

    public void setObjetivosEspecificos(String objetivosEspecificos) {
        this.objetivosEspecificos = objetivosEspecificos;
    }

    public String getResultadosEsperados() {
        return resultadosEsperados;
    }

    public void setResultadosEsperados(String resultadosEsperados) {
        this.resultadosEsperados = resultadosEsperados;
    }

    public String getCarrerasBeneficiadas() {
        return carrerasBeneficiadas;
    }

    public void setCarrerasBeneficiadas(String carrerasBeneficiadas) {
        this.carrerasBeneficiadas = carrerasBeneficiadas;
    }

    public String getTipoConvenio() {
        return tipoConvenio;
    }

    public void setTipoConvenio(String tipoConvenio) {
        this.tipoConvenio = tipoConvenio;
    }

    public UUID getArchivoId() {
        return archivoId;
    }

    public void setArchivoId(UUID archivoId) {
        this.archivoId = archivoId;
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
        ConvenioInstitucionalJpaEntity that = (ConvenioInstitucionalJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
