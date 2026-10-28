package com.undec.acreditacion.domain.services;

import com.undec.acreditacion.domain.entities.ItemMatrizPermisoAtenea;
import com.undec.acreditacion.domain.entities.TipoPermisoMatrizAtenea;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CatalogoMatrizPermisosAtenea {

    private CatalogoMatrizPermisosAtenea() {}

    private record ItemDef(String proceso, String item, String seccion) {}

    private static final List<ItemDef> CATALOGO = List.of(
            // 1. Infraestructura y equipamiento para enseñanza y aprendizaje
            new ItemDef("1. Infraestructura y equipamiento para enseñanza y aprendizaje", "Todos", "Todos"),
            new ItemDef("1. Infraestructura y equipamiento para enseñanza y aprendizaje", "Ficha de inmuebles", "Institución"),
            new ItemDef("1. Infraestructura y equipamiento para enseñanza y aprendizaje", "Punto 6. Infraestructura (Subpuntos 6.1, 6.2, 6.3)", "Unidad Académica"),
            new ItemDef("1. Infraestructura y equipamiento para enseñanza y aprendizaje", "Anexos de la UA", "Unidad Académica"),

            // 2. Vinculación interinstitucional
            new ItemDef("2. Vinculación interinstitucional", "Todos", "Todos"),
            new ItemDef("2. Vinculación interinstitucional", "Fichas Convenios", "Unidad Académica"),
            new ItemDef("2. Vinculación interinstitucional", "Anexos de la UA", "Unidad Académica"),

            // 3. Ámbitos de formación teórico-práctica
            new ItemDef("3. Ámbitos de formación teórico-práctica", "Todos", "Todos"),
            new ItemDef("3. Ámbitos de formación teórico-práctica", "Ficha de ámbitos de práctica", "Institución"),
            new ItemDef("3. Ámbitos de formación teórico-práctica", "Ficha de unidad demostrativa/experimental", "Institución"),
            new ItemDef("3. Ámbitos de formación teórico-práctica", "Ficha de Centros de atención médica", "Institución"),
            new ItemDef("3. Ámbitos de formación teórico-práctica", "Anexos de la UA", "Institución"),

            // 4. Organización, coordinación y gestión académica
            new ItemDef("4. Organización, coordinación y gestión académica", "Todos", "Todos"),
            new ItemDef("4. Organización, coordinación y gestión académica", "Punto 3. Estructura y organización de la Unidad Académica (Subpuntos 3.1, 3.2, 3.3, 3.5.1, 3.5.2, 3.5.3, 3.6, 3.7 y 3.8)", "Unidad Académica"),
            new ItemDef("4. Organización, coordinación y gestión académica", "Punto 4. Políticas institucionales (Subpunto 4.1.1, 4.1.3, 4.1.4, 4.4, 4.5)", "Unidad Académica"),
            new ItemDef("4. Organización, coordinación y gestión académica", "Anexos de la UA", "Unidad Académica"),

            // 5. Estructura curricular, organización y gestión de la carrera
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Todos", "Todos"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Punto 1. Organización (todos los subpuntos)", "Carrera"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Punto 2. Plan de estudios (subpuntos 2.1 y 2.2)", "Carrera"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Punto 3. Cuerpo académico", "Carrera"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Punto 4. Alumnos y graduados (subpuntos 4.1 y 4.6)", "Carrera"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Ficha de plan de estudio vigente", "Carrera"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Anexos de Carrera", "Carrera"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "3. Estructura y organización de la Unidad Académica (subpunto 3.4)", "Unidad Académica"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Ficha de actividades curriculares (todos los sub-puntos)", "Unidad Académica"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Ficha de ámbitos de práctica de cada actividad curricular", "Unidad Académica"),
            new ItemDef("5. Estructura curricular, organización y gestión de la carrera", "Anexos de la UA", "Unidad Académica"),

            // 6. Estudiantes y graduados
            new ItemDef("6. Estudiantes y graduados", "Todos", "Todos"),
            new ItemDef("6. Estudiantes y graduados", "4. Alumnos y graduados (Subpunto 4.2.2, 4.3, 4.4, 4.5 y 4.6)", "Carrera"),
            new ItemDef("6. Estudiantes y graduados", "4. Alumnos y graduados (Subpunto 4.2.4, 4.2.5, 4.2.6, 4.6.1 y 4.6.2)", "Carrera"),
            new ItemDef("6. Estudiantes y graduados", "5. Alumnos y graduados (Subpunto 5.1, 5.2, 5.3, 5.4, 5.5, 5.6 y 5.7)", "Unidad Académica"),
            new ItemDef("6. Estudiantes y graduados", "5. Alumnos y graduados (subpunto 5.9)", "Unidad Académica"),
            new ItemDef("6. Estudiantes y graduados", "Ficha de actividades curriculares (Subpunto 7)", "Unidad Académica"),
            new ItemDef("6. Estudiantes y graduados", "Ficha de convenios (actualizar resultados cuando corresponda)", "Unidad Académica"),
            new ItemDef("6. Estudiantes y graduados", "Anexos de la UA", "Unidad Académica"),

            // 7. Conformación y evaluación del cuerpo académico
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "Todos", "Todos"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "2. Cuerpo académico (Sub-punto 2.3)", "Institución"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "4. Políticas institucionales", "Todos"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "Ficha de vinculaciones al cuerpo académico (una por cada docente)", "Unidad Académica"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "Fichas de actividades curriculares (subpunto 6)", "Unidad Académica"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "Fichas de investigación", "Todos"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "Fichas de vinculación con el medio", "Todos"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "Anexos de la UA", "Unidad Académica"),
            new ItemDef("7. Conformación y evaluación del cuerpo académico", "3. Cuerpo académico (Sub-punto 3.1.2)", "Carrera"),

            // 8. Capacitación, actualización y/o perfeccionamiento del personal
            new ItemDef("8. Capacitación, actualización y/o perfeccionamiento del personal", "Todos", "Todos"),
            new ItemDef("8. Capacitación, actualización y/o perfeccionamiento del personal", "4. Políticas institucionales (Subpunto 4.1.1, 4.1.2 y 4.1.4)", "Unidad Académica"),
            new ItemDef("8. Capacitación, actualización y/o perfeccionamiento del personal", "Anexos de la UA", "Unidad Académica"),

            // 9. Actividades de investigación del cuerpo académico
            new ItemDef("9. Actividades de investigación del cuerpo académico", "Todos", "Todos"),
            new ItemDef("9. Actividades de investigación del cuerpo académico", "4. Políticas institucionales (Subpunto 4.2.1 y 4.2.2)", "Unidad Académica"),
            new ItemDef("9. Actividades de investigación del cuerpo académico", "Fichas de investigación (una por cada proyecto temáticamente vinculado a la carrera)", "Unidad Académica"),
            new ItemDef("9. Actividades de investigación del cuerpo académico", "Ficha de vinculaciones al cuerpo académico", "Unidad Académica"),
            new ItemDef("9. Actividades de investigación del cuerpo académico", "Anexos de la UA", "Unidad Académica"),

            // 10. Actividades de vinculación con el medio/extensión
            new ItemDef("10. Actividades de vinculación con el medio/extensión", "Todos", "Todos"),
            new ItemDef("10. Actividades de vinculación con el medio/extensión", "4. Políticas institucionales (Subpunto 4.3.1 y 4.3.2)", "Unidad Académica"),
            new ItemDef("10. Actividades de vinculación con el medio/extensión", "Ficha de vinculación con el medio/extensión (una por cada proyecto temáticamente vinculado a la carrera)", "Unidad Académica"),
            new ItemDef("10. Actividades de vinculación con el medio/extensión", "Ficha de vinculaciones al cuerpo académico", "Unidad Académica"),
            new ItemDef("10. Actividades de vinculación con el medio/extensión", "Anexos de la UA", "Unidad Académica"),

            // 11. Diagnóstico de la carrera y elaboración de Planes de Mejora
            new ItemDef("11. Diagnóstico de la carrera y elaboración de Planes de Mejora", "Todos", "Todos"),
            new ItemDef("11. Diagnóstico de la carrera y elaboración de Planes de Mejora", "Punto 6. Autoevaluación", "Carrera"),
            new ItemDef("11. Diagnóstico de la carrera y elaboración de Planes de Mejora", "Anexos de la UA", "Carrera"),
            new ItemDef("11. Diagnóstico de la carrera y elaboración de Planes de Mejora", "Anexos de Carrera", "Carrera"),

            // 12. Respuesta a la vista
            new ItemDef("12. Respuesta a la vista", "Todos", "Todos"),
            new ItemDef("12. Respuesta a la vista", "Punto 7. Respuesta a la vista", "Carrera"),
            new ItemDef("12. Respuesta a la vista", "Anexos de la UA", "Carrera"),
            new ItemDef("12. Respuesta a la vista", "Anexos de Carrera", "Carrera"),

            // 13. Recurso en reconsideración
            new ItemDef("13. Recurso en reconsideración", "Todos", "Todos"),
            new ItemDef("13. Recurso en reconsideración", "Punto 7. Recurso en reconsideración", "Carrera"),
            new ItemDef("13. Recurso en reconsideración", "Anexos de la UA", "Carrera"),
            new ItemDef("13. Recurso en reconsideración", "Anexos de Carrera", "Carrera"),

            // 14. Seguimiento de compromisos
            new ItemDef("14. Seguimiento de compromisos", "Todos", "Todos"),
            new ItemDef("14. Seguimiento de compromisos", "Punto 7. Seguimiento de compromisos", "Carrera"),
            new ItemDef("14. Seguimiento de compromisos", "Anexos de la UA", "Carrera"),
            new ItemDef("14. Seguimiento de compromisos", "Anexos de Carrera", "Carrera")
    );

    public static List<ItemMatrizPermisoAtenea> crearMatrizPorDefecto(UUID registroId) {
        List<ItemMatrizPermisoAtenea> items = new ArrayList<>();
        int orden = 1;

        for (ItemDef def : CATALOGO) {
            boolean esCarrera = "Carrera".equalsIgnoreCase(def.seccion());

            // 1. Lectura
            ItemMatrizPermisoAtenea lectura = ItemMatrizPermisoAtenea.crear(
                    def.proceso(),
                    def.item(),
                    def.seccion(),
                    TipoPermisoMatrizAtenea.LECTURA,
                    true, // SGI-AEA Lectura
                    true, // CA Lectura
                    orden++
            );
            if (registroId != null) {
                lectura.setRegistroId(registroId);
            }
            items.add(lectura);

            // 2. Lectura y modificación
            ItemMatrizPermisoAtenea modificacion = ItemMatrizPermisoAtenea.crear(
                    def.proceso(),
                    def.item(),
                    def.seccion(),
                    TipoPermisoMatrizAtenea.LECTURA_MODIFICACION,
                    true,      // SGI-AEA Lectura y Modificación
                    esCarrera, // CA Lectura y Modificación sólo en sección Carrera / ámbitos de carrera
                    orden++
            );
            if (registroId != null) {
                modificacion.setRegistroId(registroId);
            }
            items.add(modificacion);
        }

        return items;
    }
}
