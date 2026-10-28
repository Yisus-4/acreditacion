package com.undec.acreditacion.domain.services;

import com.undec.acreditacion.domain.entities.ItemMatrizPermisoAtenea;
import com.undec.acreditacion.domain.entities.TipoPermisoMatrizAtenea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoMatrizPermisosAteneaTest {

    @Test
    @DisplayName("Debe generar catálogo con los 14 procesos y 146 items de matriz con permisos estándar")
    void debeGenerarCatalogoCompleto() {
        UUID registroId = UUID.randomUUID();
        List<ItemMatrizPermisoAtenea> items = CatalogoMatrizPermisosAtenea.crearMatrizPorDefecto(registroId);

        assertNotNull(items);
        assertEquals(146, items.size());

        // Todos tienen registroId asociado
        assertTrue(items.stream().allMatch(i -> registroId.equals(i.getRegistroId())));

        // SGI-AEA tiene permiso en todos (lectura y modificación)
        assertTrue(items.stream().allMatch(ItemMatrizPermisoAtenea::isPermisoSgiAea));

        // CA tiene permiso de lectura en todos los items de tipo LECTURA
        long lecturas = items.stream().filter(i -> i.getTipoPermiso() == TipoPermisoMatrizAtenea.LECTURA).count();
        assertEquals(73, lecturas);
        assertTrue(items.stream()
                .filter(i -> i.getTipoPermiso() == TipoPermisoMatrizAtenea.LECTURA)
                .allMatch(ItemMatrizPermisoAtenea::isPermisoCa));

        // CA tiene permiso de modificación en la sección Carrera
        long caModificacion = items.stream()
                .filter(i -> i.getTipoPermiso() == TipoPermisoMatrizAtenea.LECTURA_MODIFICACION && i.isPermisoCa())
                .count();
        assertTrue(caModificacion > 0);

        // En sección Institución, CA no tiene modificación por defecto
        boolean caModificaInstitucion = items.stream()
                .filter(i -> "Institución".equalsIgnoreCase(i.getSeccion()) && i.getTipoPermiso() == TipoPermisoMatrizAtenea.LECTURA_MODIFICACION)
                .anyMatch(ItemMatrizPermisoAtenea::isPermisoCa);
        assertFalse(caModificaInstitucion);
    }
}
