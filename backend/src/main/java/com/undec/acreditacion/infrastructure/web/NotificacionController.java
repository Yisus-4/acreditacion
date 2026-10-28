package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.ConsultarNotificacionesUseCase;
import com.undec.acreditacion.application.input.GestionarSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.ConteoNotificacionesDto;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ConsultarNotificacionesUseCase consultarNotificacionesUseCase;
    private final GestionarSolicitudNotificacionUseCase gestionarSolicitudNotificacionUseCase;
    private final UserRepository userRepository;

    public NotificacionController(
            ConsultarNotificacionesUseCase consultarNotificacionesUseCase,
            GestionarSolicitudNotificacionUseCase gestionarSolicitudNotificacionUseCase,
            UserRepository userRepository
    ) {
        this.consultarNotificacionesUseCase = consultarNotificacionesUseCase;
        this.gestionarSolicitudNotificacionUseCase = gestionarSolicitudNotificacionUseCase;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
    public ResponseEntity<List<SolicitudNotificacionDto>> listar(
            @RequestParam(value = "rol", required = false, defaultValue = "AEA") String rol,
            @RequestParam(value = "estado", required = false) String estado
    ) {
        List<SolicitudNotificacionDto> lista = consultarNotificacionesUseCase.listar(rol, estado);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/conteo-pendientes")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
    public ResponseEntity<ConteoNotificacionesDto> conteoPendientes(
            @RequestParam(value = "rol", required = false, defaultValue = "AEA") String rol
    ) {
        long conteo = consultarNotificacionesUseCase.contarPendientes(rol);
        return ResponseEntity.ok(new ConteoNotificacionesDto(conteo));
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
    public ResponseEntity<SolicitudNotificacionDto> aceptar(@PathVariable UUID id) {
        UUID resolutorId = resolverUsuarioAutenticadoId();
        SolicitudNotificacionDto res = gestionarSolicitudNotificacionUseCase.aceptar(id, resolutorId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
    public ResponseEntity<SolicitudNotificacionDto> rechazar(@PathVariable UUID id) {
        UUID resolutorId = resolverUsuarioAutenticadoId();
        SolicitudNotificacionDto res = gestionarSolicitudNotificacionUseCase.rechazar(id, resolutorId);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/marcar-leida")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
    public ResponseEntity<SolicitudNotificacionDto> marcarLeida(@PathVariable UUID id) {
        SolicitudNotificacionDto res = gestionarSolicitudNotificacionUseCase.marcarLeida(id);
        return ResponseEntity.ok(res);
    }

    private Optional<User> resolverUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            Optional<User> userOpt = userRepository.findByEmail(principal);
            if (userOpt.isPresent()) {
                return userOpt;
            }
        }
        return userRepository.findByEmail("admin@undec.edu.ar");
    }

    private UUID resolverUsuarioAutenticadoId() {
        return resolverUsuarioAutenticado()
                .map(User::getId)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().map(User::getId)
                        .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }
}
