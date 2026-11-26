package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.domain.entities.CodigosRol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtConfigAndRoleUniquenessTest {

    private static final Set<String> FORBIDDEN_DUPLICATE_ROLES = Set.of(
            "CAREER_DIRECTOR",
            "SCHOOL_DIRECTOR",
            "SELF_EVALUATION_COMMITTEE_MEMBER",
            "AC_COORDINATOR",
            "COORDINADOR_AEA",
            "INFORMATION_PROVIDER",
            "ADMINISTRATOR",
            "DEVELOPER"
    );

    @Test
    @DisplayName("application.yml configura expiración por defecto de JWT en 1 hora (3600000 ms)")
    void jwtExpirationDefaultIsOneHour() throws IOException {
        Path appYml = Path.of("src/main/resources/application.yml");
        String content = Files.readString(appYml);
        assertTrue(
                content.contains("${JWT_EXPIRATION_MS:3600000}"),
                "application.yml debe fijar JWT_EXPIRATION_MS por defecto en 3600000 ms (1 hora)"
        );
    }

    @Test
    @DisplayName("Ningún archivo Java o TypeScript de producción referencia códigos de rol duplicados u obsoletos")
    void noDuplicateRoleCodesInMainJavaAndFrontend() throws IOException {
        List<String> violations = new ArrayList<>();
        scanDirectory(Path.of("src/main/java"), ".java", violations);
        Path frontendApp = Path.of("../frontend/src/app");
        if (Files.isDirectory(frontendApp)) {
            scanDirectory(frontendApp, ".ts", violations);
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Se detectaron códigos de rol duplicados/obsoletos en producción:\n" + String.join("\n", violations)
        );
    }

    @Test
    @DisplayName("CodigosRol incluye SECRETARIA_ACADEMICA y CONSULTA_INSTITUCIONAL y V48 migra esquema Fase 1")
    void codigosRolYV48IncluyenRolesYEstructuraFase1() throws IOException {
        assertTrue(CodigosRol.TODOS.contains(CodigosRol.SECRETARIA_ACADEMICA));
        assertTrue(CodigosRol.TODOS.contains(CodigosRol.CONSULTA_INSTITUCIONAL));

        Path v48 = Path.of("src/main/resources/db/migration/V48__departamentos_academicos_catalogos_docentes_y_estados_asignatura.sql");
        assertTrue(Files.exists(v48), "Debe existir la migración V48");
        String sql = Files.readString(v48);
        assertTrue(sql.contains("SECRETARIA_ACADEMICA"));
        assertTrue(sql.contains("CONSULTA_INSTITUCIONAL"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS departamento_academico"));
        assertTrue(sql.contains("SIN_ASIGNAR"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cargo_docente"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS dedicacion_docente"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS tipo_designacion_docente"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS periodo_academico"));
        assertTrue(sql.contains("SOLO_EXAMENES_FINALES"));
        assertTrue(sql.contains("horas_semanales"));
    }

    private static void scanDirectory(Path root, String extension, List<String> violations) throws IOException {
        Pattern quotedToken = Pattern.compile("['\"]([A-Z_]{3,40})['\"]");
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(extension) && !p.toString().endsWith(".spec.ts")).forEach(file -> {
                try {
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        Matcher m = quotedToken.matcher(lines.get(i));
                        while (m.find()) {
                            String token = m.group(1);
                            if (FORBIDDEN_DUPLICATE_ROLES.contains(token)) {
                                violations.add(root.relativize(file) + ":" + (i + 1) + " -> " + token);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
