package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class V38MigrationVerificationTest {

    @Test
    @DisplayName("V38 migration exists and contains all required table alterations, new tables and backfill")
    void testV38MigrationContent() throws IOException {
        Path migrationPath = Paths.get("src/main/resources/db/migration/V38__datos_centrales_multiplan_y_espacios_curriculares.sql");
        if (!Files.exists(migrationPath)) {
            // Also try backend relative path if running from root
            migrationPath = Paths.get("backend/src/main/resources/db/migration/V38__datos_centrales_multiplan_y_espacios_curriculares.sql");
        }
        assertTrue(Files.exists(migrationPath), "Flyway V38 migration script must exist");

        String sql = Files.readString(migrationPath);

        // 1. plan_estudio_ft001
        assertTrue(sql.contains("ALTER TABLE plan_estudio_ft001"), "Must alter plan_estudio_ft001");
        assertTrue(sql.contains("nombre_plan VARCHAR(150)"), "Must add nombre_plan");
        assertTrue(sql.contains("estado_vigencia VARCHAR(30)"), "Must add estado_vigencia");
        assertTrue(sql.contains("es_maestro BOOLEAN"), "Must add es_maestro");
        assertTrue(sql.contains("chk_plan_estudio_estado_vigencia"), "Must include chk_plan_estudio_estado_vigencia");
        assertTrue(sql.contains("fk_plan_estudio_origen_maestro"), "Must include fk_plan_estudio_origen_maestro");

        // 2. actividad_curricular_plan & programa_analitico_asignatura
        assertTrue(sql.contains("ALTER TABLE actividad_curricular_plan"), "Must alter actividad_curricular_plan");
        assertTrue(sql.contains("ALTER TABLE programa_analitico_asignatura"), "Must alter programa_analitico_asignatura");

        // 3. inmueble & espacio_fisico
        assertTrue(sql.contains("ALTER TABLE inmueble"), "Must alter inmueble");
        assertTrue(sql.contains("ALTER TABLE espacio_fisico"), "Must alter espacio_fisico");

        // 4. autoevaluacion_carrera_ambito
        assertTrue(sql.contains("ALTER TABLE autoevaluacion_carrera_ambito"), "Must alter autoevaluacion_carrera_ambito");

        // 5. convocatoria_carrera_plan
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS convocatoria_carrera_plan"), "Must create convocatoria_carrera_plan");
        assertTrue(sql.contains("uq_convocatoria_carrera_plan"), "Must have uq_convocatoria_carrera_plan");

        // 6. cuerpo_academico_maestro & cuerpo_academico_actividad_maestro
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cuerpo_academico_maestro"), "Must create cuerpo_academico_maestro");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cuerpo_academico_actividad_maestro"), "Must create cuerpo_academico_actividad_maestro");

        // 7. proceso_cuerpo_academico & proceso_cuerpo_academico_actividad
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS proceso_cuerpo_academico"), "Must create proceso_cuerpo_academico");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS proceso_cuerpo_academico_actividad"), "Must create proceso_cuerpo_academico_actividad");

        // 8. espacio_fisico_actividad_curricular
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS espacio_fisico_actividad_curricular"), "Must create espacio_fisico_actividad_curricular");
        assertTrue(sql.contains("uq_espacio_plan_actividad"), "Must have uq_espacio_plan_actividad");

        // 9. Historical Backfill
        assertTrue(sql.contains("INSERT INTO convocatoria_carrera_plan"), "Must backfill existing convocatorias into convocatoria_carrera_plan");
    }
}
