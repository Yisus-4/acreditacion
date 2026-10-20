package com.undec.acreditacion.application.input;

import java.util.UUID;

public interface ExportarHojaInfoExcelUseCase {
    byte[] ejecutar(UUID planTrabajoId);
}
