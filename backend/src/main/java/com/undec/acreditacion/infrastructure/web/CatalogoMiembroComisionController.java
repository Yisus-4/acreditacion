package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.ConsultarCatalogoMiembrosCaUseCase;
import com.undec.acreditacion.application.output.CatalogoMiembroComisionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/comision-autoevaluacion/catalogo")
public class CatalogoMiembroComisionController {

    private final ConsultarCatalogoMiembrosCaUseCase consultarCatalogoMiembrosCaUseCase;

    public CatalogoMiembroComisionController(ConsultarCatalogoMiembrosCaUseCase consultarCatalogoMiembrosCaUseCase) {
        this.consultarCatalogoMiembrosCaUseCase = consultarCatalogoMiembrosCaUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<List<CatalogoMiembroComisionDto>> consultar(
            @RequestParam(value = "busqueda", required = false) String busqueda
    ) {
        List<CatalogoMiembroComisionDto> list = consultarCatalogoMiembrosCaUseCase.buscar(busqueda);
        return ResponseEntity.ok(list);
    }
}
