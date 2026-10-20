package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.*;

import java.util.List;
import java.util.Optional;

public interface ExcelReportGeneratorPort {
    byte[] generarHojaInfoExcel(
        PlanTrabajo planTrabajo,
        ProcesoAcreditacion proceso,
        Carrera carrera,
        RegistroConvocatoria convocatoria,
        List<ActividadPlanTrabajo> actividades,
        Optional<SolicitudNormativaCarrera> solicitudNormativa
    );
}
