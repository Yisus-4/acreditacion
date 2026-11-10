package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.infrastructure.persistence.postgres.RoleJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataUserRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryExtensionTest {

    @Mock
    private SpringDataActividadCurricularPlanRepository actividadCurricularPlanRepository;

    @Mock
    private SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionCarreraAmbitoRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Test
    @DisplayName("findMaxNumeroOrdenByPlanEstudioId retorna el maximo orden o 0 si no hay asignaturas")
    void testFindMaxNumeroOrdenByPlanEstudioId() {
        UUID planId = UUID.randomUUID();

        when(actividadCurricularPlanRepository.findMaxNumeroOrdenByPlanEstudioId(planId))
                .thenReturn(0)
                .thenReturn(15);

        assertEquals(0, actividadCurricularPlanRepository.findMaxNumeroOrdenByPlanEstudioId(planId));
        assertEquals(15, actividadCurricularPlanRepository.findMaxNumeroOrdenByPlanEstudioId(planId));
        verify(actividadCurricularPlanRepository, times(2)).findMaxNumeroOrdenByPlanEstudioId(planId);
    }

    @Test
    @DisplayName("existsByPlanEstudioIdAndCodigo verifica unicidad de codigo dentro del plan")
    void testExistsByPlanEstudioIdAndCodigo() {
        UUID planId = UUID.randomUUID();
        String codigo = "AGR-101";

        when(actividadCurricularPlanRepository.existsByPlanEstudioIdAndCodigo(planId, codigo)).thenReturn(true);
        when(actividadCurricularPlanRepository.existsByPlanEstudioIdAndCodigo(planId, "AGR-999")).thenReturn(false);

        assertTrue(actividadCurricularPlanRepository.existsByPlanEstudioIdAndCodigo(planId, codigo));
        assertFalse(actividadCurricularPlanRepository.existsByPlanEstudioIdAndCodigo(planId, "AGR-999"));
    }

    @Test
    @DisplayName("findByProcesoIdOrderByAmbitoNumeroOrdenAsc filtra estrictamente por proceso")
    void testFindByProcesoIdOrderByAmbitoNumeroOrdenAsc() {
        UUID procesoId = UUID.randomUUID();
        AutoevaluacionCarreraAmbitoJpaEntity ambito = new AutoevaluacionCarreraAmbitoJpaEntity();
        ambito.setId(UUID.randomUUID());
        ambito.setProcesoId(procesoId);

        when(autoevaluacionCarreraAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId))
                .thenReturn(List.of(ambito));

        List<AutoevaluacionCarreraAmbitoJpaEntity> result =
                autoevaluacionCarreraAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(procesoId, result.get(0).getProcesoId());
    }

    @Test
    @DisplayName("findDocentesActivos retorna solo usuarios activos con rol DOCENTE")
    void testFindDocentesActivos() {
        UserJpaEntity docente = new UserJpaEntity(
                UUID.randomUUID(),
                "cgonzalez",
                "cgonzalez@undec.edu.ar",
                "hash",
                true,
                false,
                Instant.now()
        );
        RoleJpaEntity roleDocente = new RoleJpaEntity(
                UUID.randomUUID(),
                "DOCENTE",
                "Docente",
                "Docente universitario",
                true,
                true,
                Instant.now()
        );
        docente.setRoles(Set.of(roleDocente));

        when(userRepository.findDocentesActivos("gonzalez")).thenReturn(List.of(docente));

        List<UserJpaEntity> result = userRepository.findDocentesActivos("gonzalez");

        assertEquals(1, result.size());
        assertEquals("cgonzalez", result.get(0).getUsername());
        assertTrue(result.get(0).isActive());
        assertTrue(result.get(0).getRoles().stream().anyMatch(r -> "DOCENTE".equals(r.getCode())));
    }
}
