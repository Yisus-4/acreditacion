package com.undec.acreditacion.domain.entities;

import java.util.Set;

/**
 * Catálogo único y canónico de códigos de rol institucionales del sistema.
 */
public final class CodigosRol {

    public static final String AEA = "AEA";
    public static final String DIRECTOR_ESCUELA = "DIRECTOR_ESCUELA";
    public static final String DIRECTOR_CARRERA = "DIRECTOR_CARRERA";
    public static final String DIRECTOR_DEPARTAMENTO = "DIRECTOR_DEPARTAMENTO";
    public static final String SECRETARIA_ACADEMICA = "SECRETARIA_ACADEMICA";
    public static final String CONSULTA_INSTITUCIONAL = "CONSULTA_INSTITUCIONAL";
    public static final String COMISION_AUTOEVALUACION = "COMISION_AUTOEVALUACION";
    public static final String DOCENTE = "DOCENTE";
    public static final String INFRAESTRUCTURA = "INFRAESTRUCTURA";
    public static final String BEDELIA = "BEDELIA";
    public static final String HIGIENE_SEGURIDAD = "HIGIENE_SEGURIDAD";
    public static final String BIBLIOTECA = "BIBLIOTECA";
    public static final String PROVEEDOR_INFORMACION = "PROVEEDOR_INFORMACION";

    public static final Set<String> TODOS = Set.of(
            AEA,
            DIRECTOR_ESCUELA,
            DIRECTOR_CARRERA,
            DIRECTOR_DEPARTAMENTO,
            SECRETARIA_ACADEMICA,
            CONSULTA_INSTITUCIONAL,
            COMISION_AUTOEVALUACION,
            DOCENTE,
            INFRAESTRUCTURA,
            BEDELIA,
            HIGIENE_SEGURIDAD,
            BIBLIOTECA,
            PROVEEDOR_INFORMACION
    );

    private CodigosRol() {
    }
}
