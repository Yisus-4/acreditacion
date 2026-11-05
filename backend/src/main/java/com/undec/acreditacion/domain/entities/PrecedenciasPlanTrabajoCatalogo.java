package com.undec.acreditacion.domain.entities;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Catálogo de precedencias y dependencias lógicas oficiales del procedimiento PRG-002
 * (Evaluación de Grado) y Plan de Trabajo (PT-001).
 *
 * Define qué actividades son requeridas (insumos o registros maestros previos)
 * antes de poder iniciar o completar una actividad dependiente.
 */
public final class PrecedenciasPlanTrabajoCatalogo {

    private PrecedenciasPlanTrabajoCatalogo() {
    }

    private static final Map<String, List<String>> DEPENDENCIAS_PRG_002 = Map.ofEntries(
            // Ola 1 -> Ola 2 (Comité de Autoevaluación)
            Map.entry("3.2", List.of("3.1")),
            Map.entry("3.3", List.of("3.2")),
            Map.entry("3.4", List.of("3.3")),

            // Recursos Bibliográficos RG-014 depende de Programas Analíticos (8.4)
            Map.entry("4.4", List.of("8.4")),

            // Convenios de la carrera
            Map.entry("5.2", List.of("5.1")),
            Map.entry("5.3", List.of("5.2")),

            // Ámbitos de formación interna (6.2) depende de Ámbitos maestro RG-006 (6.1)
            Map.entry("6.2", List.of("6.1")),

            // Cuadros alumnos/graduados (9.1) depende de generación de fichas curriculares / planes (8.3)
            Map.entry("9.1", List.of("8.3")),

            // Vinculaciones docentes CVar (10.6) depende de Conformación del cuerpo académico RG-010 (10.1)
            Map.entry("10.6", List.of("10.1")),

            // Autoevaluación y plan de mejoras (Hoja INFO)
            Map.entry("13.2", List.of("13.1")),
            Map.entry("13.3", List.of("13.2")),
            Map.entry("13.4", List.of("13.3")),
            Map.entry("13.5", List.of("13.4")),

            // Hoja CONEAU Global
            Map.entry("2.2", List.of("2.1")),
            Map.entry("2.3", List.of("2.2")),
            Map.entry("2.4", List.of("2.3")),
            Map.entry("2.5", List.of("2.4")),

            // CONEAU 4.3 (Infraestructura) depende de 4.1 completada
            Map.entry("4.3", List.of("4.1")),

            // Ola 3: CONEAU 8.5 (Asignaturas FT-003) cuello de botella: depende de Biblioteca 4.4 y Cuadros SGA 9.1
            Map.entry("8.5", List.of("4.4", "9.1")),

            // CONEAU 9.6 (Alumnos y graduados) depende de 9.1 completada
            Map.entry("9.6", List.of("9.1")),

            // CONEAU 10.4 (Cuerpo académico) depende de 10.1 y 10.6
            Map.entry("10.4", List.of("10.1", "10.6")),

            // CONEAU 11.4 (Investigación) depende de 11.1
            Map.entry("11.4", List.of("11.1")),

            // CONEAU 12.4 (Vinculación con el medio) depende de 12.1
            Map.entry("12.4", List.of("12.1"))
    );

    /**
     * Resuelve la lista inmutable de códigos de actividades predecesoras por defecto
     * para el código de actividad especificado según el procedimiento PRG-002.
     *
     * @param codigoActividad Código identificador (ej: "4.4", "8.5", "6.2", etc.)
     * @return Lista de códigos predecesores requeridos o lista vacía si es independiente.
     */
    public static List<String> resolver(String codigoActividad) {
        if (codigoActividad == null) {
            return Collections.emptyList();
        }
        return DEPENDENCIAS_PRG_002.getOrDefault(codigoActividad.trim(), Collections.emptyList());
    }
}
