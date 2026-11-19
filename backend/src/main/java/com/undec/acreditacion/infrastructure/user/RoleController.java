package com.undec.acreditacion.infrastructure.user;

import com.undec.acreditacion.application.input.ListRolesUseCase;
import com.undec.acreditacion.infrastructure.user.dto.RoleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('AEA')")
public class RoleController {

    private final ListRolesUseCase listRolesUseCase;

    public RoleController(ListRolesUseCase listRolesUseCase) {
        this.listRolesUseCase = listRolesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> listRoles() {
        List<RoleResponse> roles = listRolesUseCase.listRoles().stream()
                .map(RoleResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(roles);
    }
}
