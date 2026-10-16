package com.undec.acreditacion.infrastructure.auth;

import com.undec.acreditacion.application.input.RegisterUserUseCase;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.infrastructure.auth.dto.RegisterRequest;
import com.undec.acreditacion.infrastructure.auth.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class RegisterController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    public RegisterController(
            RegisterUserUseCase registerUserUseCase,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        String passwordHash = passwordHasher.hash(request.password());
        User user = registerUserUseCase.register(request.username(), request.email(), passwordHash);

        if (request.roleCodes() != null) {
            for (String code : request.roleCodes()) {
                roleRepository.findByCode(code).ifPresent(user::assignRole);
            }
            userRepository.save(user);
        }

        List<String> roleCodes = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).toList()
                : List.of();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(user.getId(), user.getUsername(), user.getEmail(), user.isActive(), roleCodes));
    }
}
