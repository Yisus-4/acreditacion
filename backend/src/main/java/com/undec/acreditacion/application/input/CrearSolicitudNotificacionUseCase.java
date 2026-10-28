package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;

import java.util.UUID;

public interface CrearSolicitudNotificacionUseCase {

    SolicitudNotificacionDto registrarSolicitudAltaMiembro(
            UUID procesoId,
            UUID solicitanteId,
            String solicitanteNombre,
            String carreraNombre,
            DatosPayloadAltaMiembro payload
    );
}
