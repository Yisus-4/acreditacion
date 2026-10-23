package com.undec.acreditacion.infrastructure.institucion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.ActualizarInstitucionUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoInstitucionUseCase;
import com.undec.acreditacion.application.input.ConsultarInstitucionUseCase;
import com.undec.acreditacion.application.input.EliminarInstitucionUseCase;
import com.undec.acreditacion.application.input.ListarInstitucionesUseCase;
import com.undec.acreditacion.application.input.RegistrarInstitucionCommand;
import com.undec.acreditacion.application.input.RegistrarInstitucionUseCase;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.infrastructure.institucion.dto.CambiarEstadoRequest;
import com.undec.acreditacion.infrastructure.institucion.dto.InstitucionResponse;
import com.undec.acreditacion.infrastructure.institucion.dto.RegistrarInstitucionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/instituciones")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
public class InstitucionController {

    private final RegistrarInstitucionUseCase registrarInstitucionUseCase;
    private final ListarInstitucionesUseCase listarInstitucionesUseCase;
    private final ConsultarInstitucionUseCase consultarInstitucionUseCase;
    private final ActualizarInstitucionUseCase actualizarInstitucionUseCase;
    private final CambiarEstadoInstitucionUseCase cambiarEstadoInstitucionUseCase;
    private final EliminarInstitucionUseCase eliminarInstitucionUseCase;
    private final DocumentStoragePort documentStoragePort;
    private final ObjectMapper objectMapper;

