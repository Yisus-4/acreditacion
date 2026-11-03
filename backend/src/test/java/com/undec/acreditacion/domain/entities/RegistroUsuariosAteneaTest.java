package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistroUsuariosAteneaTest {

    @Test
    @DisplayName("Debe inicializar registro con estado EN_DEFINICION y plazo de 2 días hábiles")
    void debeInicializarRegistroCorrectamente() {
        UUID procesoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 10, 26); // Lunes

        RegistroUsuariosAtenea registro = RegistroUsuariosAtenea.iniciar(procesoId, usuarioId, inicio);

        assertNotNull(registro.getId());
        assertEquals(procesoId, registro.getProcesoId());
        assertEquals(EstadoRegistroUsuariosAtenea.EN_DEFINICION, registro.getEstado());
        assertEquals(LocalDate.of(2026, 10, 28), registro.getFechaLimite()); // Miércoles (+2 días hábiles)
        assertTrue(registro.getUsuarios().isEmpty());
        assertNull(registro.getAprobadoEn());
        assertNull(registro.getRutaDocumento());
    }

    @Test
    @DisplayName("Debe importar miembros de CA hacia el grupo COMISION_AUTOEVALUACION")
    void debeImportarMiembrosCaHaciaGrupoComision() {
        UUID procesoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        RegistroUsuariosAtenea registro = RegistroUsuariosAtenea.iniciar(procesoId, usuarioId, LocalDate.now());

        MiembroComisionAutoevaluacion m1 = new MiembroComisionAutoevaluacion(
                UUID.randomUUID(), registro.getId(), "Gomez", "Juan", "Carlos",
                "20-30111222-3", "20-30111222-3", "jgomez@undec.edu.ar", "jgomez@gmail.com",
                "Director de Carrera", "3825112233", RolClaustroCa.DOCENTE,
                false, 1
        );

        registro.importarMiembrosCa(List.of(m1));

        assertEquals(1, registro.getUsuarios().size());
        UsuarioPermisoAtenea usuario = registro.getUsuarios().get(0);
        assertEquals(GrupoUsuarioAtenea.COMISION_AUTOEVALUACION, usuario.getGrupoUsuario());
        assertEquals("Juan Carlos", usuario.getNombre());
        assertEquals("Gomez", usuario.getApellido());
        assertEquals("20-30111222-3", usuario.getDniCuil());
        assertEquals("jgomez@undec.edu.ar", usuario.getCorreo());
        assertTrue(usuario.isRequiereCuentaNuevaConeauGlobal()); // false en ca -> requiere cuenta nueva
        assertFalse(usuario.isCuentaConeauGlobalCreada());
    }

    @Test
    @DisplayName("Debe permitir agregar y actualizar usuarios de distintos grupos")
    void debePermitirAgregarYActualizarUsuarios() {
        UUID procesoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        RegistroUsuariosAtenea registro = RegistroUsuariosAtenea.iniciar(procesoId, usuarioId, LocalDate.now());

        UsuarioPermisoAtenea uAea = UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.AEA, "Maria", "Perez", "27-28333444-9",
                "mperez@undec.edu.ar", "Área de Evaluación y Acreditación (AEA)",
                "Responsable Técnico AEA", TipoAccesoAtenea.ADMINISTRACION,
                "Administración Global ATENEA", false, true, true
        );

        UsuarioPermisoAtenea uSgi = UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.SGI, "Roberto", "Diaz", "20-19222333-5",
                "rdiaz@undec.edu.ar", "Secretaría de Gestión Institucional",
                "Secretario SGI", TipoAccesoAtenea.VISUALIZACION,
                "Datos Institucionales", false, true, true
        );

        registro.agregarUsuario(uAea);
        registro.agregarUsuario(uSgi);

        assertEquals(2, registro.getUsuarios().size());
    }

    @Test
    @DisplayName("Debe fallar al finalizar si no contiene usuarios o ya fue finalizado")
    void debeFallarAlFinalizarInvalido() {
        UUID procesoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        RegistroUsuariosAtenea registro = RegistroUsuariosAtenea.iniciar(procesoId, usuarioId, LocalDate.now());

        assertThrows(DomainValidationException.class, () -> registro.finalizar(usuarioId, "path/doc.pdf"));

        UsuarioPermisoAtenea u = UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.AEA, "Ana", "Lopez", "27-30111222-4",
                "alopez@undec.edu.ar", "AEA", "Coordinadora", TipoAccesoAtenea.ADMINISTRACION,
                "Formulario", false, true, true
        );
        registro.agregarUsuario(u);

        registro.finalizar(usuarioId, "path/doc.pdf");
        assertEquals(EstadoRegistroUsuariosAtenea.FINALIZADO, registro.getEstado());
        assertNotNull(registro.getAprobadoEn());
        assertEquals("path/doc.pdf", registro.getRutaDocumento());

        assertThrows(DomainValidationException.class, () -> registro.finalizar(usuarioId, "otro/path.pdf"));
    }

    @Test
    @DisplayName("Debe inicializar y actualizar matriz de permisos en borrador")
    void debeGestionarMatrizPermisosCorrectamente() {
        UUID procesoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        RegistroUsuariosAtenea registro = RegistroUsuariosAtenea.iniciar(procesoId, usuarioId, LocalDate.now());

        assertNotNull(registro.getMatrizPermisos());
        assertEquals(146, registro.getMatrizPermisos().size());

        ItemMatrizPermisoAtenea primerItem = registro.getMatrizPermisos().get(0);
        primerItem.actualizarPermisos(true, false);

        registro.actualizarMatrizPermisos(List.of(primerItem));
        assertEquals(1, registro.getMatrizPermisos().size());
        assertFalse(registro.getMatrizPermisos().get(0).isPermisoCa());

        // Al finalizar no debe permitir modificar matriz
        UsuarioPermisoAtenea u = UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.AEA, "Ana", "Lopez", "27-30111222-4",
                "alopez@undec.edu.ar", "AEA", "Coordinadora", TipoAccesoAtenea.ADMINISTRACION,
                "Formulario", false, true, true
        );
        registro.agregarUsuario(u);
        registro.finalizar(usuarioId, "path/doc.pdf");

        assertThrows(DomainValidationException.class, () -> registro.actualizarMatrizPermisos(List.of()));
    }
}
