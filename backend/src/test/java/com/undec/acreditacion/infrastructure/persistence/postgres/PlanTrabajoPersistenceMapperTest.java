package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlanTrabajoPersistenceMapperTest {

    private final PlanTrabajoPersistenceMapper mapper = new PlanTrabajoPersistenceMapper();

    @Test
    @DisplayName("Debe serializar áreas informadas y documentación remitida separadas por ' | ' y restaurarlas")
    void debeHacerRoundTripDelRegistroDeComunicacion() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadInicial(UUID.randomUUID());
        actividad.registrarComunicacionYCompletar(
                List.of(AreaInformadaProceso.DIRECCION_CARRERA, AreaInformadaProceso.DIRECCIONES_INSTITUTOS),
                List.of(DocumentacionRemitidaProceso.RM_ESTANDARES, DocumentacionRemitidaProceso.ORDENANZAS_NOTAS_TECNICAS),
                Instant.parse("2026-03-10T03:00:00Z"),
                "Observación",
                UUID.randomUUID()
        );

        ActividadPlanTrabajoJpaEntity entity = mapper.toJpaActividad(actividad);

        assertEquals("DIRECCION_CARRERA | DIRECCIONES_INSTITUTOS", entity.getAreasInformadas());
        assertEquals("RM_ESTANDARES | ORDENANZAS_NOTAS_TECNICAS", entity.getDocumentacionRemitida());
        assertEquals("Observación", entity.getSintesisAcuerdos());

        ActividadPlanTrabajo restaurada = mapper.toDomainActividad(entity);

        assertEquals(actividad.getAreasInformadas(), restaurada.getAreasInformadas());
        assertEquals(actividad.getDocumentacionRemitida(), restaurada.getDocumentacionRemitida());
        assertEquals("Observación", restaurada.getObservaciones());
    }

    @Test
    @DisplayName("Debe tolerar columnas nulas o con códigos desconocidos (datos previos) sin fallar")
    void debeToleraDatosPrevios() {
        ActividadPlanTrabajo actividad = ActividadPlanTrabajo.crearActividadElaborarPlan(UUID.randomUUID());
        ActividadPlanTrabajoJpaEntity entity = mapper.toJpaActividad(actividad);
        assertNull(entity.getAreasInformadas());
        assertNull(entity.getDocumentacionRemitida());

        entity.setAreasInformadas("SECRETARIAS | CODIGO_OBSOLETO");
        ActividadPlanTrabajo restaurada = mapper.toDomainActividad(entity);

        assertEquals(List.of(AreaInformadaProceso.SECRETARIAS), restaurada.getAreasInformadas());
        assertTrue(restaurada.getDocumentacionRemitida().isEmpty());
    }
}
