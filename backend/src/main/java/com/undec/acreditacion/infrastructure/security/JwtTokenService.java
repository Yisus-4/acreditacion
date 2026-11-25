package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.application.output.TokenService;
import com.undec.acreditacion.domain.auth.TokenPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenService implements TokenService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:3600000}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(UUID userId, String email, List<String> roles) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    @Override
    public Optional<TokenPayload> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles", List.class);

            Date expiration = claims.getExpiration();
            Instant expiresAt = expiration != null ? expiration.toInstant() : Instant.now();

            return Optional.of(new TokenPayload(userId, email, roles, expiresAt));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
