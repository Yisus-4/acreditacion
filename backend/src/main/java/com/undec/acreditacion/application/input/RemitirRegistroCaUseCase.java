package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroCaDto;

import java.util.List;
import java.util.UUID;

public interface RemitirRegistroCaUseCase {
    RegistroCaDto remitir(
            UUID procesoId,
            UUID usuarioDcId,
            boolean coincideCurricular,
            List<MiembroCaInputDto> miembros,
            byte[] resolucionBytes,
            String resolucionNombreOriginal
    );
}
