package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AutoevaluacionCarreraAmbitoDto {

    public record ActividadVinculadaResumen(UUID id, String codigo, String nombre) {}

    private UUID id;
    private UUID procesoId;
    private UUID carreraId;

    @JsonAlias({"ambitoPracticaId", "ambitoId"})
    private UUID ambitoId;

    private AmbitoPracticaDto ambito;
    private Boolean utilizadoPorCarrera;
    private String asignaturasVinculadas;
    private List<UUID> actividadCurricularIds;
    private List<ActividadVinculadaResumen> actividadesVinculadasDetalle;
    private List<String> asignaturasManuales;
    private String responsablePractica;
    private String horariosUsoAnioAnterior;

    @JsonAlias({"actividadesPrevistas", "tipoPracticaEstudiantes"})
    private String tipoPracticaEstudiantes;

    @JsonAlias({"observaciones", "observacionesCarrera"})
    private String observacionesCarrera;

    private Instant creadoEn;
    private Instant actualizadoEn;

    public AutoevaluacionCarreraAmbitoDto() {
    }

    public AutoevaluacionCarreraAmbitoDto(
            UUID id,
            UUID procesoId,
            UUID carreraId,
            UUID ambitoId,
            AmbitoPracticaDto ambito,
            Boolean utilizadoPorCarrera,
            String asignaturasVinculadas,
            String responsablePractica,
            String horariosUsoAnioAnterior,
            String tipoPracticaEstudiantes,
            String observacionesCarrera,
            Instant creadoEn,
            Instant actualizadoEn
    ) {
        this.id = id;
        this.procesoId = procesoId;
        this.carreraId = carreraId;
        this.ambitoId = ambitoId;
        this.ambito = ambito;
        this.utilizadoPorCarrera = utilizadoPorCarrera;
        this.asignaturasVinculadas = asignaturasVinculadas;
        this.responsablePractica = responsablePractica;
        this.horariosUsoAnioAnterior = horariosUsoAnioAnterior;
        this.tipoPracticaEstudiantes = tipoPracticaEstudiantes;
        this.observacionesCarrera = observacionesCarrera;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public AutoevaluacionCarreraAmbitoDto(
            UUID id,
            UUID procesoId,
            UUID ambitoId,
            AmbitoPracticaDto ambito,
            Boolean utilizadoPorCarrera,
            String asignaturasVinculadas,
            String responsablePractica,
            String horariosUsoAnioAnterior,
            String tipoPracticaEstudiantes,
            String observacionesCarrera,
            Instant creadoEn,
            Instant actualizadoEn
    ) {
        this(id, procesoId, null, ambitoId, ambito, utilizadoPorCarrera, asignaturasVinculadas, responsablePractica, horariosUsoAnioAnterior, tipoPracticaEstudiantes, observacionesCarrera, creadoEn, actualizadoEn);
    }

    // Record-style accessors
    public UUID id() {
        return id;
    }

    public UUID procesoId() {
        return procesoId;
    }

    public UUID carreraId() {
        return carreraId;
    }

    public UUID ambitoId() {
        return ambitoId;
    }

    public AmbitoPracticaDto ambito() {
        return ambito;
    }

    public Boolean utilizadoPorCarrera() {
        return utilizadoPorCarrera;
    }

    public String asignaturasVinculadas() {
        return asignaturasVinculadas;
    }

    public List<UUID> actividadCurricularIds() {
        return actividadCurricularIds;
    }

    public List<ActividadVinculadaResumen> actividadesVinculadasDetalle() {
        return actividadesVinculadasDetalle;
    }

    public List<String> asignaturasManuales() {
        return asignaturasManuales;
    }

    public String responsablePractica() {
        return responsablePractica;
    }

    public String horariosUsoAnioAnterior() {
        return horariosUsoAnioAnterior;
    }

    public String tipoPracticaEstudiantes() {
        return tipoPracticaEstudiantes;
    }

    public String observacionesCarrera() {
        return observacionesCarrera;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public Instant actualizadoEn() {
        return actualizadoEn;
    }

    // Standard JavaBeans getters
    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public UUID getCarreraId() {
        return carreraId;
    }

    public UUID getAmbitoId() {
        return ambitoId;
    }

    public AmbitoPracticaDto getAmbito() {
        return ambito;
    }

    public Boolean getUtilizadoPorCarrera() {
        return utilizadoPorCarrera;
    }

    public String getAsignaturasVinculadas() {
        return asignaturasVinculadas;
    }

    public List<UUID> getActividadCurricularIds() {
        return actividadCurricularIds;
    }

    public List<ActividadVinculadaResumen> getActividadesVinculadasDetalle() {
        return actividadesVinculadasDetalle;
    }

    public List<String> getAsignaturasManuales() {
        return asignaturasManuales;
    }

    public String getResponsablePractica() {
        return responsablePractica;
    }

    public String getHorariosUsoAnioAnterior() {
        return horariosUsoAnioAnterior;
    }

    public String getTipoPracticaEstudiantes() {
        return tipoPracticaEstudiantes;
    }

    public String getObservacionesCarrera() {
        return observacionesCarrera;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    // Dual-compatibility getters for JSON serialization
    @JsonProperty("ambitoPracticaId")
    public UUID getAmbitoPracticaId() {
        return ambitoId;
    }

    @JsonProperty("actividadesPrevistas")
    public String getActividadesPrevistas() {
        return tipoPracticaEstudiantes;
    }

    @JsonProperty("observaciones")
    public String getObservaciones() {
        return observacionesCarrera;
    }

    // Setters
    public void setId(UUID id) {
        this.id = id;
    }

    public void setProcesoId(UUID procesoId) {
        this.procesoId = procesoId;
    }

    public void setCarreraId(UUID carreraId) {
        this.carreraId = carreraId;
    }

    public void setAmbitoId(UUID ambitoId) {
        this.ambitoId = ambitoId;
    }

    public void setAmbitoPracticaId(UUID ambitoPracticaId) {
        if (this.ambitoId == null) {
            this.ambitoId = ambitoPracticaId;
        }
    }

    public void setAmbito(AmbitoPracticaDto ambito) {
        this.ambito = ambito;
    }

    public void setUtilizadoPorCarrera(Boolean utilizadoPorCarrera) {
        this.utilizadoPorCarrera = utilizadoPorCarrera;
    }

    public void setAsignaturasVinculadas(String asignaturasVinculadas) {
        this.asignaturasVinculadas = asignaturasVinculadas;
    }

    public void setActividadCurricularIds(List<UUID> actividadCurricularIds) {
        this.actividadCurricularIds = actividadCurricularIds;
    }

    public void setActividadesVinculadasDetalle(List<ActividadVinculadaResumen> actividadesVinculadasDetalle) {
        this.actividadesVinculadasDetalle = actividadesVinculadasDetalle;
    }

    public void setAsignaturasManuales(List<String> asignaturasManuales) {
        this.asignaturasManuales = asignaturasManuales;
    }

    public void setResponsablePractica(String responsablePractica) {
        this.responsablePractica = responsablePractica;
    }

    public void setHorariosUsoAnioAnterior(String horariosUsoAnioAnterior) {
        this.horariosUsoAnioAnterior = horariosUsoAnioAnterior;
    }

    public void setTipoPracticaEstudiantes(String tipoPracticaEstudiantes) {
        this.tipoPracticaEstudiantes = tipoPracticaEstudiantes;
    }

    public void setActividadesPrevistas(String actividadesPrevistas) {
        if (this.tipoPracticaEstudiantes == null) {
            this.tipoPracticaEstudiantes = actividadesPrevistas;
        }
    }

    public void setObservacionesCarrera(String observacionesCarrera) {
        this.observacionesCarrera = observacionesCarrera;
    }

    public void setObservaciones(String observaciones) {
        if (this.observacionesCarrera == null) {
            this.observacionesCarrera = observaciones;
        }
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutoevaluacionCarreraAmbitoDto that = (AutoevaluacionCarreraAmbitoDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AutoevaluacionCarreraAmbitoDto{" +
                "id=" + id +
                ", procesoId=" + procesoId +
                ", carreraId=" + carreraId +
                ", ambitoId=" + ambitoId +
                ", utilizadoPorCarrera=" + utilizadoPorCarrera +
                '}';
    }
}
