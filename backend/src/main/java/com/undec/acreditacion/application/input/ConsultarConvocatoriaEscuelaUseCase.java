package com.undec.acreditacion.application.input;

import java.util.List;
import java.util.UUID;

public interface ConsultarConvocatoriaEscuelaUseCase {
    ConvocatoriaEscuelaDetalleDto obtenerPorId(UUID id);
    List<ConvocatoriaEscuelaDetalleDto> listarTodas();
}
