package com.undec.acreditacion.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.domain.entities.CatalogoMiembroComision;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.entities.RolClaustroCa;
import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import com.undec.acreditacion.domain.ports.CatalogoMiembroComisionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.ports.SolicitudNotificacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GestionarSolicitudNotificacionServiceTest {

    private SolicitudNotificacionRepositoryPort notificacionRepository;
    private CatalogoMiembroComisionRepositoryPort catalogoRepository;
    private RegistroConformacionCaRepositoryPort registroCaRepository;
    private ObjectMapper objectMapper;
    private GestionarSolicitudNotificacionService service;

    private UUID notifId;
    private UUID procesoId;
    private UUID resolutorId;

    @BeforeEach
    void setUp() {
        notificacionRepository = mock(SolicitudNotificacionRepositoryPort.class);
        catalogoRepository = mock(CatalogoMiembroComisionRepositoryPort.class);
        registroCaRepository = mock(RegistroConformacionCaRepositoryPort.class);
        objectMapper = new ObjectMapper();

        service = new GestionarSolicitudNotificacionService(
                notificacionRepository,
                catalogoRepository,
                registroCaRepository,
                objectMapper
        );

        notifId = UUID.randomUUID();
        procesoId = UUID.randomUUID();
        resolutorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Aceptar solicitud de alta registra en catálogo institucional y actualiza miembro en CA")
    void aceptarSolicitudAltaRegistraEnCatalogo() {
        String payloadJson = "{\"apellido\":\"Salerno\",\"nombreCompleto\":\"Julio Luis\",\"documento\":\"8386710\",\"cuitPasaporte\":\"20-08386710-9\",\"correoElectronico\":\"jsalerno@undec.edu.ar\",\"correoGmail\":\"salerno@gmail.com\",\"cargoFuncion\":\"Director de Carrera\",\"esDocente\":true}";

        SolicitudNotificacion notif = new SolicitudNotificacion(
                notifId,
                SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO,
                "Alta Salerno",
                "Solicitud de alta",
                payloadJson,
                procesoId,
                "Ingeniería",
                UUID.randomUUID(),
                "DC",
                "AEA",
                SolicitudNotificacion.ESTADO_PENDIENTE,
                false,
                null,
                null,
                Instant.now()
        );

        when(notificacionRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(catalogoRepository.findByDocumento("8386710")).thenReturn(Optional.empty());
        when(catalogoRepository.findByCuitPasaporte("20-08386710-9")).thenReturn(Optional.empty());

        MiembroComisionAutoevaluacion miembro = new MiembroComisionAutoevaluacion(
                UUID.randomUUID(), null, "Salerno", "Julio Luis", "", "8386710",
                "20-08386710-9", "jsalerno@undec.edu.ar", "salerno@gmail.com",
                "Director", null, RolClaustroCa.COORDINADOR, true, true, true, true, 1
        );
        RegistroConformacionCa ca = new RegistroConformacionCa(UUID.randomUUID(), procesoId);
        List<MiembroComisionAutoevaluacion> miembros = new ArrayList<>();
        miembros.add(miembro);
        ca.guardarBorrador(true, miembros);

        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));
        when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudNotificacionDto result = service.aceptar(notifId, resolutorId);

        assertThat(result.estado()).isEqualTo(SolicitudNotificacion.ESTADO_ACEPTADA);
        assertThat(result.leida()).isTrue();
        assertThat(result.resueltoPor()).isEqualTo(resolutorId);

        verify(catalogoRepository).save(any(CatalogoMiembroComision.class));
        verify(registroCaRepository).save(ca);
        assertThat(miembro.isPendienteAltaSistema()).isFalse();
    }

    @Test
    @DisplayName("Rechazar solicitud actualiza estado a RECHAZADA")
    void rechazarSolicitud() {
        SolicitudNotificacion notif = new SolicitudNotificacion(
                notifId,
                SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO,
                "Alta",
                "Mensaje",
                "{}",
                procesoId,
                "Ingeniería",
                null, null, "AEA",
                SolicitudNotificacion.ESTADO_PENDIENTE,
                false, null, null, Instant.now()
        );

        when(notificacionRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudNotificacionDto result = service.rechazar(notifId, resolutorId);

        assertThat(result.estado()).isEqualTo(SolicitudNotificacion.ESTADO_RECHAZADA);
        assertThat(result.leida()).isTrue();
        verifyNoInteractions(catalogoRepository);
    }
}