    public InstitucionController(RegistrarInstitucionUseCase registrarInstitucionUseCase,
                                 ListarInstitucionesUseCase listarInstitucionesUseCase,
                                 ConsultarInstitucionUseCase consultarInstitucionUseCase,
                                 ActualizarInstitucionUseCase actualizarInstitucionUseCase,
                                 CambiarEstadoInstitucionUseCase cambiarEstadoInstitucionUseCase,
                                 EliminarInstitucionUseCase eliminarInstitucionUseCase,
                                 DocumentStoragePort documentStoragePort,
                                 ObjectMapper objectMapper) {
        this.registrarInstitucionUseCase = registrarInstitucionUseCase;
        this.listarInstitucionesUseCase = listarInstitucionesUseCase;
        this.consultarInstitucionUseCase = consultarInstitucionUseCase;
        this.actualizarInstitucionUseCase = actualizarInstitucionUseCase;
        this.cambiarEstadoInstitucionUseCase = cambiarEstadoInstitucionUseCase;
        this.eliminarInstitucionUseCase = eliminarInstitucionUseCase;
        this.documentStoragePort = documentStoragePort;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InstitucionResponse> registrarInstitucionMultipart(
            @RequestPart(value = "datos", required = false) String datosJson,
            @RequestPart(value = "logotipo", required = false) MultipartFile logotipo
    ) throws IOException {
        if (datosJson == null || datosJson.isBlank()) {
            throw new DomainValidationException("Los datos de la institución son obligatorios");
        }

        RegistrarInstitucionRequest request;
        try {
            request = objectMapper.readValue(datosJson, RegistrarInstitucionRequest.class);
        } catch (Exception e) {
            throw new DomainValidationException("Formato inválido en los datos de la institución: " + e.getMessage());
        }

        byte[] logoBytes = logotipo != null ? logotipo.getBytes() : null;
        String logoFilename = logotipo != null ? logotipo.getOriginalFilename() : null;
        String logoContentType = logotipo != null ? logotipo.getContentType() : null;

        RegistrarInstitucionCommand command = toCommand(request, logoBytes, logoFilename, logoContentType);
        Institucion creada = registrarInstitucionUseCase.registrar(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InstitucionResponse.fromDomain(creada, documentStoragePort));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstitucionResponse> registrarInstitucionJson(
            @RequestBody RegistrarInstitucionRequest request
    ) {
        if (request == null) {
            throw new DomainValidationException("El cuerpo de la solicitud no puede estar vacío");
        }

        RegistrarInstitucionCommand command = toCommand(request, null, null, null);
        Institucion creada = registrarInstitucionUseCase.registrar(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InstitucionResponse.fromDomain(creada, documentStoragePort));
    }

    @GetMapping
    public ResponseEntity<List<InstitucionResponse>> listarInstituciones() {
        List<InstitucionResponse> lista = listarInstitucionesUseCase.listar().stream()
                .map(i -> InstitucionResponse.fromDomain(i, documentStoragePort))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstitucionResponse> obtenerPorId(@PathVariable UUID id) {
        Institucion institucion = consultarInstitucionUseCase.consultarPorId(id);
        return ResponseEntity.ok(InstitucionResponse.fromDomain(institucion, documentStoragePort));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InstitucionResponse> actualizarInstitucionMultipart(
            @PathVariable UUID id,
            @RequestPart(value = "datos", required = false) String datosJson,
            @RequestPart(value = "logotipo", required = false) MultipartFile logotipo
    ) throws IOException {
        if (datosJson == null || datosJson.isBlank()) {
            throw new DomainValidationException("Los datos de la institución son obligatorios");
        }

        RegistrarInstitucionRequest request;
        try {
            request = objectMapper.readValue(datosJson, RegistrarInstitucionRequest.class);
        } catch (Exception e) {
            throw new DomainValidationException("Formato inválido en los datos de la institución: " + e.getMessage());
        }

        byte[] logoBytes = logotipo != null ? logotipo.getBytes() : null;
        String logoFilename = logotipo != null ? logotipo.getOriginalFilename() : null;
        String logoContentType = logotipo != null ? logotipo.getContentType() : null;

        RegistrarInstitucionCommand command = toCommand(request, logoBytes, logoFilename, logoContentType);
        Institucion actualizada = actualizarInstitucionUseCase.actualizar(id, command);

        return ResponseEntity.ok(InstitucionResponse.fromDomain(actualizada, documentStoragePort));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstitucionResponse> actualizarInstitucionJson(
            @PathVariable UUID id,
            @RequestBody RegistrarInstitucionRequest request
    ) {
        if (request == null) {
            throw new DomainValidationException("El cuerpo de la solicitud no puede estar vacío");
        }

        RegistrarInstitucionCommand command = toCommand(request, null, null, null);
        Institucion actualizada = actualizarInstitucionUseCase.actualizar(id, command);

        return ResponseEntity.ok(InstitucionResponse.fromDomain(actualizada, documentStoragePort));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<InstitucionResponse> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam(value = "activo", required = false) Boolean activoParam,
            @RequestBody(required = false) CambiarEstadoRequest body
    ) {
        boolean activo = activoParam != null ? activoParam : (body != null && body.activo() != null ? body.activo() : false);
        Institucion actualizada = cambiarEstadoInstitucionUseCase.cambiarEstado(id, activo);
        return ResponseEntity.ok(InstitucionResponse.fromDomain(actualizada, documentStoragePort));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarInstitucion(@PathVariable UUID id) {
        eliminarInstitucionUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private RegistrarInstitucionCommand toCommand(RegistrarInstitucionRequest request,
                                                  byte[] logoBytes,
                                                  String logoFilename,
                                                  String logoContentType) {
        return new RegistrarInstitucionCommand(
                request.nombre(),
                request.sigla(),
                logoBytes,
                logoFilename,
                logoContentType,
                request.maximaAutoridad() != null ? request.maximaAutoridad().apellido() : null,
                request.maximaAutoridad() != null ? request.maximaAutoridad().primerNombre() : null,
                request.maximaAutoridad() != null ? request.maximaAutoridad().segundoNombre() : null,
                request.maximaAutoridad() != null ? request.maximaAutoridad().telefono() : null,
                request.maximaAutoridad() != null ? request.maximaAutoridad().email() : null,
                request.administradorInstitucional() != null ? request.administradorInstitucional().apellido() : null,
                request.administradorInstitucional() != null ? request.administradorInstitucional().primerNombre() : null,
                request.administradorInstitucional() != null ? request.administradorInstitucional().segundoNombre() : null,
                request.administradorInstitucional() != null ? request.administradorInstitucional().telefono() : null,
                request.administradorInstitucional() != null ? request.administradorInstitucional().email() : null,
                request.administradorInstitucional() != null ? request.administradorInstitucional().cargo() : null,
                request.administradorInstitucional() != null ? request.administradorInstitucional().ambito() : null,
                request.sedeCentral() != null ? request.sedeCentral().calle() : null,
                request.sedeCentral() != null ? request.sedeCentral().numero() : null,
                request.sedeCentral() != null ? request.sedeCentral().piso() : null,
                request.sedeCentral() != null ? request.sedeCentral().departamento() : null,
                request.sedeCentral() != null ? request.sedeCentral().codigoPostal() : null,
                request.sedeCentral() != null ? request.sedeCentral().provinciaId() : null,
                request.sedeCentral() != null ? request.sedeCentral().departamentoId() : null,
                request.sedeCentral() != null ? request.sedeCentral().localidadId() : null
        );
    }
}
