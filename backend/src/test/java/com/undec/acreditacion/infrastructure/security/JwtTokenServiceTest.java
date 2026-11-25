package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.domain.auth.TokenPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    private static final String SECRET_256_BIT = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET_256_BIT, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generates a valid JWT token and decodes claims accurately")
    void generatesAndDecodesToken() {
        UUID userId = UUID.randomUUID();
        String email = "admin@undec.edu.ar";
        List<String> roles = List.of("AEA", "DIRECTOR_ESCUELA");

        String token = jwtTokenService.generateToken(userId, email, roles);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Optional<TokenPayload> payloadOpt = jwtTokenService.validateToken(token);

        assertTrue(payloadOpt.isPresent(), "Token should be successfully validated");
        TokenPayload payload = payloadOpt.get();

        assertEquals(userId, payload.userId());
        assertEquals(email, payload.email());
        assertEquals(roles, payload.roles());
        assertFalse(payload.isExpired(), "Fresh token should not be expired");
    }

    @Test
    @DisplayName("validateToken returns empty for expired token")
    void returnsEmptyForExpiredToken() {
        JwtTokenService expiredService = new JwtTokenService(SECRET_256_BIT, -1000); // expired 1 sec ago
        UUID userId = UUID.randomUUID();

        String token = expiredService.generateToken(userId, "admin@undec.edu.ar", List.of("AEA"));

        Optional<TokenPayload> payload = jwtTokenService.validateToken(token);

        assertTrue(payload.isEmpty(), "Expired token must not be valid");
    }

    @Test
    @DisplayName("validateToken returns empty for tampered token")
    void returnsEmptyForTamperedToken() {
        String token = jwtTokenService.generateToken(UUID.randomUUID(), "test@undec.edu.ar", List.of("ROLE"));
        String tampered = token + "corrupted";

        Optional<TokenPayload> payload = jwtTokenService.validateToken(tampered);

        assertTrue(payload.isEmpty(), "Tampered token must be rejected");
    }

    @Test
    @DisplayName("validateToken returns empty for null or blank string")
    void returnsEmptyForNullOrBlank() {
        assertTrue(jwtTokenService.validateToken(null).isEmpty());
        assertTrue(jwtTokenService.validateToken("   ").isEmpty());
    }
}
