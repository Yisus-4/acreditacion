package com.undec.acreditacion.infrastructure.persistence.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ficha_formalizacion_ft012")
public class FichaFormalizacionFt012JpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proceso_id", nullable = false)
    private UUID procesoId;

    @Column(name = "modalidad_dictado", nullable = false, length = 50)
    private String modalidadDictado = "PRESENCIAL";

    @Column(name = "estado", nullable = false, length = 50)
    private String estado = "PENDIENTE_SOLICITUD";

    // 1. Información institucional
    @Column(name = "institucion_universitaria", nullable = false, length = 200)
    private String institucionUniversitaria = "Universidad Nacional de Chilecito";

    @Column(name = "autoridad_apellido", nullable = false, length = 100)
    private String autoridadApellido = "Salcedo";

    @Column(name = "autoridad_primer_nombre", nullable = false, length = 100)
    private String autoridadPrimerNombre = "César";

    @Column(name = "autoridad_segundo_nombre", length = 100)
    private String autoridadSegundoNombre = "Alberto";

    @Column(name = "autoridad_correo", nullable = false, length = 150)
    private String autoridadCorreo = "csalcedo@undec.edu.ar";

    @Column(name = "autoridad_telefono", length = 50)
    private String autoridadTelefono;

    @Column(name = "unidad_academica", nullable = false, length = 200)
    private String unidadAcademica = "Universidad Nacional de Chilecito";

    @Column(name = "sede_calle", nullable = false, length = 150)
    private String sedeCalle = "9 de Julio";

    @Column(name = "sede_numero", nullable = false, length = 30)
    private String sedeNumero = "22";

    @Column(name = "sede_piso", length = 30)
    private String sedePiso;

    @Column(name = "sede_departamento", length = 30)
    private String sedeDepartamento;

    @Column(name = "sede_codigo_postal", nullable = false, length = 20)
    private String sedeCodigoPostal = "5360";

    @Column(name = "sede_provincia", nullable = false, length = 100)
    private String sedeProvincia = "La Rioja";

    @Column(name = "sede_localidad", nullable = false, length = 100)
    private String sedeLocalidad = "Chilecito";

    @Column(name = "sede_departamento_partido", nullable = false, length = 100)
    private String sedeDepartamentoPartido = "Chilecito";

    @Column(name = "nexo_apellido", length = 100)
    private String nexoApellido;

    @Column(name = "nexo_primer_nombre", length = 100)
    private String nexoPrimerNombre;

    @Column(name = "nexo_segundo_nombre", length = 100)
    private String nexoSegundoNombre;

    @Column(name = "nexo_telefono", length = 50)
    private String nexoTelefono;

    @Column(name = "nexo_cargo", length = 100)
    private String nexoCargo;

    @Column(name = "nexo_correo", length = 150)
    private String nexoCorreo;

    // 2. Información por carrera
    @Column(name = "carrera_nombre", length = 200)
    private String carreraNombre;

    @Column(name = "titulo_asimilado", length = 200)
    private String tituloAsimilado;

    @Column(name = "carga_horaria_no_presencial", length = 100)
    private String cargaHorariaNoPresencial;

    @Column(name = "anio_inicio_dictado")
    private Integer anioInicioDictado;

    @Column(name = "responsable_apellido", length = 100)
    private String responsableApellido;

    @Column(name = "responsable_primer_nombre", length = 100)
    private String responsablePrimerNombre;

    @Column(name = "responsable_segundo_nombre", length = 100)
    private String responsableSegundoNombre;

    @Column(name = "responsable_telefono", length = 50)
    private String responsableTelefono;

    @Column(name = "responsable_correo", length = 150)
    private String responsableCorreo;

    @Column(name = "responsable_cargo", length = 100)
    private String responsableCargo = "Director";

    @Column(name = "coordinador_ca_apellido", length = 100)
    private String coordinadorCaApellido;

    @Column(name = "coordinador_ca_primer_nombre", length = 100)
    private String coordinadorCaPrimerNombre;

    @Column(name = "coordinador_ca_segundo_nombre", length = 100)
    private String coordinadorCaSegundoNombre;

    @Column(name = "coordinador_ca_telefono", length = 50)
    private String coordinadorCaTelefono;

    @Column(name = "coordinador_ca_correo", length = 150)
    private String coordinadorCaCorreo;

    // Comprobante CONEAU Global ATENEA y fechas
    @Column(name = "comprobante_nombre", length = 255)
    private String comprobanteNombre;

    @Column(name = "comprobante_storage_path", length = 500)
    private String comprobanteStoragePath;

    @Column(name = "comprobante_tamanio_bytes")
    private Long comprobanteTamanioBytes;

    @Column(name = "fecha_formalizacion_electronica")
    private LocalDate fechaFormalizacionElectronica;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "solicitado_por")
    private UUID solicitadoPor;

    @Column(name = "solicitado_en")
    private Instant solicitadoEn;

    @Column(name = "remitido_por")
    private UUID remitidoPor;

    @Column(name = "remitido_en")
    private Instant remitidoEn;

    @Column(name = "formalizado_por")
    private UUID formalizadoPor;

    @Column(name = "formalizado_en")
    private Instant formalizadoEn;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public FichaFormalizacionFt012JpaEntity() {
    }

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

    public String getModalidadDictado() {
        return modalidadDictado;
    }

    public void setModalidadDictado(String modalidadDictado) {
        this.modalidadDictado = modalidadDictado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getInstitucionUniversitaria() {
        return institucionUniversitaria;
    }

    public void setInstitucionUniversitaria(String institucionUniversitaria) {
        this.institucionUniversitaria = institucionUniversitaria;
    }

    public String getAutoridadApellido() {
        return autoridadApellido;
    }

    public void setAutoridadApellido(String autoridadApellido) {
        this.autoridadApellido = autoridadApellido;
    }

    public String getAutoridadPrimerNombre() {
        return autoridadPrimerNombre;
    }

    public void setAutoridadPrimerNombre(String autoridadPrimerNombre) {
        this.autoridadPrimerNombre = autoridadPrimerNombre;
    }

    public String getAutoridadSegundoNombre() {
        return autoridadSegundoNombre;
    }

    public void setAutoridadSegundoNombre(String autoridadSegundoNombre) {
        this.autoridadSegundoNombre = autoridadSegundoNombre;
    }

    public String getAutoridadCorreo() {
        return autoridadCorreo;
    }

    public void setAutoridadCorreo(String autoridadCorreo) {
        this.autoridadCorreo = autoridadCorreo;
    }

    public String getAutoridadTelefono() {
        return autoridadTelefono;
    }

    public void setAutoridadTelefono(String autoridadTelefono) {
        this.autoridadTelefono = autoridadTelefono;
    }

    public String getUnidadAcademica() {
        return unidadAcademica;
    }

    public void setUnidadAcademica(String unidadAcademica) {
        this.unidadAcademica = unidadAcademica;
    }

    public String getSedeCalle() {
        return sedeCalle;
    }

    public void setSedeCalle(String sedeCalle) {
        this.sedeCalle = sedeCalle;
    }

    public String getSedeNumero() {
        return sedeNumero;
    }

    public void setSedeNumero(String sedeNumero) {
        this.sedeNumero = sedeNumero;
    }

    public String getSedePiso() {
        return sedePiso;
    }

    public void setSedePiso(String sedePiso) {
        this.sedePiso = sedePiso;
    }

    public String getSedeDepartamento() {
        return sedeDepartamento;
    }

    public void setSedeDepartamento(String sedeDepartamento) {
        this.sedeDepartamento = sedeDepartamento;
    }

    public String getSedeCodigoPostal() {
        return sedeCodigoPostal;
    }

    public void setSedeCodigoPostal(String sedeCodigoPostal) {
        this.sedeCodigoPostal = sedeCodigoPostal;
    }

    public String getSedeProvincia() {
        return sedeProvincia;
    }

    public void setSedeProvincia(String sedeProvincia) {
        this.sedeProvincia = sedeProvincia;
    }

    public String getSedeLocalidad() {
        return sedeLocalidad;
    }

    public void setSedeLocalidad(String sedeLocalidad) {
        this.sedeLocalidad = sedeLocalidad;
    }

    public String getSedeDepartamentoPartido() {
        return sedeDepartamentoPartido;
    }

    public void setSedeDepartamentoPartido(String sedeDepartamentoPartido) {
        this.sedeDepartamentoPartido = sedeDepartamentoPartido;
    }

    public String getNexoApellido() {
        return nexoApellido;
    }

    public void setNexoApellido(String nexoApellido) {
        this.nexoApellido = nexoApellido;
    }

    public String getNexoPrimerNombre() {
        return nexoPrimerNombre;
    }

    public void setNexoPrimerNombre(String nexoPrimerNombre) {
        this.nexoPrimerNombre = nexoPrimerNombre;
    }

    public String getNexoSegundoNombre() {
        return nexoSegundoNombre;
    }

    public void setNexoSegundoNombre(String nexoSegundoNombre) {
        this.nexoSegundoNombre = nexoSegundoNombre;
    }

    public String getNexoTelefono() {
        return nexoTelefono;
    }

    public void setNexoTelefono(String nexoTelefono) {
        this.nexoTelefono = nexoTelefono;
    }

    public String getNexoCargo() {
        return nexoCargo;
    }

    public void setNexoCargo(String nexoCargo) {
        this.nexoCargo = nexoCargo;
    }

    public String getNexoCorreo() {
        return nexoCorreo;
    }

    public void setNexoCorreo(String nexoCorreo) {
        this.nexoCorreo = nexoCorreo;
    }

    public String getCarreraNombre() {
        return carreraNombre;
    }

    public void setCarreraNombre(String carreraNombre) {
        this.carreraNombre = carreraNombre;
    }

    public String getTituloAsimilado() {
        return tituloAsimilado;
    }

    public void setTituloAsimilado(String tituloAsimilado) {
        this.tituloAsimilado = tituloAsimilado;
    }

    public String getCargaHorariaNoPresencial() {
        return cargaHorariaNoPresencial;
    }

    public void setCargaHorariaNoPresencial(String cargaHorariaNoPresencial) {
        this.cargaHorariaNoPresencial = cargaHorariaNoPresencial;
    }

    public Integer getAnioInicioDictado() {
        return anioInicioDictado;
    }

    public void setAnioInicioDictado(Integer anioInicioDictado) {
        this.anioInicioDictado = anioInicioDictado;
    }

    public String getResponsableApellido() {
        return responsableApellido;
    }

    public void setResponsableApellido(String responsableApellido) {
        this.responsableApellido = responsableApellido;
    }

    public String getResponsablePrimerNombre() {
        return responsablePrimerNombre;
    }

    public void setResponsablePrimerNombre(String responsablePrimerNombre) {
        this.responsablePrimerNombre = responsablePrimerNombre;
    }

    public String getResponsableSegundoNombre() {
        return responsableSegundoNombre;
    }

    public void setResponsableSegundoNombre(String responsableSegundoNombre) {
        this.responsableSegundoNombre = responsableSegundoNombre;
    }

    public String getResponsableTelefono() {
        return responsableTelefono;
    }

    public void setResponsableTelefono(String responsableTelefono) {
        this.responsableTelefono = responsableTelefono;
    }

    public String getResponsableCorreo() {
        return responsableCorreo;
    }

    public void setResponsableCorreo(String responsableCorreo) {
        this.responsableCorreo = responsableCorreo;
    }

    public String getResponsableCargo() {
        return responsableCargo;
    }

    public void setResponsableCargo(String responsableCargo) {
        this.responsableCargo = responsableCargo;
    }

    public String getCoordinadorCaApellido() {
        return coordinadorCaApellido;
    }

    public void setCoordinadorCaApellido(String coordinadorCaApellido) {
        this.coordinadorCaApellido = coordinadorCaApellido;
    }

    public String getCoordinadorCaPrimerNombre() {
        return coordinadorCaPrimerNombre;
    }

    public void setCoordinadorCaPrimerNombre(String coordinadorCaPrimerNombre) {
        this.coordinadorCaPrimerNombre = coordinadorCaPrimerNombre;
    }

    public String getCoordinadorCaSegundoNombre() {
        return coordinadorCaSegundoNombre;
    }

    public void setCoordinadorCaSegundoNombre(String coordinadorCaSegundoNombre) {
        this.coordinadorCaSegundoNombre = coordinadorCaSegundoNombre;
    }

    public String getCoordinadorCaTelefono() {
        return coordinadorCaTelefono;
    }

    public void setCoordinadorCaTelefono(String coordinadorCaTelefono) {
        this.coordinadorCaTelefono = coordinadorCaTelefono;
    }

    public String getCoordinadorCaCorreo() {
        return coordinadorCaCorreo;
    }

    public void setCoordinadorCaCorreo(String coordinadorCaCorreo) {
        this.coordinadorCaCorreo = coordinadorCaCorreo;
    }

    public String getComprobanteNombre() {
        return comprobanteNombre;
    }

    public void setComprobanteNombre(String comprobanteNombre) {
        this.comprobanteNombre = comprobanteNombre;
    }

    public String getComprobanteStoragePath() {
        return comprobanteStoragePath;
    }

    public void setComprobanteStoragePath(String comprobanteStoragePath) {
        this.comprobanteStoragePath = comprobanteStoragePath;
    }

    public Long getComprobanteTamanioBytes() {
        return comprobanteTamanioBytes;
    }

    public void setComprobanteTamanioBytes(Long comprobanteTamanioBytes) {
        this.comprobanteTamanioBytes = comprobanteTamanioBytes;
    }

    public LocalDate getFechaFormalizacionElectronica() {
        return fechaFormalizacionElectronica;
    }

    public void setFechaFormalizacionElectronica(LocalDate fechaFormalizacionElectronica) {
        this.fechaFormalizacionElectronica = fechaFormalizacionElectronica;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public UUID getSolicitadoPor() {
        return solicitadoPor;
    }

    public void setSolicitadoPor(UUID solicitadoPor) {
        this.solicitadoPor = solicitadoPor;
    }

    public Instant getSolicitadoEn() {
        return solicitadoEn;
    }

    public void setSolicitadoEn(Instant solicitadoEn) {
        this.solicitadoEn = solicitadoEn;
    }

    public UUID getRemitidoPor() {
        return remitidoPor;
    }

    public void setRemitidoPor(UUID remitidoPor) {
        this.remitidoPor = remitidoPor;
    }

    public Instant getRemitidoEn() {
        return remitidoEn;
    }

    public void setRemitidoEn(Instant remitidoEn) {
        this.remitidoEn = remitidoEn;
    }

    public UUID getFormalizadoPor() {
        return formalizadoPor;
    }

    public void setFormalizadoPor(UUID formalizadoPor) {
        this.formalizadoPor = formalizadoPor;
    }

    public Instant getFormalizadoEn() {
        return formalizadoEn;
    }

    public void setFormalizadoEn(Instant formalizadoEn) {
        this.formalizadoEn = formalizadoEn;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FichaFormalizacionFt012JpaEntity that = (FichaFormalizacionFt012JpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
