package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.Permission;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPersistenceMapperTest {

    private final UserPersistenceMapper mapper = new UserPersistenceMapper();

    @Test
    @DisplayName("toDomain maps UserJpaEntity and its relations to domain User")
    void toDomainMapsEntityToDomain() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID permId = UUID.randomUUID();

        PermissionJpaEntity permEntity = new PermissionJpaEntity(
                permId,
                "SECURITY_USER_READ",
                "Consultar usuarios",
                "Permite listar usuarios",
                Instant.now()
        );

        RoleJpaEntity roleEntity = new RoleJpaEntity(
                roleId,
                "AEA",
                "Área de Evaluación y Acreditación",
                "Administracion",
                true,
                true,
                Instant.now()
        );
        roleEntity.setPermissions(Set.of(permEntity));

        UserJpaEntity userEntity = new UserJpaEntity(
                userId,
                "admin",
                "admin@undec.edu.ar",
                "hashed_pwd",
                true,
                true,
                Instant.now()
        );
        userEntity.setRoles(Set.of(roleEntity));

        User domainUser = mapper.toDomain(userEntity);

        assertNotNull(domainUser);
        assertEquals(userId, domainUser.getId());
        assertEquals("admin", domainUser.getUsername());
        assertEquals("admin@undec.edu.ar", domainUser.getEmail());
        assertEquals("hashed_pwd", domainUser.getPasswordHash());
        assertTrue(domainUser.isActive());
        assertTrue(domainUser.isSystemUser());
        assertEquals(1, domainUser.getRoles().size());

        Role domainRole = domainUser.getRoles().iterator().next();
        assertEquals("AEA", domainRole.getCode());
        assertTrue(domainRole.isSystemRole());
        assertEquals(1, domainRole.getPermissions().size());

        Permission domainPerm = domainRole.getPermissions().iterator().next();
        assertEquals("SECURITY_USER_READ", domainPerm.getCode());
    }

    @Test
    @DisplayName("toEntity maps domain User to UserJpaEntity")
    void toEntityMapsDomainToEntity() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID permId = UUID.randomUUID();

        Permission permission = new Permission(permId, "SECURITY_USER_READ", "Consultar", "Desc");
        Role role = Role.rehydrate(roleId, "AEA", "Área de Evaluación y Acreditación", "Desc", true, true, Set.of(permission));
        User user = User.rehydrate(userId, "admin", "admin@undec.edu.ar", "hashed_pwd", true, true, Set.of(role));

        UserJpaEntity entity = mapper.toEntity(user);

        assertNotNull(entity);
        assertEquals(userId, entity.getId());
        assertEquals("admin", entity.getUsername());
        assertEquals("admin@undec.edu.ar", entity.getEmail());
        assertEquals("hashed_pwd", entity.getPasswordHash());
        assertTrue(entity.isActive());
        assertTrue(entity.isSystemUser());
        assertEquals(1, entity.getRoles().size());
    }

    @Test
    @DisplayName("toDomain and toEntity return null for null input")
    void nullInputsReturnNull() {
        assertNull(mapper.toDomain(null));
        assertNull(mapper.toEntity(null));
    }
}
