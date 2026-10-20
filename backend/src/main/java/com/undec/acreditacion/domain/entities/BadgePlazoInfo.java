package com.undec.acreditacion.domain.entities;

public record BadgePlazoInfo(
    String estado,
    int diasRestantes,
    String etiqueta,
    String cssClass
) {
    public static BadgePlazoInfo enTermino(int diasRestantes) {
        String etiqueta = "En término (" + diasRestantes + " días hábiles restantes)";
        return new BadgePlazoInfo("EN_TERMINO", diasRestantes, etiqueta, "badge-success");
    }

    public static BadgePlazoInfo fueraDeTermino(int diasVencidos) {
        String etiqueta = "Fuera de término (" + Math.abs(diasVencidos) + " días de demora)";
        return new BadgePlazoInfo("FUERA_DE_TERMINO", diasVencidos, etiqueta, "badge-danger");
    }

    public static BadgePlazoInfo entregadoEnTermino() {
        return new BadgePlazoInfo("ENTREGADO_EN_TERMINO", 0, "Entregado en término", "badge-success");
    }

    public static BadgePlazoInfo entregadoFueraDeTermino() {
        return new BadgePlazoInfo("ENTREGADO_FUERA_DE_TERMINO", 0, "Entregado fuera de término", "badge-danger");
    }

    public static BadgePlazoInfo enSubsanacion(int diasRestantes) {
        return new BadgePlazoInfo("EN_SUBSANACION", diasRestantes, "En subsanación", "badge-warning");
    }

    public static BadgePlazoInfo completado() {
        return new BadgePlazoInfo("COMPLETADO", 0, "Completado", "badge-success");
    }
}
