package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ProgramaAnaliticoAsignaturaDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramaAnaliticoControllerTest {

    @Mock
    private SpringDataProgramaAnaliticoAsignaturaRepository programaRepository;

    @Mock
    private SpringDataPlanEstudioFt001Repository planEstudioRepository;

    @Mock
    private SpringDataActividadCurricularPlanRepository asignaturaRepository;

    @Mock
    private SpringDataProcesoAcreditacionRepository procesoRepository;

    @Mock
    private PlanTrabajoRepositoryPort planTrabajoRepository;

    @Mock
    private DocumentStoragePort storagePort;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProgramaAnaliticoController controller;

    private UUID planId;
    private UUID procesoId;
    private UUID asignaturaId;
    private ActividadCurricularPlanJpaEntity asignatura;
    private PlanEstudioFt001JpaEntity plan;

    @BeforeEach
    void setUp() {
        planId = UUID.randomUUID();
        procesoId = UUID.randomUUID();
        asignaturaId = UUID.randomUUID();

        plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);
        plan.setProcesoId(procesoId);
        plan.setCodigoResolucion("Resolución HCS 015/24");

        CarreraJpaEntity carrera = new CarreraJpaEntity();
        carrera.setId(UUID.randomUUID());
        carrera.setNombre("Ingeniería en Agrimensura");
        plan.setCarrera(carrera);

        asignatura = new ActividadCurricularPlanJpaEntity();
        asignatura.setId(asignaturaId);
        asignatura.setNombre("Álgebra y Geometría Analítica");
        asignatura.setCodigo("CB01");
        asignatura.setSubgrupo("1º");
        asignatura.setPlanEstudio(plan);
    }

    @Test
    @DisplayName("GET /api/programas-analiticos?planId=... debe retornar lista de programas de ese plan")
    void listarPorPlanIdRetornaProgramas() {
        ProgramaAnaliticoAsignaturaJpaEntity entity = new ProgramaAnaliticoAsignaturaJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setProcesoId(procesoId);
        entity.setActividadCurricular(asignatura);
        entity.setNombreAsignatura(asignatura.getNombre());
        entity.setEstado("CARGADO");
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        when(programaRepository.findByPlanEstudioId(planId)).thenReturn(List.of(entity));

        ResponseEntity<List<ProgramaAnaliticoAsignaturaDto>> response = controller.listar(null, planId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        ProgramaAnaliticoAsignaturaDto dto = response.getBody().get(0);
        assertEquals(entity.getId(), dto.id());
        assertEquals(asignaturaId, dto.asignaturaPlanId());
        assertEquals(asignaturaId, dto.actividadCurricularId());
        assertEquals("CARGADO", dto.estadoCarga());
        assertEquals("CARGADO", dto.estado());
        assertEquals("Álgebra y Geometría Analítica", dto.asignaturaNombre());
    }

    @Test
    @DisplayName("POST /api/programas-analiticos/upload debe subir el archivo a MinIO y guardar el programa analítico")
    void subirProgramaExitoso() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "programa_algebra.pdf",
                "application/pdf",
                "%PDF-1.4 test content".getBytes()
        );

        when(asignaturaRepository.findById(asignaturaId)).thenReturn(Optional.of(asignatura));
        when(programaRepository.findByActividadCurricularId(asignaturaId)).thenReturn(Optional.empty());
        when(storagePort.uploadFile(any(), any(), eq("application/pdf"))).thenReturn("minio://bucket/programas/test.pdf");

        ProgramaAnaliticoAsignaturaJpaEntity guardado = new ProgramaAnaliticoAsignaturaJpaEntity();
        guardado.setId(UUID.randomUUID());
        guardado.setProcesoId(procesoId);
        guardado.setActividadCurricular(asignatura);
        guardado.setNombreAsignatura(asignatura.getNombre());
        guardado.setArchivoNombre("programa_algebra.pdf");
        guardado.setArchivoRuta("minio://bucket/programas/test.pdf");
        guardado.setEstado("CARGADO");
        guardado.setResolucionAprobacion("Resolución HCS 015/24");
        guardado.setCreadoEn(Instant.now());
        guardado.setActualizadoEn(Instant.now());

        when(programaRepository.save(any(ProgramaAnaliticoAsignaturaJpaEntity.class))).thenReturn(guardado);

        ResponseEntity<ProgramaAnaliticoAsignaturaDto> response = controller.subirPrograma(
                archivo,
                asignaturaId,
                procesoId,
                "Resolución HCS 015/24",
                2024,
                null,
                "Contenidos mínimos alineados"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CARGADO", response.getBody().estadoCarga());
        assertEquals(asignaturaId, response.getBody().asignaturaPlanId());
        assertEquals("programa_algebra.pdf", response.getBody().archivoNombre());

        verify(storagePort).uploadFile(any(), any(), eq("application/pdf"));
        verify(programaRepository).save(any(ProgramaAnaliticoAsignaturaJpaEntity.class));
    }
}
