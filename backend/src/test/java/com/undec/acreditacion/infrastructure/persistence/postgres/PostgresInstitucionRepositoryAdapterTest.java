package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.AutoridadInstitucional;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.entities.SedeCentral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresInstitucionRepositoryAdapterTest {

    private SpringDataInstitucionRepository springDataRepository;
    private InstitucionPersistenceMapper mapper;
    private PostgresInstitucionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataInstitucionRepository.class);
        mapper = mock(InstitucionPersistenceMapper.class);
        adapter = new PostgresInstitucionRepositoryAdapter(springDataRepository, mapper);
    }

    private Institucion sampleInstitucion() {
        AutoridadInstitucional maxAut = AutoridadInstitucional.maximaAutoridad(
                "Pérez", "Juan", null, "123", "rector@undec.edu.ar");
        AutoridadInstitucional adminAut = AutoridadInstitucional.administradorInstitucional(
                "Gómez", "María", null, "456", "admin@undec.edu.ar", "Sec", "Rec");
        SedeCentral sede = SedeCentral.crear(
                "9 de Julio", "22", null, null, "5360", 1L, 1L, 1L);
        return Institucion.registrar(
                "Universidad Nacional de Chilecito",
                "UNdeC",
                "logos/undec.png",
                maxAut,
                adminAut,
                sede
        );
    }

    @Test
    @DisplayName("save delega en mapper y SpringDataInstitucionRepository")
    void saveDelegaCorrectamente() {
        Institucion domain = sampleInstitucion();
        InstitucionJpaEntity entity = new InstitucionJpaEntity();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(springDataRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Institucion result = adapter.save(domain);

        assertEquals(domain, result);
        verify(mapper).toEntity(domain);
        verify(springDataRepository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    @DisplayName("existsByNombre delega en springDataRepository con trim e ignoreCase")
    void existsByNombreDelega() {
        when(springDataRepository.existsByNombreIgnoreCase("UNdeC")).thenReturn(true);

        boolean exists = adapter.existsByNombre("  UNdeC  ");

        assertTrue(exists);
        verify(springDataRepository).existsByNombreIgnoreCase("UNdeC");
    }

    @Test
    @DisplayName("existsBySigla delega en springDataRepository con trim")
    void existsBySiglaDelega() {
        when(springDataRepository.existsBySigla("UNdeC")).thenReturn(true);

        boolean exists = adapter.existsBySigla("  UNdeC  ");

        assertTrue(exists);
        verify(springDataRepository).existsBySigla("UNdeC");
    }

    @Test
    @DisplayName("findAll delega y mapea a domain")
    void findAllDelegaYMapea() {
        Institucion domain = sampleInstitucion();
        InstitucionJpaEntity entity = new InstitucionJpaEntity();

        when(springDataRepository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Institucion> result = adapter.findAll();

        assertEquals(1, result.size());
        assertEquals(domain, result.get(0));
    }

    @Test
    @DisplayName("findById delega y mapea a domain")
    void findByIdDelegaYMapea() {
        UUID id = UUID.randomUUID();
        Institucion domain = sampleInstitucion();
        InstitucionJpaEntity entity = new InstitucionJpaEntity();

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Institucion> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    @DisplayName("findBySigla delega en springDataRepository con trim e ignoreCase")
    void findBySiglaDelega() {
        Institucion domain = sampleInstitucion();
        InstitucionJpaEntity entity = new InstitucionJpaEntity();

        when(springDataRepository.findBySiglaIgnoreCase("UNdeC")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Institucion> result = adapter.findBySigla("  UNdeC  ");

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
        verify(springDataRepository).findBySiglaIgnoreCase("UNdeC");
    }

    @Test
    @DisplayName("deleteById delega en springDataRepository")
    void deleteByIdDelega() {
        UUID id = UUID.randomUUID();

        adapter.deleteById(id);

        verify(springDataRepository).deleteById(id);
    }
}
