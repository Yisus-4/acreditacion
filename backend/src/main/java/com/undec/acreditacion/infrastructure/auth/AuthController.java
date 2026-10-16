package com.undec.acreditacion.infrastructure.auth;

import com.undec.acreditacion.application.exception.RateLimitExceededException;
import com.undec.acreditacion.application.input.LoginUseCase;
import com.undec.acreditacion.application.output.LoginRateLimiter;
import com.undec.acreditacion.application.output.TokenService;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.infrastructure.auth.dto.LoginRequest;
import com.undec.acreditacion.infrastructure.auth.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public final class AuthController {

    private final LoginUseCase loginUseCase;
    private final TokenService tokenService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(
            LoginUseCase loginUseCase,
            TokenService tokenService,
            LoginRateLimiter rateLimiter
    ) {
        this.loginUseCase = loginUseCase;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        String rateLimitKey = clientIp + ":" + (request.email() != null ? request.email().trim().toLowerCase() : "");

        if (!rateLimiter.tryConsume(rateLimitKey)) {
            throw new RateLimitExceededException("Demasiados intentos de inicio de sesión. Por favor, reintente más tarde.");
        }

        User user = loginUseCase.login(request.email(), request.password());
        List<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).toList()
                : List.of();

        String token = tokenService.generateToken(user.getId(), user.getEmail(), roleCodes);
        long issuedAt = System.currentTimeMillis();

        LoginResponse.UserSessionView userView = new LoginResponse.UserSessionView(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                roleCodes
        );

        return ResponseEntity.ok(new LoginResponse(userView, token, issuedAt));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
