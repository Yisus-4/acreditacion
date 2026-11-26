package com.undec.acreditacion.infrastructure.designacion;

import com.undec.acreditacion.domain.entities.TipoEventoSolicitudDesignacion;
import com.undec.acreditacion.infrastructure.designacion.persistence.SolicitudDesignacionCambioJpaEntity;
import com.undec.acreditacion.infrastructure.designacion.persistence.SolicitudDocenteDesignacionJpaEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class V50MigrationVerificationTest {

    @Test
    void enumYEntidadDeAuditoriaSoportanEventosYOrigenDeClonacion() {
        assertEquals(12, TipoEventoSolicitudDesignacion.values().length);
        assertNotNull(TipoEventoSolicitudDesignacion.valueOf("CLONACION_PERIODO_ANTERIOR"));
        assertNotNull(TipoEventoSolicitudDesignacion.valueOf("EXCLUSION_DESIGNACION"));
        assertNotNull(TipoEventoSolicitudDesignacion.valueOf("REACTIVACION_DESIGNACION"));
        assertNotNull(TipoEventoSolicitudDesignacion.valueOf("PRESENTACION_SGA"));
        assertNotNull(TipoEventoSolicitudDesignacion.valueOf("APROBACION_SGA"));
        assertNotNull(TipoEventoSolicitudDesignacion.valueOf("OBSERVACION_SGA"));

        UUID origenId = UUID.randomUUID();
        SolicitudDocenteDesignacionJpaEntity desig = new SolicitudDocenteDesignacionJpaEntity();
        desig.setDesignacionOrigenId(origenId);
        assertEquals(origenId, desig.getDesignacionOrigenId());

        SolicitudDesignacionCambioJpaEntity cambio = new SolicitudDesignacionCambioJpaEntity();
        cambio.setId(UUID.randomUUID());
        cambio.setTipoEvento(TipoEventoSolicitudDesignacion.MODIFICACION_DESIGNACION);
        cambio.setMotivo("Cambio de dedicación aprobado por consejo departamental");
        assertEquals(TipoEventoSolicitudDesignacion.MODIFICACION_DESIGNACION, cambio.getTipoEvento());
        assertEquals("Cambio de dedicación aprobado por consejo departamental", cambio.getMotivo());
    }

    @Test
    void migracionV50DefineTablaAuditoriaYColumnaDesignacionOrigen() throws IOException {
        var stream = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V50__auditoria_cambios_y_flujo_sga_designacion.sql");
        assertNotNull(stream, "Debe existir V50__auditoria_cambios_y_flujo_sga_designacion.sql");
        String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(sql.contains("designacion_origen_id"), "Debe agregar designacion_origen_id a solicitud_docente_designacion");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS solicitud_designacion_cambio"), "Debe crear tabla solicitud_designacion_cambio");
        assertTrue(sql.contains("tipo_evento"), "Debe incluir columna tipo_evento");
        assertTrue(sql.contains("valor_anterior"), "Debe incluir columna valor_anterior");
        assertTrue(sql.contains("valor_nuevo"), "Debe incluir columna valor_nuevo");
        assertTrue(sql.contains("motivo"), "Debe incluir columna motivo");
        assertTrue(sql.contains("idx_sol_desig_cambio_solicitud"), "Debe indexar por solicitud_id y fecha_cambio");
    }
}
