package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.CumplimientoPlazo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EmailNotificationPort {
    void enviarSolicitudNormativa(UUID procesoId, String destinatarioEmail, String destinatarioNombre, String carreraNombre, String codigoConvocatoria, LocalDate fechaLimite, String observaciones);
    void enviarRemisionRevision(UUID procesoId, String destinatarioEmail, String destinatarioNombre, String carreraNombre, String codigoConvocatoria, CumplimientoPlazo cumplimiento, int cantidadDocumentos);
    void enviarObservaciones(UUID procesoId, String destinatarioEmail, String destinatarioNombre, String carreraNombre, String codigoConvocatoria, String observacionesRevision, List<String> documentosObservados);
    void enviarAprobacion(UUID procesoId, String destinatarioEmail, String destinatarioNombre, String carreraNombre, String codigoConvocatoria, String observaciones);
}
