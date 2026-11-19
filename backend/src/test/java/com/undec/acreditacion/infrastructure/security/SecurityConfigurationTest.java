package com.undec.acreditacion.infrastructure.security;

import com.undec.acreditacion.application.input.LoginUseCase;
import com.undec.acreditacion.application.output.TokenService;
import com.undec.acreditacion.domain.auth.TokenPayload;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.infrastructure.auth.AuthController;
import com.undec.acreditacion.infrastructure.auth.AuthExceptionHandler;
import com.undec.acreditacion.application.output.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigurationTest {

    private MockMvc mockMvc;
    private TokenService tokenService;
    private LoginUseCase loginUseCase;
    private LoginRateLimiter rateLimiter;

    @RestController
    static class DummyProtectedController {
        @GetMapping("/api/protected/test")
        public String protectedResource() {
            return "ok";
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        tokenService = mock(TokenService.class);
        loginUseCase = mock(LoginUseCase.class);
        rateLimiter = mock(LoginRateLimiter.class);

        when(rateLimiter.tryConsume(anyString())).thenReturn(true);
        when(tokenService.generateToken(any(), any(), any())).thenReturn("mocked.jwt.token");

        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokenService);
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(filter, "http://localhost:4200");

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(loginUseCase, tokenService, rateLimiter), new DummyProtectedController())
                .setControllerAdvice(new AuthExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @Test
    @DisplayName("POST /auth/login is accessible without authentication")
    void loginEndpointIsAccessibleWithoutAuth() throws Exception {
        User user = User.register("admin", "admin@undec.edu.ar", "hash");
        when(loginUseCase.login("admin@undec.edu.ar", "admin123")).thenReturn(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@undec.edu.ar\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("protected resource with valid Bearer token proceeds through filter")
    void protectedResourceWithValidTokenProceeds() throws Exception {
        String token = "valid-token";
        TokenPayload payload = new TokenPayload(
                UUID.randomUUID(),
                "admin@undec.edu.ar",
                List.of("AEA"),
                Instant.now().plusSeconds(3600)
        );
        when(tokenService.validateToken(token)).thenReturn(Optional.of(payload));

        mockMvc.perform(get("/api/protected/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
