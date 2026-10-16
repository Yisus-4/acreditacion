package com.undec.acreditacion.infrastructure.estructura;

import com.undec.acreditacion.application.input.ActualizarCarreraCommand;
import com.undec.acreditacion.application.input.ActualizarCarreraUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoCarreraUseCase;
import com.undec.acreditacion.application.input.EliminarCarreraUseCase;
import com.undec.acreditacion.application.input.ListarCarrerasUseCase;
import com.undec.acreditacion.application.input.RegistrarCarreraCommand;
import com.undec.acreditacion.application.input.RegistrarCarreraUseCase;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.EstadoCarrera;
import com.undec.acreditacion.domain.entities.ModalidadCarrera;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.estructura.dto.ActualizarCarreraRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.CambiarEstadoRequest;
import com.undec.acreditacion.infrastructure.estructura.dto.CarreraResponse;
import com.undec.acreditacion.infrastructure.estructura.dto.RegistrarCarreraRequest;
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
@RequestMapping("/api/carreras")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
public class CarreraController {

    private final RegistrarCarreraUseCase registrarCarreraUseCase;
    private final ListarCarrerasUseCase listarCarrerasUseCase;
    private final ActualizarCarreraUseCase actualizarCarreraUseCase;
    private final CambiarEstadoCarreraUseCase cambiarEstadoCarreraUseCase;
    private final EliminarCarreraUseCase eliminarCarreraUseCase;

    public CarreraController(RegistrarCarreraUseCase registrarCarreraUseCase,
                             ListarCarrerasUseCase listarCarrerasUseCase,
                             ActualizarCarreraUseCase actualizarCarreraUseCase,
                             CambiarEstadoCarreraUseCase cambiarEstadoCarreraUseCase,
                             EliminarCarreraUseCase eliminarCarreraUseCase) {
        this.registrarCarreraUseCase = registrarCarreraUseCase;
        this.listarCarrerasUseCase = listarCarrerasUseCase;
        this.actualizarCarreraUseCase = actualizarCarreraUseCase;
        this.cambiarEstadoCarreraUseCase = cambiarEstadoCarreraUseCase;
        this.eliminarCarreraUseCase = eliminarCarreraUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CarreraResponse>> listar(
            @RequestParam(required = false) UUID unidadAcademicaId
    ) {
        List<Carrera> lista = unidadAcademicaId != null
                ? listarCarrerasUseCase.listarPorUnidadAcademica(unidadAcademicaId)
                : listarCarrerasUseCase.listarTodas();

        List<CarreraResponse> response = lista.stream()
                .map(CarreraResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CarreraResponse> registrar(
            @RequestBody RegistrarCarreraRequest request
    ) {
        ModalidadCarrera modalidad = parseModalidad(request.modalidad());
        EstadoCarrera estado = parseEstado(request.estado());

        RegistrarCarreraCommand command = new RegistrarCarreraCommand(
                request.unidadAcademicaId(),
                request.nombre(),
                request.sigla(),
                request.areaDisciplinaria(),
                modalidad,
                estado,
                request.tituloOtorgado(),
                request.duracionAnios(),
                request.descripcion(),
                request.sedesIds()
        );

        Carrera creada = registrarCarreraUseCase.registrar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CarreraResponse.fromDomain(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarreraResponse> actualizar(
            @PathVariable UUID id,
            @RequestBody ActualizarCarreraRequest request
    ) {
        ModalidadCarrera modalidad = parseModalidad(request.modalidad());
        EstadoCarrera estado = parseEstado(request.estado());

        ActualizarCarreraCommand command = new ActualizarCarreraCommand(
                id,
                request.nombre(),
                request.sigla(),
                request.areaDisciplinaria(),
                modalidad,
                estado,
                request.tituloOtorgado(),
                request.duracionAnios(),
                request.descripcion(),
                request.activo(),
                request.sedesIds()
        );

        Carrera actualizada = actualizarCarreraUseCase.actualizar(command);
        return ResponseEntity.ok(CarreraResponse.fromDomain(actualizada));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CarreraResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestBody CambiarEstadoRequest request
    ) {
        Carrera actualizada = cambiarEstadoCarreraUseCase.cambiarEstado(id, request.activo());
        return ResponseEntity.ok(CarreraResponse.fromDomain(actualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarCarreraUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private ModalidadCarrera parseModalidad(String modalidadStr) {
        try {
            return modalidadStr != null
                    ? ModalidadCarrera.valueOf(modalidadStr.trim().toUpperCase())
                    : null;
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Modalidad inválida. Debe ser PRESENCIAL o DISTANCIA");
        }
    }

    private EstadoCarrera parseEstado(String estadoStr) {
        try {
            return estadoStr != null && !estadoStr.isBlank()
                    ? EstadoCarrera.valueOf(estadoStr.trim().toUpperCase())
                    : EstadoCarrera.EN_FUNCIONAMIENTO;
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Estado inválido. Debe ser NUEVA o EN_FUNCIONAMIENTO");
        }
    }
}
