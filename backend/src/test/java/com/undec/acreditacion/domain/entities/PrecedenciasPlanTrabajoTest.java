package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests para Dependencias y Precedencias Lógicas (PRG-002)")
class PrecedenciasPlanTrabajoTest {

    @Test
    @DisplayName("Debe resolver precedencias según catálogo oficial PRG-002")
    void debeResolverPrecedenciasCatalogo() {
        // Ola 1 (independientes)
        assertTrue(PrecedenciasPlanTrabajoCatalogo.resolver("4.1").isEmpty());
        assertTrue(PrecedenciasPlanTrabajoCatalogo.resolver("8.4").isEmpty());
        assertTrue(PrecedenciasPlanTrabajoCatalogo.resolver("6.1").isEmpty());
        assertTrue(PrecedenciasPlanTrabajoCatalogo.resolver("10.1").isEmpty());

        // Ola 2
        assertEquals(List.of("8.4"), PrecedenciasPlanTrabajoCatalogo.resolver("4.4"));
        assertEquals(List.of("6.1"), PrecedenciasPlanTrabajoCatalogo.resolver("6.2"));
        assertEquals(List.of("8.3"), PrecedenciasPlanTrabajoCatalogo.resolver("9.1"));
        assertEquals(List.of("10.1"), PrecedenciasPlanTrabajoCatalogo.resolver("10.6"));

        // Ola 3 (Cuello de botella)
        assertEquals(List.of("4.4", "9.1"), PrecedenciasPlanTrabajoCatalogo.resolver("8.5"));
    }

    @Test
    @DisplayName("Actividad dependiente debe estar bloqueada mientras sus predecesoras no estén completadas")
    void actividadDependienteDebeEstarBloqueadaSiPredecesoraNoEstaCompletada() {
        PlanTrabajo plan = PlanTrabajo.inicializar(UUID.randomUUID(), "PT-TEST-001", TipoPlanTrabajo.PT_001_ACREDITACION);

        // Actividad 8.4 (Programas analíticos)
        ActividadPlanTrabajo act84 = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(), 10, "8.4", "Gestionar programas analíticos",
                HojaPlanTrabajo.INFO, LocalDate.now(), LocalDate.now().plusDays(10),
                "Director", PrioridadActividad.ALTA
        );

        // Actividad 4.4 (Biblioteca RG-014 - depende de 8.4)
        ActividadPlanTrabajo act44 = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(), 11, "4.4", "Gestionar recursos bibliográficos",
                HojaPlanTrabajo.INFO, LocalDate.now(), LocalDate.now().plusDays(10),
                "Biblioteca", PrioridadActividad.ALTA, List.of("8.4")
        );

        plan.agregarActividad(act84);
        plan.agregarActividad(act44);

        ActividadPlanTrabajo enPlan84 = plan.getActividades().stream().filter(a -> "8.4".equals(a.getCodigoActividad())).findFirst().orElseThrow();
        ActividadPlanTrabajo enPlan44 = plan.getActividades().stream().filter(a -> "4.4".equals(a.getCodigoActividad())).findFirst().orElseThrow();

        // Al inicio, 8.4 está PENDIENTE -> 4.4 debe estar bloqueada
        assertTrue(plan.isActividadBloqueada(enPlan44));
        assertEquals(List.of("8.4"), plan.getPredecesorasPendientes(enPlan44));

        // Intentar completar 4.4 debe lanzar DomainValidationException
        assertThrows(DomainValidationException.class, () -> plan.validarPredecesorasCompletadas(enPlan44));

        // Cuando 8.4 se completa:
        enPlan84.completar(UUID.randomUUID(), "Programas analíticos subidos y validados");

        // 4.4 ahora debe estar desbloqueada
        assertFalse(plan.isActividadBloqueada(enPlan44));
        assertTrue(plan.getPredecesorasPendientes(enPlan44).isEmpty());
        assertDoesNotThrow(() -> plan.validarPredecesorasCompletadas(enPlan44));
    }

    @Test
    @DisplayName("Actividad con múltiples dependencias (8.5) debe requerir que todas estén completadas")
    void actividadConMultiplesDependenciasDebeRequerirTodas() {
        PlanTrabajo plan = PlanTrabajo.inicializar(UUID.randomUUID(), "PT-TEST-002", TipoPlanTrabajo.PT_001_ACREDITACION);

        ActividadPlanTrabajo act44 = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(), 10, "4.4", "Biblioteca RG-014",
                HojaPlanTrabajo.INFO, LocalDate.now(), LocalDate.now().plusDays(10),
                "Biblioteca", PrioridadActividad.ALTA
        );

        ActividadPlanTrabajo act91 = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(), 11, "9.1", "Cuadros alumnos SGA",
                HojaPlanTrabajo.INFO, LocalDate.now(), LocalDate.now().plusDays(10),
                "SGA", PrioridadActividad.MEDIA
        );

        ActividadPlanTrabajo act85 = ActividadPlanTrabajo.crearActividadCronograma(
                plan.getId(), 12, "8.5", "CONEAU ATENEA Asignaturas FT-003",
                HojaPlanTrabajo.CONEAU, LocalDate.now(), LocalDate.now().plusDays(10),
                "Docentes / AEA", PrioridadActividad.ALTA, List.of("4.4", "9.1")
        );

        plan.agregarActividad(act44);
        plan.agregarActividad(act91);
        plan.agregarActividad(act85);

        ActividadPlanTrabajo enPlan44 = plan.getActividades().stream().filter(a -> "4.4".equals(a.getCodigoActividad())).findFirst().orElseThrow();
        ActividadPlanTrabajo enPlan91 = plan.getActividades().stream().filter(a -> "9.1".equals(a.getCodigoActividad())).findFirst().orElseThrow();
        ActividadPlanTrabajo enPlan85 = plan.getActividades().stream().filter(a -> "8.5".equals(a.getCodigoActividad())).findFirst().orElseThrow();

        // Ambas pendientes -> bloqueada
        assertTrue(plan.isActividadBloqueada(enPlan85));
        assertEquals(2, plan.getPredecesorasPendientes(enPlan85).size());

        // Completamos solo 4.4 -> sigue bloqueada porque falta 9.1
        enPlan44.completar(UUID.randomUUID(), "Libros verificados");
        assertTrue(plan.isActividadBloqueada(enPlan85));
        assertEquals(List.of("9.1"), plan.getPredecesorasPendientes(enPlan85));

        // Completamos 9.1 -> se desbloquea completamente
        enPlan91.completar(UUID.randomUUID(), "Cuadros completados");
        assertFalse(plan.isActividadBloqueada(enPlan85));
        assertTrue(plan.getPredecesorasPendientes(enPlan85).isEmpty());
    }
}
