package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Proveedor centralizado para resolver el usuario autenticado en el contexto de seguridad.
 */
@Component
public class AuthenticatedUserProvider {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatedUserProvider.class);
    private static final UUID DEFAULT_FALLBACK_USER_ID = UUID.fromString("b915837d-6a68-4ad1-95b9-5f6455e586ce");

    private final UserRepository userRepository;

    public AuthenticatedUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        String principal = auth.getName() != null ? auth.getName() : auth.getPrincipal().toString();
        if (principal.isBlank() || "anonymousUser".equalsIgnoreCase(principal)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(principal);
    }

    public UUID getAuthenticatedUserIdOrNull() {
        return getAuthenticatedUser().map(User::getId).orElse(null);
    }

    public UUID getAuthenticatedUserIdOrFallback() {
        Optional<User> current = getAuthenticatedUser();
        if (current.isPresent()) {
            return current.get().getId();
        }
        log.warn("No se encontró usuario autenticado activo en SecurityContext; aplicando resolución de respaldo.");
        return userRepository.findByEmail("admin@undec.edu.ar")
                .map(User::getId)
                .orElseGet(() -> userRepository.findAll().stream()
                        .findFirst()
                        .map(User::getId)
                        .orElse(DEFAULT_FALLBACK_USER_ID));
    }
}
