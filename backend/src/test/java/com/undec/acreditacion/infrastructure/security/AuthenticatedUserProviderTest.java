package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticatedUserProvider provider;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void retornaIdUsuarioAutenticadoCuandoExisteEnContexto() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(userRepository.findByEmail("docente@undec.edu.ar")).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("docente@undec.edu.ar", null, Collections.emptyList())
        );

        assertEquals(userId, provider.getAuthenticatedUserIdOrNull());
        assertEquals(userId, provider.getAuthenticatedUserIdOrFallback());
    }

    @Test
    void retornaNullYFallbackCuandoNoHayAutenticacion() {
        SecurityContextHolder.clearContext();
        UUID adminId = UUID.randomUUID();
        User admin = mock(User.class);
        when(admin.getId()).thenReturn(adminId);
        when(userRepository.findByEmail("admin@undec.edu.ar")).thenReturn(Optional.of(admin));

        assertNull(provider.getAuthenticatedUserIdOrNull());
        assertEquals(adminId, provider.getAuthenticatedUserIdOrFallback());
    }
}
