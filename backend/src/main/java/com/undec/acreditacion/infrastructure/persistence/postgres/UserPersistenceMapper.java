package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.Permission;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Set<Role> roles = entity.getRoles() != null
                ? entity.getRoles().stream().map(this::toDomainRole).collect(Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>();

        return User.rehydrate(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.isActive(),
                entity.isSystemUser(),
                entity.getUnidadAcademicaId(),
                entity.getDepartamentoAcademicoId(),
                roles
        );
    }

    public UserJpaEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserJpaEntity entity = new UserJpaEntity(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                user.isSystemUser(),
                Instant.now()
        );
        entity.setUnidadAcademicaId(user.getUnidadAcademicaId());
        entity.setDepartamentoAcademicoId(user.getDepartamentoAcademicoId());

        if (user.getRoles() != null) {
            Set<RoleJpaEntity> roleEntities = user.getRoles().stream()
                    .map(this::toEntityRole)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            entity.setRoles(roleEntities);
        }

        return entity;
    }

    public Role toDomainRole(RoleJpaEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }

        Set<Permission> permissions = roleEntity.getPermissions() != null
                ? roleEntity.getPermissions().stream()
                .map(this::toDomainPermission)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>();

        return Role.rehydrate(
                roleEntity.getId(),
                roleEntity.getCode(),
                roleEntity.getName(),
                roleEntity.getDescription(),
                roleEntity.isActive(),
                roleEntity.isSystemRole(),
                permissions
        );
    }

    public RoleJpaEntity toEntityRole(Role role) {
        if (role == null) {
            return null;
        }

        RoleJpaEntity entity = new RoleJpaEntity(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                role.isSystemRole(),
                Instant.now()
        );

        if (role.getPermissions() != null) {
            Set<PermissionJpaEntity> permissionEntities = role.getPermissions().stream()
                    .map(this::toEntityPermission)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            entity.setPermissions(permissionEntities);
        }

        return entity;
    }

    private Permission toDomainPermission(PermissionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Permission(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription()
        );
    }

    private PermissionJpaEntity toEntityPermission(Permission permission) {
        if (permission == null) {
            return null;
        }
        return new PermissionJpaEntity(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                Instant.now()
        );
    }
}
