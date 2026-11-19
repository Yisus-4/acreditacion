package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresRoleRepositoryAdapterTest {

    private SpringDataRoleRepository springDataRoleRepository;
    private UserPersistenceMapper mapper;
    private PostgresRoleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        springDataRoleRepository = mock(SpringDataRoleRepository.class);
        mapper = mock(UserPersistenceMapper.class);
        adapter = new PostgresRoleRepositoryAdapter(springDataRoleRepository, mapper);
    }

    @Test
    @DisplayName("findByCode delegates to springDataRoleRepository and maps to domain")
    void findByCodeDelegatesAndMaps() {
        String code = "AEA";
        RoleJpaEntity entity = new RoleJpaEntity();
        Role domain = Role.rehydrate(UUID.randomUUID(), code, "Admin", "Desc", true, true, Set.of());

        when(springDataRoleRepository.findByCode(code)).thenReturn(Optional.of(entity));
        when(mapper.toDomainRole(entity)).thenReturn(domain);

        Optional<Role> result = adapter.findByCode(code);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
        verify(springDataRoleRepository).findByCode(code);
        verify(mapper).toDomainRole(entity);
    }

    @Test
    @DisplayName("save delegates to mapper and springDataRoleRepository")
    void saveDelegates() {
        Role domain = Role.rehydrate(UUID.randomUUID(), "AEA", "AEA", "Desc", true, true, Set.of());
        RoleJpaEntity entity = new RoleJpaEntity();

        when(mapper.toEntityRole(domain)).thenReturn(entity);
        when(springDataRoleRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomainRole(entity)).thenReturn(domain);

        Role saved = adapter.save(domain);

        assertEquals(domain, saved);
        verify(mapper).toEntityRole(domain);
        verify(springDataRoleRepository).save(entity);
        verify(mapper).toDomainRole(entity);
    }
}
