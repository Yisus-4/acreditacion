package com.undec.acreditacion.domain.entities;

import java.util.*;
import java.util.stream.Collectors;

public final class MatrizNormativa {

    private static final Set<TipoDocumentoNormativa> OBLIGATORIOS_BASE = Set.of(
        TipoDocumentoNormativa.CREACION_CARRERA,
        TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS,
        TipoDocumentoNormativa.RESOLUCION_DIRECTOR_CARRERA,
        TipoDocumentoNormativa.RESOLUCION_COMISION_CURRICULAR,
        TipoDocumentoNormativa.PLAN_DESARROLLO_CARRERA
    );

    private static final Set<TipoDocumentoNormativa> OBLIGATORIOS_EN_FUNCIONAMIENTO = Set.of(
        TipoDocumentoNormativa.RM_VALIDEZ_NACIONAL,
        TipoDocumentoNormativa.APROBACION_PROGRAMAS_ANALITICOS
    );

    private static final Set<TipoDocumentoNormativa> OBLIGATORIOS_HISTORIAL = Set.of();

    private MatrizNormativa() {
        // Utility class
    }

    public static Set<TipoDocumentoNormativa> obtenerRequeridos(boolean esNueva, boolean conHistorialConeau) {
        Set<TipoDocumentoNormativa> requeridos = new HashSet<>(OBLIGATORIOS_BASE);
        if (!esNueva) {
            requeridos.addAll(OBLIGATORIOS_EN_FUNCIONAMIENTO);
        }
        if (conHistorialConeau) {
            requeridos.addAll(OBLIGATORIOS_HISTORIAL);
        }
        return Collections.unmodifiableSet(requeridos);
    }

    public static List<TipoDocumentoNormativa> obtenerFaltantes(Collection<TipoDocumentoNormativa> tiposCargados, boolean esNueva, boolean conHistorialConeau) {
        Set<TipoDocumentoNormativa> cargados = tiposCargados != null ? new HashSet<>(tiposCargados) : Collections.emptySet();
        Set<TipoDocumentoNormativa> requeridos = obtenerRequeridos(esNueva, conHistorialConeau);

        return requeridos.stream()
            .filter(req -> {
                if (cargados.contains(req)) return false;
                if (req == TipoDocumentoNormativa.ORDENANZA_PLAN_ESTUDIOS && cargados.contains(TipoDocumentoNormativa.PLAN_ESTUDIOS_VIGENTE)) {
                    return false;
                }
                return true;
            })
            .sorted(Comparator.comparing(Enum::name))
            .collect(Collectors.toList());
    }

    public static boolean esCompleta(Collection<TipoDocumentoNormativa> tiposCargados, boolean esNueva, boolean conHistorialConeau) {
        return obtenerFaltantes(tiposCargados, esNueva, conHistorialConeau).isEmpty();
    }
}
