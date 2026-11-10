package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import com.undec.acreditacion.infrastructure.persistence.postgres.UserJpaEntity;

import java.util.UUID;

public record DocenteResumenResponse(
        UUID id,
        String username,
        String nombreCompleto,
        String email,
        UUID unidadAcademicaId,
        String unidadAcademicaNombre
) {

    public static DocenteResumenResponse fromUser(UserJpaEntity user, String unidadNombre) {
        String nombre = formatearNombreLegible(user.getUsername(), user.getEmail());
        return new DocenteResumenResponse(
                user.getId(),
                user.getUsername(),
                nombre,
                user.getEmail(),
                user.getUnidadAcademicaId(),
                unidadNombre
        );
    }

    public static String formatearNombreLegible(String username, String email) {
        if (username != null && (username.contains(".") || username.contains("_") || username.contains("-"))) {
            return capitalizarPalabras(username.replace(".", " ").replace("_", " ").replace("-", " "));
        }
        if (email != null && email.contains("@")) {
            String prefix = email.substring(0, email.indexOf("@"));
            if (prefix.contains(".") || prefix.contains("_") || prefix.contains("-")) {
                return capitalizarPalabras(prefix.replace(".", " ").replace("_", " ").replace("-", " "));
            }
        }
        if (username != null && !username.isBlank()) {
            return capitalizarPalabras(username);
        }
        return email != null ? email : "Docente";
    }

    private static String capitalizarPalabras(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String[] partes = texto.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i];
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) {
                    sb.append(p.substring(1).toLowerCase());
                }
                if (i < partes.length - 1) {
                    sb.append(" ");
                }
            }
        }
        return sb.toString();
    }
}
