package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.RegistroCaDto;

import java.util.List;
import java.util.UUID;

public interface GuardarRegistroCaBorradorUseCase {
    RegistroCaDto guardarBorrador(UUID procesoId, boolean coincideCurricular, List<MiembroCaInputDto> miembros);

    default RegistroCaDto guardarBorrador(UUID procesoId, UUID usuarioId, boolean coincideCurricular, List<MiembroCaInputDto> miembros) {
        return guardarBorrador(procesoId, coincideCurricular, miembros);
    }
}
