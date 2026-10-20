package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.PlantillaPlanTrabajoDto;
import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.TipoPlanTrabajo;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObtenerPlantillaPlanTrabajoServiceTest {

    private PlanTrabajoRepositoryPort planTrabajoRepository;
    private ObtenerPlantillaPlanTrabajoService service;

    @BeforeEach
    void setUp() {
        planTrabajoRepository = mock(PlanTrabajoRepositoryPort.class);
        service = new ObtenerPlantillaPlanTrabajoService(planTrabajoRepository);
    }

    @Test
    @DisplayName("Debe retornar la plantilla estándar para PT_001_ACREDITACION con 31 actividades INFO y 14 CONEAU")
    void debeRetornarPlantillaAcreditacion() {
        PlantillaPlanTrabajoDto plantilla = service.obtenerPorTipo(TipoPlanTrabajo.PT_001_ACREDITACION);

        assertNotNull(plantilla);
        assertEquals(TipoPlanTrabajo.PT_001_ACREDITACION, plantilla.tipoPlan());
        assertEquals(31, plantilla.actividadesInfo().size());
        assertEquals(14, plantilla.actividadesConeau().size());

        assertTrue(plantilla.actividadesInfo().stream().allMatch(a -> a.hoja() == HojaPlanTrabajo.INFO));
        assertTrue(plantilla.actividadesConeau().stream().allMatch(a -> a.hoja() == HojaPlanTrabajo.CONEAU));

        // Verificar actividades específicas de INFO
        assertTrue(plantilla.actividadesInfo().stream()
                .anyMatch(a -> a.nombre().contains("Gestionar normativa de la carrera")));
        assertTrue(plantilla.actividadesInfo().stream()
                .anyMatch(a -> a.nombre().contains("Gestionar información sobre infraestructura")));
        assertTrue(plantilla.actividadesInfo().stream()
                .anyMatch(a -> a.nombre().contains("programas analíticos de las asignaturas")));
        assertTrue(plantilla.actividadesInfo().stream()
                .anyMatch(a -> a.nombre().contains("Elaborar el informe de autoevaluación")));
        assertTrue(plantilla.actividadesInfo().stream()
                .anyMatch(a -> a.nombre().contains("Elaborar plan de mejoras")));

        // Verificar actividades específicas de CONEAU
        assertTrue(plantilla.actividadesConeau().stream()
                .anyMatch(a -> a.nombre().contains("información institucional precargada")));
        assertTrue(plantilla.actividadesConeau().stream()
                .anyMatch(a -> a.nombre().contains("Creación de la solicitud de acreditación")));
        assertTrue(plantilla.actividadesConeau().stream()
                .anyMatch(a -> a.nombre().contains("CONEAU Global ATENEA (Infraestructura)")));
        assertTrue(plantilla.actividadesConeau().stream()
                .anyMatch(a -> a.nombre().contains("CONEAU Global ATENEA (Cuerpo académico)")));
        assertTrue(plantilla.actividadesConeau().stream()
                .anyMatch(a -> a.nombre().contains("CONEAU Global ATENEA (Autoevaluación y Planes de Mejora)")));
    }

    @Test
    @DisplayName("Debe retornar la plantilla estándar para PT_003_SEGUIMIENTO con actividades priorizadas de compromisos")
    void debeRetornarPlantillaSeguimiento() {
        PlantillaPlanTrabajoDto plantilla = service.obtenerPorTipo(TipoPlanTrabajo.PT_003_SEGUIMIENTO);

        assertNotNull(plantilla);
        assertEquals(TipoPlanTrabajo.PT_003_SEGUIMIENTO, plantilla.tipoPlan());
        assertFalse(plantilla.actividadesInfo().isEmpty());
        assertFalse(plantilla.actividadesConeau().isEmpty());
    }

    @Test
    @DisplayName("Debe resolver la plantilla a partir del procesoId buscando su plan de trabajo")
    void debeObtenerPlantillaPorProcesoId() {
        UUID procesoId = UUID.randomUUID();
        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        PlantillaPlanTrabajoDto plantilla = service.obtenerPorProcesoId(procesoId);

        assertNotNull(plantilla);
        assertEquals(TipoPlanTrabajo.PT_001_ACREDITACION, plantilla.tipoPlan());
    }
}
