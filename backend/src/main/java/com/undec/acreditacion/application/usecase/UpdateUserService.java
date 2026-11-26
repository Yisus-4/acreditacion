package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.UpdateUserCommand;
import com.undec.acreditacion.application.input.UpdateUserUseCase;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    public UpdateUserService(UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.roleRepository = Objects.requireNonNull(roleRepository, "Role repository is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher is required");
    }

    @Override
    public User updateUser(UpdateUserCommand command) {
        Objects.requireNonNull(command, "Command is required");
        Objects.requireNonNull(command.userId(), "User id is required");

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        if (command.username() != null && !command.username().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.existsByUsername(command.username())) {
                throw new IllegalArgumentException("Username already in use: " + command.username());
            }
        }

        if (command.email() != null && !command.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(command.email())) {
                throw new IllegalArgumentException("Email already in use: " + command.email());
            }
        }

        user.updateProfile(
                command.username() != null ? command.username() : user.getUsername(),
                command.email() != null ? command.email() : user.getEmail()
        );

        if (command.optionalNewPassword() != null && !command.optionalNewPassword().isBlank()) {
            user.changePassword(passwordHasher.hash(command.optionalNewPassword()));
        }

        user.assignUnidadAcademica(command.unidadAcademicaId());
        user.assignDepartamentoAcademico(command.departamentoAcademicoId());

        if (command.roleCodes() != null) {
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
