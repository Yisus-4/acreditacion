package com.undec.acreditacion.infrastructure.estructura;

import com.undec.acreditacion.application.input.ActualizarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.ActualizarSedeLocalizacionUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoSedeLocalizacionUseCase;
import com.undec.acreditacion.application.input.EliminarSedeLocalizacionUseCase;
import com.undec.acreditacion.application.input.ListarSedesLocalizacionesUseCase;
import com.undec.acreditacion.application.input.RegistrarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.RegistrarSedeLocalizacionUseCase;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.infrastructure.estructura.dto.ActualizarSedeLocalizacionRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.CambiarEstadoRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.RegistrarSedeLocalizacionRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.SedeLocalizacionResponse;
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
@RequestMapping("/api/sedes-localizaciones")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
public class SedeLocalizacionController {

    private final RegistrarSedeLocalizacionUseCase registrarSedeLocalizacionUseCase;
    private final ListarSedesLocalizacionesUseCase listarSedesLocalizacionesUseCase;
    private final ActualizarSedeLocalizacionUseCase actualizarSedeLocalizacionUseCase;
    private final CambiarEstadoSedeLocalizacionUseCase cambiarEstadoSedeLocalizacionUseCase;
    private final EliminarSedeLocalizacionUseCase eliminarSedeLocalizacionUseCase;

    public SedeLocalizacionController(RegistrarSedeLocalizacionUseCase registrarSedeLocalizacionUseCase,
                                      ListarSedesLocalizacionesUseCase listarSedesLocalizacionesUseCase,
                                      ActualizarSedeLocalizacionUseCase actualizarSedeLocalizacionUseCase,
                                      CambiarEstadoSedeLocalizacionUseCase cambiarEstadoSedeLocalizacionUseCase,
                                      EliminarSedeLocalizacionUseCase eliminarSedeLocalizacionUseCase) {
        this.registrarSedeLocalizacionUseCase = registrarSedeLocalizacionUseCase;
        this.listarSedesLocalizacionesUseCase = listarSedesLocalizacionesUseCase;
        this.actualizarSedeLocalizacionUseCase = actualizarSedeLocalizacionUseCase;
        this.cambiarEstadoSedeLocalizacionUseCase = cambiarEstadoSedeLocalizacionUseCase;
        this.eliminarSedeLocalizacionUseCase = eliminarSedeLocalizacionUseCase;
    }

    @GetMapping
    public ResponseEntity<List<SedeLocalizacionResponse>> listar(
            @RequestParam(required = false) UUID institucionId
    ) {
        List<SedeLocalizacion> lista = institucionId != null
                ? listarSedesLocalizacionesUseCase.listarPorInstitucion(institucionId)
                : listarSedesLocalizacionesUseCase.listarTodas();

        List<SedeLocalizacionResponse> response = lista.stream()
                .map(SedeLocalizacionResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SedeLocalizacionResponse> registrar(
            @RequestBody RegistrarSedeLocalizacionRequest request
    ) {
        RegistrarSedeLocalizacionCommand command = new RegistrarSedeLocalizacionCommand(
                request.institucionId(),
                request.nombre(),
                request.calle(),
                request.numero(),
                request.piso(),
                request.codigoPostal(),
                request.provinciaId(),
                request.departamentoId(),
                request.localidadId(),
                request.esPropia() != null ? request.esPropia() : true
        );

        SedeLocalizacion creada = registrarSedeLocalizacionUseCase.registrar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(SedeLocalizacionResponse.fromDomain(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SedeLocalizacionResponse> actualizar(
            @PathVariable UUID id,
            @RequestBody ActualizarSedeLocalizacionRequest request
    ) {
        ActualizarSedeLocalizacionCommand command = new ActualizarSedeLocalizacionCommand(
                id,
                request.nombre(),
                request.calle(),
                request.numero(),
                request.piso(),
                request.codigoPostal(),
                request.provinciaId(),
                request.departamentoId(),
                request.localidadId(),
                request.esPropia() != null ? request.esPropia() : true
        );

        SedeLocalizacion actualizada = actualizarSedeLocalizacionUseCase.actualizar(command);
        return ResponseEntity.ok(SedeLocalizacionResponse.fromDomain(actualizada));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<SedeLocalizacionResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestBody CambiarEstadoRequest request
    ) {
        SedeLocalizacion actualizada = cambiarEstadoSedeLocalizacionUseCase.cambiarEstado(id, request.activo());
        return ResponseEntity.ok(SedeLocalizacionResponse.fromDomain(actualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarSedeLocalizacionUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
