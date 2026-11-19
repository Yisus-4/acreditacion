package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.AutoevaluacionCarreraAmbitoJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AutoevaluacionCarreraAmbitoDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("RED: Entity should support carreraId getter and setter")
    void deberiaPermitirSetearYObtenerCarreraIdEnEntity() {
        AutoevaluacionCarreraAmbitoJpaEntity entity = new AutoevaluacionCarreraAmbitoJpaEntity();
        UUID carreraId = UUID.randomUUID();
        entity.setCarreraId(carreraId);

        assertEquals(carreraId, entity.getCarreraId());
    }

    @Test
    @DisplayName("RED: DTO should deserialize legacy frontend aliases (ambitoPracticaId, actividadesPrevistas, observaciones)")
    void deberiaDeserializarPayloadConAliasesDelFrontend() throws Exception {
        UUID id = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        String json = """
                {
                  "id": "%s",
                  "procesoId": "%s",
                  "carreraId": "%s",
                  "ambitoPracticaId": "%s",
                  "utilizadoPorCarrera": true,
                  "asignaturasVinculadas": "Topografía I",
                  "responsablePractica": "Ing. Gómez",
                  "horariosUsoAnioAnterior": "Lun y Mierc 14-18",
                  "actividadesPrevistas": "Prácticas de campo con estación total",
                  "observaciones": "Convenio con estancia vigente"
                }
                """.formatted(id, procesoId, carreraId, ambitoId);

        AutoevaluacionCarreraAmbitoDto dto = objectMapper.readValue(json, AutoevaluacionCarreraAmbitoDto.class);

        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals(procesoId, dto.procesoId());
        assertEquals(carreraId, dto.carreraId());
        assertEquals(ambitoId, dto.ambitoId());
        assertEquals(ambitoId, dto.getAmbitoPracticaId());
        assertTrue(dto.utilizadoPorCarrera());
        assertEquals("Topografía I", dto.asignaturasVinculadas());
        assertEquals("Ing. Gómez", dto.responsablePractica());
        assertEquals("Lun y Mierc 14-18", dto.horariosUsoAnioAnterior());
        assertEquals("Prácticas de campo con estación total", dto.tipoPracticaEstudiantes());
        assertEquals("Prácticas de campo con estación total", dto.getActividadesPrevistas());
        assertEquals("Convenio con estancia vigente", dto.observacionesCarrera());
        assertEquals("Convenio con estancia vigente", dto.getObservaciones());
    }

    @Test
    @DisplayName("TRIANGULATE: DTO should deserialize canonical names and serialize both canonical and legacy compatibility keys")
    void deberiaDeserializarNombresCanonicosYSerializarAmbosJuegosDePropiedades() throws Exception {
        UUID id = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        String json = """
                {
                  "id": "%s",
                  "procesoId": "%s",
                  "carreraId": "%s",
                  "ambitoId": "%s",
                  "utilizadoPorCarrera": false,
                  "tipoPracticaEstudiantes": "Actividades de laboratorio",
                  "observacionesCarrera": "Observación canónica"
                }
                """.formatted(id, procesoId, carreraId, ambitoId);

        AutoevaluacionCarreraAmbitoDto dto = objectMapper.readValue(json, AutoevaluacionCarreraAmbitoDto.class);

        assertNotNull(dto);
        assertEquals(ambitoId, dto.ambitoId());
        assertEquals("Actividades de laboratorio", dto.tipoPracticaEstudiantes());
        assertEquals("Observación canónica", dto.observacionesCarrera());

        // Serialize back to JSON and verify dual keys
        String serialized = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(serialized);

        assertEquals(ambitoId.toString(), node.get("ambitoId").asText());
        assertEquals(ambitoId.toString(), node.get("ambitoPracticaId").asText());
        assertEquals("Actividades de laboratorio", node.get("tipoPracticaEstudiantes").asText());
        assertEquals("Actividades de laboratorio", node.get("actividadesPrevistas").asText());
        assertEquals("Observación canónica", node.get("observacionesCarrera").asText());
        assertEquals("Observación canónica", node.get("observaciones").asText());
    }

    @Test
    @DisplayName("DTO serializa y deserializa actividadCurricularIds, actividadesVinculadasDetalle y asignaturasManuales")
    void deberiaSerializarYDeserializarCamposRelacionalesYManuales() throws Exception {
        UUID actId = UUID.randomUUID();
        UUID ambitoId = UUID.randomUUID();

        String json = """
                {
                  "ambitoId": "%s",
                  "utilizadoPorCarrera": true,
                  "actividadCurricularIds": ["%s"],
                  "asignaturasManuales": ["Taller Optativo"]
                }
                """.formatted(ambitoId, actId);

        AutoevaluacionCarreraAmbitoDto dto = objectMapper.readValue(json, AutoevaluacionCarreraAmbitoDto.class);
        assertEquals(List.of(actId), dto.getActividadCurricularIds());
        assertEquals(List.of("Taller Optativo"), dto.getAsignaturasManuales());

        dto.setActividadesVinculadasDetalle(List.of(
                new AutoevaluacionCarreraAmbitoDto.ActividadVinculadaResumen(actId, "101", "Análisis 1")
        ));

        String serialized = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(serialized);
        assertEquals(actId.toString(), node.get("actividadCurricularIds").get(0).asText());
        assertEquals("101", node.get("actividadesVinculadasDetalle").get(0).get("codigo").asText());
        assertEquals("Análisis 1", node.get("actividadesVinculadasDetalle").get(0).get("nombre").asText());
        assertEquals("Taller Optativo", node.get("asignaturasManuales").get(0).asText());
    }
}
