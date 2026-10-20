package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("normativaSecurity")
public class NormativaSecurityEvaluator {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public NormativaSecurityEvaluator(ProcesoAcreditacionRepositoryPort procesoRepository) {
        this.procesoRepository = procesoRepository;
    }

    public boolean canAccess(Authentication auth, UUID procesoId) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (hasRole(auth, "ADMINISTRATOR") || hasRole(auth, "AEA")) {
            return true;
        }
        return hasRole(auth, "CAREER_DIRECTOR")
                || hasAuthority(auth, "NORMATIVA_SOLICITAR")
                || hasAuthority(auth, "NORMATIVA_CARGAR_BORRADOR")
                || hasAuthority(auth, "NORMATIVA_ENVIAR")
                || hasAuthority(auth, "CONVOCATORIA_READ");
    }

    public boolean canManage(Authentication auth, UUID procesoId) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (hasRole(auth, "ADMINISTRATOR") || hasRole(auth, "AEA")) {
            return true;
        }
        return hasRole(auth, "CAREER_DIRECTOR")
                || hasAuthority(auth, "NORMATIVA_CARGAR_BORRADOR")
                || hasAuthority(auth, "NORMATIVA_ENVIAR");
    }

    public boolean canReview(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return hasRole(auth, "ADMINISTRATOR")
                || hasRole(auth, "AEA")
                || hasAuthority(auth, "NORMATIVA_REVISAR");
    }

    public boolean canExportHojaInfo(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return hasRole(auth, "ADMINISTRATOR")
                || hasRole(auth, "AEA")
                || hasRole(auth, "CAREER_DIRECTOR")
                || hasAuthority(auth, "NORMATIVA_EXPORT_INFO");
    }

    private boolean hasRole(Authentication auth, String role) {
        String roleName = "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase(roleName) || a.equalsIgnoreCase(role));
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase(authority));
    }
}
