package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.application.output.TokenService;
import com.undec.acreditacion.domain.auth.TokenPayload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenAuthenticationFilterTest {

    private TokenService tokenService;
    private FilterChain filterChain;
    private TokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        filterChain = mock(FilterChain.class);
        filter = new TokenAuthenticationFilter(tokenService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("valid Bearer token populates SecurityContext and continues chain")
    void validTokenAuthenticates() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String rawToken = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + rawToken);

        UUID userId = UUID.randomUUID();
        TokenPayload payload = new TokenPayload(
                userId,
                "admin@undec.edu.ar",
                List.of("AEA"),
                Instant.now().plusSeconds(3600)
        );

        when(tokenService.validateToken(rawToken)).thenReturn(Optional.of(payload));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set in context");
        assertEquals("admin@undec.edu.ar", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AEA")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("missing Authorization header leaves SecurityContext empty and continues chain")
    void missingHeaderContinuesWithoutAuth() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("expired token leaves SecurityContext empty and continues chain")
    void expiredTokenDoesNotAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String rawToken = "expired-token";
        request.addHeader("Authorization", "Bearer " + rawToken);

        TokenPayload payload = new TokenPayload(
                UUID.randomUUID(),
                "admin@undec.edu.ar",
                List.of("AEA"),
                Instant.now().minusSeconds(10) // expired
        );

        when(tokenService.validateToken(rawToken)).thenReturn(Optional.of(payload));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("invalid token leaves SecurityContext empty and continues chain")
    void invalidTokenDoesNotAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String rawToken = "bad-token";
        request.addHeader("Authorization", "Bearer " + rawToken);

        when(tokenService.validateToken(rawToken)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
