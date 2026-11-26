package com.undec.acreditacion.infrastructure.designacion;

import com.undec.acreditacion.domain.entities.EstadoItemDesignacion;
import com.undec.acreditacion.domain.entities.EstadoSolicitudDesignacion;
import com.undec.acreditacion.domain.entities.ModalidadParticipacionDocente;
import com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class V49MigrationVerificationTest {

    @Test
    @DisplayName("Enums de Fase 2 definen todos los valores y reglas de edición esperados")
    void enumsFase2DefinidosCorrectamente() {
        assertEquals(2, TipoSolicitudDesignacion.values().length);
        assertEquals(4, EstadoSolicitudDesignacion.values().length);
        assertTrue(EstadoSolicitudDesignacion.BORRADOR.esEditablePorDepartamento());
        assertTrue(EstadoSolicitudDesignacion.OBSERVADA.esEditablePorDepartamento());
        assertFalse(EstadoSolicitudDesignacion.PRESENTADA_SGA.esEditablePorDepartamento());
        assertFalse(EstadoSolicitudDesignacion.APROBADA.esEditablePorDepartamento());

        assertEquals(4, EstadoItemDesignacion.values().length);
        assertEquals(2, ModalidadParticipacionDocente.values().length);
        assertNotNull(ModalidadParticipacionDocente.DESARROLLO_CLASES);
        assertNotNull(ModalidadParticipacionDocente.EVALUACION_FINALES);
    }

    @Test
    @DisplayName("V49 crea tablas solicitud_designacion, solicitud_docente_designacion y solicitud_asignacion_curricular con restricciones")
    void v49ContieneTablasYRestriccionesEsperadas() throws IOException {
        Path v49 = Path.of("src/main/resources/db/migration/V49__solicitudes_designacion_docente_y_distribucion_horaria.sql");
        assertTrue(Files.exists(v49), "Debe existir la migración V49");
        String sql = Files.readString(v49);

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS solicitud_designacion"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS solicitud_docente_designacion"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS solicitud_asignacion_curricular"));
        assertTrue(sql.contains("horas_clases"));
        assertTrue(sql.contains("horas_investigacion"));
        assertTrue(sql.contains("horas_extension"));
        assertTrue(sql.contains("horas_gestion"));
        assertTrue(sql.contains("horas_direccion_tesis"));
        assertTrue(sql.contains("horas_otras"));
        assertTrue(sql.contains("DESARROLLO_CLASES"));
        assertTrue(sql.contains("EVALUACION_FINALES"));
        assertTrue(sql.contains("uq_solicitud_asignacion_curricular"));
    }
}
