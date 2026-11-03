package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroConformacionCaTest {

    @Test
    @DisplayName("Debe crearse inicialmente en estado PENDIENTE_SOLICITUD con 0 miembros")
    void creacionInicial() {
        UUID procesoId = UUID.randomUUID();
        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);

        assertThat(ca.getId()).isNotNull();
        assertThat(ca.getProcesoId()).isEqualTo(procesoId);
        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.PENDIENTE_SOLICITUD);
        assertThat(ca.isCoincideComisionCurricular()).isFalse();
        assertThat(ca.getMiembros()).isEmpty();
    }

    @Test
    @DisplayName("AEA solicita conformación de CA y fija plazo perentorio de 15 días")
    void solicitarConformacionPorAea() {
        UUID procesoId = UUID.randomUUID();
        UUID aeaUserId = UUID.randomUUID();
        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);

        ca.solicitar(aeaUserId);

        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.SOLICITADA_A_DC);
        assertThat(ca.getSolicitadoPor()).isEqualTo(aeaUserId);
        assertThat(ca.getSolicitadoEn()).isNotNull();
        assertThat(ca.getFechaLimiteDc()).isEqualTo(LocalDate.now().plusDays(15));
    }

    @Test
    @DisplayName("Guardar borrador pasa estado a EN_CARGA_BORRADOR si estaba SOLICITADA_A_DC")
    void guardarBorrador() {
        RegistroConformacionCa ca = new RegistroConformacionCa(null, UUID.randomUUID());
        ca.solicitar(UUID.randomUUID());

        MiembroComisionAutoevaluacion m1 = new MiembroComisionAutoevaluacion(
                null, null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                "3825123456", RolClaustroCa.COORDINADOR, 1
        );

        ca.guardarBorrador(true, List.of(m1));

        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.EN_CARGA_BORRADOR);
        assertThat(ca.isCoincideComisionCurricular()).isTrue();
        assertThat(ca.getMiembros()).hasSize(1);
        assertThat(ca.getMiembros().get(0).getRegistroCaId()).isEqualTo(ca.getId());
    }

    @Test
    @DisplayName("Remitir a AEA falla si no hay coordinador")
    void remitirSinCoordinadorFalla() {
        RegistroConformacionCa ca = new RegistroConformacionCa(null, UUID.randomUUID());
        ca.solicitar(UUID.randomUUID());

        MiembroComisionAutoevaluacion mDocente = new MiembroComisionAutoevaluacion(
                null, null, "Gomez", "Ana", null, "87654321", "ana@undec.edu.ar",
                null, RolClaustroCa.DOCENTE, 1
        );

        assertThatThrownBy(() -> ca.remitir(
                UUID.randomUUID(), false, List.of(mDocente),
                "path/resolucion.pdf", "resolucion.pdf", 1024L
        )).isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("COORDINADOR");
    }

    @Test
    @DisplayName("Remitir a AEA permite enviar con coordinador docente")
    void remitirDocenteEsValido() {
        RegistroConformacionCa ca = new RegistroConformacionCa(null, UUID.randomUUID());
        ca.solicitar(UUID.randomUUID());

        MiembroComisionAutoevaluacion mCoordDocente = new MiembroComisionAutoevaluacion(
                null, null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                null, RolClaustroCa.COORDINADOR, 1
        );

        ca.remitir(
                UUID.randomUUID(), false, List.of(mCoordDocente),
                "path/resolucion.pdf", "resolucion.pdf", 1024L
        );

        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.REMITIDA_A_AEA);
    }

    @Test
    @DisplayName("Remitir a AEA permite enviar sin resolución adjunta (queda pendiente la resolución)")
    void remitirSinResolucionEsValido() {
        RegistroConformacionCa ca = new RegistroConformacionCa(null, UUID.randomUUID());
        ca.solicitar(UUID.randomUUID());

        MiembroComisionAutoevaluacion mCoord = new MiembroComisionAutoevaluacion(
                null, null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                null, RolClaustroCa.COORDINADOR, 1
        );

        UUID dcUserId = UUID.randomUUID();
        ca.remitir(dcUserId, false, List.of(mCoord), null, null, null);

        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.REMITIDA_A_AEA);
        assertThat(ca.getResolucionStoragePath()).isNull();
    }

    @Test
    @DisplayName("Remisión exitosa a AEA")
    void remisionExitosa() {
        RegistroConformacionCa ca = new RegistroConformacionCa(null, UUID.randomUUID());
        ca.solicitar(UUID.randomUUID());

        MiembroComisionAutoevaluacion mCoord = new MiembroComisionAutoevaluacion(
                null, null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                null, RolClaustroCa.COORDINADOR, 1
        );

        UUID dcUserId = UUID.randomUUID();
        ca.remitir(dcUserId, true, List.of(mCoord), "path/res.pdf", "res.pdf", 2048L);

        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.REMITIDA_A_AEA);
        assertThat(ca.getRemitidoPor()).isEqualTo(dcUserId);
        assertThat(ca.getRemitidoEn()).isNotNull();
        assertThat(ca.getResolucionStoragePath()).isEqualTo("path/res.pdf");
    }

    @Test
    @DisplayName("Aprobación por AEA actualiza estado y almacena ruta del PDF formal generado")
    void aprobacionExitosa() {
        RegistroConformacionCa ca = new RegistroConformacionCa(null, UUID.randomUUID());
        ca.solicitar(UUID.randomUUID());

        MiembroComisionAutoevaluacion mCoord = new MiembroComisionAutoevaluacion(
                null, null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                null, RolClaustroCa.COORDINADOR, 1
        );
        ca.remitir(UUID.randomUUID(), false, List.of(mCoord), "res.pdf", "res.pdf", 100L);

        UUID aeaUserId = UUID.randomUUID();
        String generatedPdfPath = "01-Convocatoria y formalizacion CONEAU/RG-002_Registro de conformación de CA_Ingenieria_2026.pdf";
        ca.aprobar(aeaUserId, generatedPdfPath);

        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.APROBADA);
        assertThat(ca.getAprobadoPor()).isEqualTo(aeaUserId);
        assertThat(ca.getAprobadoEn()).isNotNull();
        assertThat(ca.getRegistroGeneradoStoragePath()).isEqualTo(generatedPdfPath);
    }

    @Test
    @DisplayName("Observar por AEA cambia estado a OBSERVADA y registra motivo")
    void observarPorAea() {
        RegistroConformacionCa ca = new RegistroConformacionCa(null, UUID.randomUUID());
        ca.solicitar(UUID.randomUUID());

        MiembroComisionAutoevaluacion mCoord = new MiembroComisionAutoevaluacion(
                null, null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                null, RolClaustroCa.COORDINADOR, 1
        );
        ca.remitir(UUID.randomUUID(), false, List.of(mCoord), "res.pdf", "res.pdf", 100L);

        UUID aeaUserId = UUID.randomUUID();
        ca.observar(aeaUserId, "Falta incluir el representante del claustro graduado");

        assertThat(ca.getEstado()).isEqualTo(EstadoRegistroCa.OBSERVADA);
        assertThat(ca.getObservacionesRevision()).isEqualTo("Falta incluir el representante del claustro graduado");
    }
}
