package com.undec.acreditacion.infrastructure.estructura;

import com.undec.acreditacion.application.input.ActualizarUnidadAcademicaCommand;
import com.undec.acreditacion.application.input.ActualizarUnidadAcademicaUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoUnidadAcademicaUseCase;
import com.undec.acreditacion.application.input.EliminarUnidadAcademicaUseCase;
import com.undec.acreditacion.application.input.ListarUnidadesAcademicasUseCase;
import com.undec.acreditacion.application.input.RegistrarUnidadAcademicaCommand;
import com.undec.acreditacion.application.input.RegistrarUnidadAcademicaUseCase;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.infrastructure.estructura.dto.ActualizarUnidadAcademicaRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.CambiarEstadoRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.RegistrarUnidadAcademicaRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.UnidadAcademicaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/unidades-academicas")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
public class UnidadAcademicaController {

    private final RegistrarUnidadAcademicaUseCase registrarUnidadAcademicaUseCase;
    private final ListarUnidadesAcademicasUseCase listarUnidadesAcademicasUseCase;
    private final ActualizarUnidadAcademicaUseCase actualizarUnidadAcademicaUseCase;
    private final CambiarEstadoUnidadAcademicaUseCase cambiarEstadoUnidadAcademicaUseCase;
    private final EliminarUnidadAcademicaUseCase eliminarUnidadAcademicaUseCase;

    public UnidadAcademicaController(RegistrarUnidadAcademicaUseCase registrarUnidadAcademicaUseCase,
                                     ListarUnidadesAcademicasUseCase listarUnidadesAcademicasUseCase,
                                     ActualizarUnidadAcademicaUseCase actualizarUnidadAcademicaUseCase,
                                     CambiarEstadoUnidadAcademicaUseCase cambiarEstadoUnidadAcademicaUseCase,
                                     EliminarUnidadAcademicaUseCase eliminarUnidadAcademicaUseCase) {
        this.registrarUnidadAcademicaUseCase = registrarUnidadAcademicaUseCase;
        this.listarUnidadesAcademicasUseCase = listarUnidadesAcademicasUseCase;
        this.actualizarUnidadAcademicaUseCase = actualizarUnidadAcademicaUseCase;
        this.cambiarEstadoUnidadAcademicaUseCase = cambiarEstadoUnidadAcademicaUseCase;
        this.eliminarUnidadAcademicaUseCase = eliminarUnidadAcademicaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<UnidadAcademicaResponse>> listar(
            @RequestParam(required = false) UUID institucionId
    ) {
        List<UnidadAcademica> lista = institucionId != null
                ? listarUnidadesAcademicasUseCase.listarPorInstitucion(institucionId)
                : listarUnidadesAcademicasUseCase.listarTodas();

        List<UnidadAcademicaResponse> response = lista.stream()
                .map(UnidadAcademicaResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UnidadAcademicaResponse> registrar(
            @RequestBody RegistrarUnidadAcademicaRequest request
    ) {
        RegistrarUnidadAcademicaCommand command = new RegistrarUnidadAcademicaCommand(
                request.institucionId(),
                request.nombre(),
                request.sigla(),
                request.descripcion()
        );

        UnidadAcademica creada = registrarUnidadAcademicaUseCase.registrar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UnidadAcademicaResponse.fromDomain(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadAcademicaResponse> actualizar(
            @PathVariable UUID id,
            @RequestBody ActualizarUnidadAcademicaRequest request
    ) {
        ActualizarUnidadAcademicaCommand command = new ActualizarUnidadAcademicaCommand(
                id,
                request.nombre(),
                request.sigla(),
                request.descripcion()
        );

        UnidadAcademica actualizada = actualizarUnidadAcademicaUseCase.actualizar(command);
        return ResponseEntity.ok(UnidadAcademicaResponse.fromDomain(actualizada));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UnidadAcademicaResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestBody CambiarEstadoRequest request
    ) {
        UnidadAcademica actualizada = cambiarEstadoUnidadAcademicaUseCase.cambiarEstado(id, request.activo());
        return ResponseEntity.ok(UnidadAcademicaResponse.fromDomain(actualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarUnidadAcademicaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
