package com.undec.acreditacion.infrastructure.user;

import com.undec.acreditacion.application.input.ChangeUserStatusUseCase;
import com.undec.acreditacion.application.input.CreateUserCommand;
import com.undec.acreditacion.application.input.CreateUserUseCase;
import com.undec.acreditacion.application.input.ListUsersUseCase;
import com.undec.acreditacion.application.input.UpdateUserCommand;
import com.undec.acreditacion.application.input.UpdateUserUseCase;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.infrastructure.user.dto.CreateUserRequest;
import com.undec.acreditacion.infrastructure.user.dto.UpdateUserRequest;
import com.undec.acreditacion.infrastructure.user.dto.UserResponse;
import com.undec.acreditacion.infrastructure.user.dto.UserStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('AEA')")
public class UserController {

    private final ListUsersUseCase listUsersUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;

    public UserController(ListUsersUseCase listUsersUseCase,
                          CreateUserUseCase createUserUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          ChangeUserStatusUseCase changeUserStatusUseCase) {
        this.listUsersUseCase = listUsersUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.changeUserStatusUseCase = changeUserStatusUseCase;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        List<UserResponse> users = listUsersUseCase.listUsers().stream()
                .map(UserResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
                request.username(),
                request.email(),
                request.password(),
                request.roleCodes(),
                request.unidadAcademicaId(),
                request.departamentoAcademicoId()
        );
        User created = createUserUseCase.createUser(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromDomain(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @RequestBody UpdateUserRequest request) {
        UpdateUserCommand command = new UpdateUserCommand(
                id,
                request.username(),
                request.email(),
                request.roleCodes(),
                request.newPassword(),
                request.unidadAcademicaId(),
                request.departamentoAcademicoId()
        );
        User updated = updateUserUseCase.updateUser(command);
        return ResponseEntity.ok(UserResponse.fromDomain(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID id,
                                                     @RequestBody UserStatusRequest request) {
        User updated = changeUserStatusUseCase.changeStatus(id, request.active());
        return ResponseEntity.ok(UserResponse.fromDomain(updated));
    }
}
