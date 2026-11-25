package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.application.ports.ResultadoSincronizacionDto;
import com.undec.acreditacion.application.ports.SincronizacionAutoevaluacionPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.CopiarDatosCentralesRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/autoevaluacion/procesos")
public class AutoevaluacionSincronizacionController {

    private final SincronizacionAutoevaluacionPort sincronizacionPort;

    public AutoevaluacionSincronizacionController(SincronizacionAutoevaluacionPort sincronizacionPort) {
        this.sincronizacionPort = sincronizacionPort;
    }

    @PostMapping("/{procesoId}/copiar-datos-centrales")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_ESCUELA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<ResultadoSincronizacionDto> copiarDatosCentrales(
            @PathVariable UUID procesoId,
            @RequestBody(required = false) CopiarDatosCentralesRequest request
    ) {
        CopiarDatosCentralesRequest req = request != null ? request : new CopiarDatosCentralesRequest(null, null);
        ResultadoSincronizacionDto resultado = sincronizacionPort.copiarDatosCentrales(
                procesoId,
                req.modo(),
                req.secciones()
        );
        return ResponseEntity.ok(resultado);
    }
}
