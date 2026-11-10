package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.infrastructure.autoevaluacion.dto.DocenteResumenResponse;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ListarDocentesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/autoevaluacion/docentes")
public class AutoevaluacionDocenteController {

    private final ListarDocentesService listarDocentesService;

    public AutoevaluacionDocenteController(ListarDocentesService listarDocentesService) {
        this.listarDocentesService = listarDocentesService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<List<DocenteResumenResponse>> listarDocentes(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "q", required = false) String q
    ) {
        String effectiveQuery = query != null ? query : q;
        List<DocenteResumenResponse> docentes = listarDocentesService.listarDocentes(effectiveQuery);
        return ResponseEntity.ok(docentes);
    }
}
