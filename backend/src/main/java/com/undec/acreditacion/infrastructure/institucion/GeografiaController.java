package com.undec.acreditacion.infrastructure.institucion;

import com.undec.acreditacion.application.input.ConsultarGeografiaUseCase;
import com.undec.acreditacion.infrastructure.institucion.dto.DepartamentoResponse;
import com.undec.acreditacion.infrastructure.institucion.dto.LocalidadResponse;
import com.undec.acreditacion.infrastructure.institucion.dto.ProvinciaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/geografia")
@PreAuthorize("isAuthenticated()")
public class GeografiaController {

    private final ConsultarGeografiaUseCase consultarGeografiaUseCase;

    public GeografiaController(ConsultarGeografiaUseCase consultarGeografiaUseCase) {
        this.consultarGeografiaUseCase = consultarGeografiaUseCase;
    }

    @GetMapping("/provincias")
    public ResponseEntity<List<ProvinciaResponse>> listarProvincias() {
        List<ProvinciaResponse> provincias = consultarGeografiaUseCase.listarProvincias().stream()
                .map(ProvinciaResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(provincias);
    }

    @GetMapping("/provincias/{id}/departamentos")
    public ResponseEntity<List<DepartamentoResponse>> listarDepartamentos(@PathVariable Long id) {
        List<DepartamentoResponse> departamentos = consultarGeografiaUseCase.listarDepartamentosPorProvincia(id).stream()
                .map(DepartamentoResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(departamentos);
    }

    @GetMapping("/departamentos/{id}/localidades")
    public ResponseEntity<List<LocalidadResponse>> listarLocalidades(@PathVariable Long id) {
        List<LocalidadResponse> localidades = consultarGeografiaUseCase.listarLocalidadesPorDepartamento(id).stream()
                .map(LocalidadResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(localidades);
    }
}
