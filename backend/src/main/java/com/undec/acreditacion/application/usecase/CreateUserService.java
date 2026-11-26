package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CreateUserCommand;
import com.undec.acreditacion.application.input.CreateUserUseCase;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    public CreateUserService(UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.roleRepository = Objects.requireNonNull(roleRepository, "Role repository is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher is required");
    }

    @Override
    public User createUser(CreateUserCommand command) {
        Objects.requireNonNull(command, "Command is required");
        if (userRepository.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username already in use: " + command.username());
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already in use: " + command.email());
        }

        String passwordHash = passwordHasher.hash(command.password());
        User user = User.register(
                command.username(),
                command.email(),
                passwordHash,
                command.unidadAcademicaId(),
                command.departamentoAcademicoId()
        );

        if (command.roleCodes() != null && !command.roleCodes().isEmpty()) {
            Set<Role> roles = new LinkedHashSet<>();
            for (String code : command.roleCodes()) {
                Role role = roleRepository.findByCode(code)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + code));
                roles.add(role);
            }
            user.syncRoles(roles);
        }

        return userRepository.save(user);
    }
}
