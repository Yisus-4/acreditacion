package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NormativaSecurityEvaluatorTest {

    private NormativaSecurityEvaluator evaluator;
    private UUID procesoId;

    @BeforeEach
    void setUp() {
        ProcesoAcreditacionRepositoryPort procesoRepo = mock(ProcesoAcreditacionRepositoryPort.class);
        evaluator = new NormativaSecurityEvaluator(procesoRepo);
        procesoId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Roles canónicos DIRECTOR_CARRERA y DIRECTOR_ESCUELA tienen acceso y gestión en normativa")
    void rolesCanonicosTienenAccesoYGestion() {
        Authentication dcAuth = authWithRole("DIRECTOR_CARRERA");
        Authentication deAuth = authWithRole("DIRECTOR_ESCUELA");

        assertTrue(evaluator.canAccess(dcAuth, procesoId));
        assertTrue(evaluator.canManage(dcAuth, procesoId));
        assertTrue(evaluator.canExportHojaInfo(dcAuth));

        assertTrue(evaluator.canAccess(deAuth, procesoId));
        assertTrue(evaluator.canManage(deAuth, procesoId));
    }

    @Test
    @DisplayName("Roles duplicados obsoletos en inglés ya no otorgan acceso por rol")
    void rolesDuplicadosEnInglesRechazados() {
        Authentication legacyDc = authWithRole("CAREER_DIRECTOR");
        Authentication legacyDe = authWithRole("SCHOOL_DIRECTOR");

        assertFalse(evaluator.canAccess(legacyDc, procesoId));
        assertFalse(evaluator.canManage(legacyDc, procesoId));
        assertFalse(evaluator.canAccess(legacyDe, procesoId));
    }

    private static Authentication authWithRole(String roleCode) {
        return new UsernamePasswordAuthenticationToken(
                "user@undec.edu.ar",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + roleCode))
        );
    }
}
