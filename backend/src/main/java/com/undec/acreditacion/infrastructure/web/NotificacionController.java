package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.ConsultarNotificacionesUseCase;
import com.undec.acreditacion.application.input.GestionarSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.ConteoNotificacionesDto;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ConsultarNotificacionesUseCase consultarNotificacionesUseCase;
    private final GestionarSolicitudNotificacionUseCase gestionarSolicitudNotificacionUseCase;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public NotificacionController(
            ConsultarNotificacionesUseCase consultarNotificacionesUseCase,
            GestionarSolicitudNotificacionUseCase gestionarSolicitudNotificacionUseCase,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.consultarNotificacionesUseCase = consultarNotificacionesUseCase;
        this.gestionarSolicitudNotificacionUseCase = gestionarSolicitudNotificacionUseCase;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<List<SolicitudNotificacionDto>> listar(
            @RequestParam(value = "rol", required = false, defaultValue = "AEA") String rol,
            @RequestParam(value = "estado", required = false) String estado
    ) {
        List<SolicitudNotificacionDto> lista = consultarNotificacionesUseCase.listar(rol, estado);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/conteo-pendientes")
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<ConteoNotificacionesDto> conteoPendientes(
            @RequestParam(value = "rol", required = false, defaultValue = "AEA") String rol
    ) {
        long conteo = consultarNotificacionesUseCase.contarPendientes(rol);
        return ResponseEntity.ok(new ConteoNotificacionesDto(conteo));
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<SolicitudNotificacionDto> aceptar(@PathVariable UUID id) {
        UUID resolutorId = resolverUsuarioAutenticadoId();
        SolicitudNotificacionDto res = gestionarSolicitudNotificacionUseCase.aceptar(id, resolutorId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<SolicitudNotificacionDto> rechazar(@PathVariable UUID id) {
        UUID resolutorId = resolverUsuarioAutenticadoId();
        SolicitudNotificacionDto res = gestionarSolicitudNotificacionUseCase.rechazar(id, resolutorId);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/marcar-leida")
    @PreAuthorize("hasRole('AEA')")
    public ResponseEntity<SolicitudNotificacionDto> marcarLeida(@PathVariable UUID id) {
        SolicitudNotificacionDto res = gestionarSolicitudNotificacionUseCase.marcarLeida(id);
        return ResponseEntity.ok(res);
    }

    private UUID resolverUsuarioAutenticadoId() {
        return authenticatedUserProvider.getAuthenticatedUserIdOrFallback();
    }
}
