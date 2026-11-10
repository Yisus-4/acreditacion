package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserJpaEntity> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserJpaEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    List<UserJpaEntity> findAll();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT DISTINCT u FROM UserJpaEntity u JOIN u.roles r " +
           "WHERE r.code = 'DOCENTE' AND u.active = true " +
           "AND (:query IS NULL OR :query = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<UserJpaEntity> findDocentesActivos(@Param("query") String query);
}
