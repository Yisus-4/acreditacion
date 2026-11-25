package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.ConsultarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.input.FinalizarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.input.GuardarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import com.undec.acreditacion.infrastructure.web.dto.GuardarUsuariosAteneaRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/procesos/{procesoId}/usuarios-atenea", "/api/convocatorias/{procesoId}/usuarios-atenea"})
public class UsuariosAteneaController {

    private final ConsultarRegistroUsuariosAteneaUseCase consultarUseCase;
    private final GuardarRegistroUsuariosAteneaUseCase guardarUseCase;
    private final FinalizarRegistroUsuariosAteneaUseCase finalizarUseCase;
    private final DocumentStoragePort storagePort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UsuariosAteneaController(
            ConsultarRegistroUsuariosAteneaUseCase consultarUseCase,
            GuardarRegistroUsuariosAteneaUseCase guardarUseCase,
            FinalizarRegistroUsuariosAteneaUseCase finalizarUseCase,
            DocumentStoragePort storagePort,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.consultarUseCase = consultarUseCase;
        this.guardarUseCase = guardarUseCase;
        this.finalizarUseCase = finalizarUseCase;
        this.storagePort = storagePort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<RegistroUsuariosAteneaDto> consultar(@PathVariable UUID procesoId) {
        RegistroUsuariosAteneaDto dto = consultarUseCase.consultarPorProcesoId(procesoId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    @PreAuthorize("hasRole('AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroUsuariosAteneaDto> guardar(
            @PathVariable UUID procesoId,
            @RequestBody GuardarUsuariosAteneaRequest request
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        RegistroUsuariosAteneaDto dto = guardarUseCase.guardar(procesoId, usuarioId, request.usuarios(), request.matrizPermisos());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/plazo")
    @PreAuthorize("hasRole('AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroUsuariosAteneaDto> actualizarPlazo(
            @PathVariable UUID procesoId,
            @RequestParam(value = "diasHabiles", required = false) Integer diasHabiles,
            @RequestParam(value = "nuevaFechaLimite", required = false) java.time.LocalDate nuevaFechaLimite
    ) {
        RegistroUsuariosAteneaDto dto;
        if (nuevaFechaLimite != null) {
            dto = guardarUseCase.extenderPlazoFecha(procesoId, nuevaFechaLimite);
        } else {
            dto = guardarUseCase.actualizarPlazo(procesoId, diasHabiles != null ? diasHabiles : 2);
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/finalizar")
    @PreAuthorize("hasRole('AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroUsuariosAteneaDto> finalizar(@PathVariable UUID procesoId) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        RegistroUsuariosAteneaDto dto = finalizarUseCase.finalizar(procesoId, usuarioId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/descargar")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID procesoId) {
        RegistroUsuariosAteneaDto dto = consultarUseCase.consultarPorProcesoId(procesoId);
        if (dto.rutaDocumento() == null || dto.rutaDocumento().isBlank()) {
            throw new ResourceNotFoundException("El documento RG-003 aún no ha sido generado o no se encuentra disponible");
        }

        byte[] bytes = storagePort.downloadFile(dto.rutaDocumento());
        if (bytes != null && esPdfLegacyStub(bytes)) {
            byte[] regenerado = finalizarUseCase.regenerarPdf(procesoId);
            if (regenerado != null && regenerado.length > 0) {
                bytes = regenerado;
            }
        }
        String filename = "RG-003_Registro_de_usuarios_de_ATENEA.pdf";
        if (dto.rutaDocumento().contains("/")) {
            filename = dto.rutaDocumento().substring(dto.rutaDocumento().lastIndexOf('/') + 1);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    private boolean esPdfLegacyStub(byte[] bytes) {
        String header = new String(bytes, 0, Math.min(bytes.length, 4096), java.nio.charset.StandardCharsets.ISO_8859_1);
        return header.contains("% REGISTRO OFICIAL RG-003:") || header.contains("% SECCION I: TABLA DE USUARIOS");
    }

    private UUID resolverUsuarioAutenticadoId() {
        return authenticatedUserProvider.getAuthenticatedUserIdOrFallback();
    }
}
