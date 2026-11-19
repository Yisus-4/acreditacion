package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.infrastructure.autoevaluacion.dto.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoevaluacionInfraestructuraControllerTest {

    @Mock
    private SpringDataInmuebleRepository inmuebleRepository;

    @Mock
    private SpringDataTipoEspacioFisicoRepository tipoEspacioRepository;

    @Mock
    private SpringDataEspacioFisicoRepository espacioFisicoRepository;

    @Mock
    private SpringDataInmuebleDocumentacionRespaldoRepository documentacionRepository;

    @Mock
    private SpringDataInstitucionRepository institucionRepository;

    @Mock
    private SpringDataProvinciaRepository provinciaRepository;

    @Mock
    private SpringDataDepartamentoRepository departamentoRepository;

    @Mock
    private SpringDataLocalidadRepository localidadRepository;

    @Mock
    private SpringDataCarreraRepository carreraRepository;

    @Mock
    private SpringDataSedeLocalizacionRepository sedeRepository;

    @Mock
    private com.undec.acreditacion.application.output.UserRepository userRepository;

    @Mock
    private com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort planTrabajoRepository;

    @InjectMocks
    private AutoevaluacionInfraestructuraController controller;

    private InmuebleJpaEntity inmueble;
    private EspacioFisicoJpaEntity espacioPadre;
    private EspacioFisicoJpaEntity subespacio;

    @BeforeEach
    void setUp() {
        inmueble = new InmuebleJpaEntity();
        inmueble.setId(UUID.randomUUID());
        inmueble.setNombre("Campus Universitario");
        inmueble.setDomicilio("Av. Los Sarmientos 123");
        inmueble.setCalle("Av. Los Sarmientos");
        inmueble.setNumero("123");
        inmueble.setSuperficieCubiertaM2(new BigDecimal("1200.00"));
        inmueble.setSuperficieTotalM2(new BigDecimal("3000.00"));
        inmueble.setCondicionTenencia("PROPIO");
        inmueble.setPropiedadInstitucion(true);
        inmueble.setActivo(true);

        espacioPadre = new EspacioFisicoJpaEntity();
        espacioPadre.setId(UUID.randomUUID());
        espacioPadre.setInmueble(inmueble);
        espacioPadre.setDenominacion("Lab Alta Complejidad");
        espacioPadre.setDescripcionInfraestructura("Laboratorio Central");
        espacioPadre.setCantidad(1);
        espacioPadre.setCapacidadPersonasSentadas(60);
        espacioPadre.setCapacidadEstudiantes(60);
        espacioPadre.setSuperficieM2(new BigDecimal("180.00"));
        espacioPadre.setTieneSubespacios(true);
        espacioPadre.setObservacionHigieneSeguridad("Ventilación forzada");
        espacioPadre.setActivo(true);

        subespacio = new EspacioFisicoJpaEntity();
        subespacio.setId(UUID.randomUUID());
        subespacio.setInmueble(inmueble);
        subespacio.setEspacioPadre(espacioPadre);
        subespacio.setDenominacion("Área Microscopía");
        subespacio.setDescripcionInfraestructura("Microscopios");
        subespacio.setCantidad(1);
        subespacio.setCapacidadPersonasSentadas(15);
        subespacio.setCapacidadEstudiantes(15);
        subespacio.setSuperficieM2(new BigDecimal("40.00"));
        subespacio.setTieneSubespacios(false);
        subespacio.setObservacionHigieneSeguridad("Campana extractora OK");
        subespacio.setActivo(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateRole(String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@undec.edu.ar",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("Bedelía en PUT /inmuebles/{id} solo puede mutar capacidades y no modifica metadatos ni crea espacios")
    void bedeliaNoModificaMetadatosInmuebleNiCreaEspacios() {
        authenticateRole("BEDELIA");

        when(inmuebleRepository.findById(inmueble.getId())).thenReturn(Optional.of(inmueble));
        when(inmuebleRepository.save(any(InmuebleJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(espacioFisicoRepository.findByInmuebleIdAndActivoTrue(inmueble.getId())).thenReturn(List.of(espacioPadre));
        when(espacioFisicoRepository.findById(espacioPadre.getId())).thenReturn(Optional.of(espacioPadre));
        when(espacioFisicoRepository.save(any(EspacioFisicoJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        // Request intentando modificar nombre, metros cuadrados y capacidad
        EspacioFisicoRequest espReq = new EspacioFisicoRequest(
                espacioPadre.getId(),
                null,
                "Intento Cambio Denominacion",
                "Intento Cambio Descripcion",
                2,
                80,
                80,
                new BigDecimal("500.00"), // Intento alterar m2
                "Nuevo Equipamiento",
                "CARGADO",
                true,
                "Intento alterar HyS", // Intento alterar HyS
                Collections.emptyList()
        );

        // Intento crear un nuevo espacio físico siendo Bedelía
        EspacioFisicoRequest nuevoEspReq = new EspacioFisicoRequest(
                null, null, "Nuevo Espacio Ilegal", "Desc", 1, 30, 30, new BigDecimal("40.00"), null, "CARGADO", false, null, Collections.emptyList()
        );

        RegistrarInmuebleRequest req = new RegistrarInmuebleRequest(
                1,
                "Nombre Hackeado",
                "Calle Falsa 123",
                "Calle Falsa",
                "123",
                null,
                null, null, null, // provinciaId, departamentoId, localidadId
                2020, 2020, false, "ALQUILADO", "Alquiler", false,
                true, null, // esMaestro, procesoId
                new BigDecimal("9999.00"), // Intento alterar superficie
                new BigDecimal("9999.00"),
                "Observacion",
                "CARGADO",
                null, // carrerasIds
                List.of(espReq, nuevoEspReq),
                List.of(new RegistrarInmuebleRequest.DocumentoRespaldoRequest("doc.pdf", "/minio/doc.pdf", "ANEXO", "Desc"))
        );

        ResponseEntity<InmuebleResponse> response = controller.actualizarInmueble(inmueble.getId(), req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Metadatos preservados intactos
        assertEquals("Campus Universitario", inmueble.getNombre());
        assertEquals("Av. Los Sarmientos 123", inmueble.getDomicilio());
        assertEquals(new BigDecimal("1200.00"), inmueble.getSuperficieCubiertaM2());
        assertEquals("PROPIO", inmueble.getCondicionTenencia());
        assertTrue(inmueble.getPropiedadInstitucion());

        // Espacio físico: capacidad modificada a 80
        assertEquals(80, espacioPadre.getCapacidadPersonasSentadas());
        assertEquals(2, espacioPadre.getCantidad());
        // Características físicas y HyS preservadas
        assertEquals("Lab Alta Complejidad", espacioPadre.getDenominacion());
        assertEquals(new BigDecimal("180.00"), espacioPadre.getSuperficieM2());
        assertEquals("Ventilación forzada", espacioPadre.getObservacionHigieneSeguridad());

        // Documentos de respaldo no fueron agregados
        verify(documentacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Infraestructura en PUT /inmuebles/{id} preserva observacionHigieneSeguridad existente")
    void infraestructuraNoSobrescribeObservacionHyS() {
        authenticateRole("INFRAESTRUCTURA");

        when(inmuebleRepository.findById(inmueble.getId())).thenReturn(Optional.of(inmueble));
        when(inmuebleRepository.save(any(InmuebleJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(espacioFisicoRepository.findByInmuebleIdAndActivoTrue(inmueble.getId())).thenReturn(List.of(espacioPadre));
        when(espacioFisicoRepository.findById(espacioPadre.getId())).thenReturn(Optional.of(espacioPadre));
        when(espacioFisicoRepository.save(any(EspacioFisicoJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        EspacioFisicoRequest espReq = new EspacioFisicoRequest(
                espacioPadre.getId(),
                null,
                "Lab Renovado",
                "Infraestructura actualizada",
                1,
                60,
                60,
                new BigDecimal("210.00"),
                "Equipamiento completo",
                "CARGADO",
                true,
                "Intento de sobreescritura HyS por Infra",
                Collections.emptyList()
        );

        RegistrarInmuebleRequest req = new RegistrarInmuebleRequest(
                1,
                "Campus Norte",
                "Av. Nueva 456",
                "Av. Nueva",
                "456",
                null,
                null, null, null,
                2010, 2012, true, "PROPIO", "Propiedad", true,
                true, null,
                new BigDecimal("1500.00"),
                new BigDecimal("3500.00"),
                "Observacion Infra",
                "CARGADO",
                null,
                List.of(espReq),
                Collections.emptyList()
        );

        ResponseEntity<InmuebleResponse> response = controller.actualizarInmueble(inmueble.getId(), req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Metadatos de Infra actualizados
        assertEquals("Campus Norte", inmueble.getNombre());
        assertEquals(new BigDecimal("1500.00"), inmueble.getSuperficieCubiertaM2());
        assertEquals("Lab Renovado", espacioPadre.getDenominacion());
        assertEquals(new BigDecimal("210.00"), espacioPadre.getSuperficieM2());

        // Observación de HyS PRESERVADA INTACTA
        assertEquals("Ventilación forzada", espacioPadre.getObservacionHigieneSeguridad());
    }

    @Test
    @DisplayName("Rol HyS debe poder actualizar la observación de Higiene y Seguridad del espacio")
    void actualizarHigieneSeguridadEspacio() {
        when(espacioFisicoRepository.findById(espacioPadre.getId())).thenReturn(Optional.of(espacioPadre));
        when(espacioFisicoRepository.save(any(EspacioFisicoJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        ActualizarHigieneSeguridadRequest req = new ActualizarHigieneSeguridadRequest("Actualización de protocolo biológico");

        ResponseEntity<EspacioFisicoResponse> resp = controller.actualizarHigieneSeguridad(espacioPadre.getId(), req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Actualización de protocolo biológico", resp.getBody().observacionHigieneSeguridad());
    }

    @Test
    @DisplayName("Debe consultar espacios maestros por rango de metros cuadrados (m²) sin mezclar copias de proceso")
    void consultarEspaciosPorRangoM2() {
        when(espacioFisicoRepository.consultarMaestrosPorFiltros(isNull(), eq(new BigDecimal("30.00")), eq(new BigDecimal("100.00")), isNull()))
                .thenReturn(List.of(subespacio));

        ResponseEntity<ConsultaEspaciosResponse> resp = controller.consultarEspacios(
                null,
                new BigDecimal("30.00"),
                new BigDecimal("100.00"),
                null
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().totalAulas());
        assertEquals(15, resp.getBody().capacidadTotalPersonas());
        assertEquals(new BigDecimal("40.00"), resp.getBody().superficieTotalM2());
        assertTrue(resp.getBody().espacios().get(0).esSubespacio());
        assertEquals(espacioPadre.getId(), resp.getBody().espacios().get(0).espacioPadreId());
        verify(espacioFisicoRepository, never()).consultarPorProcesoYFiltros(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe consultar espacios únicamente de la autoevaluación indicada cuando se envía procesoId")
    void consultarEspaciosEnProcesoFiltraUnicamentePorProceso() {
        UUID procesoId = UUID.randomUUID();
        espacioPadre.setEsMaestro(false);
        espacioPadre.setProcesoId(procesoId);
        when(espacioFisicoRepository.consultarPorProcesoYFiltros(eq(procesoId), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(espacioPadre));

        ResponseEntity<ConsultaEspaciosResponse> resp = controller.consultarEspacios(
                procesoId,
                null,
                null,
                null,
                null
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().totalAulas());
        verify(espacioFisicoRepository).consultarPorProcesoYFiltros(procesoId, null, null, null, null);
        verify(espacioFisicoRepository, never()).consultarMaestrosPorFiltros(any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /procesos/{procesoId}/inmuebles debe devolver solo las copias aisladas del proceso y no el padrón maestro")
    void obtenerInmueblesProcesoSinSnapshotMuestraTroncoPendiente() {
        UUID procesoId = UUID.randomUUID();
        inmueble.setEsMaestro(false);
        inmueble.setProcesoId(procesoId);
        inmueble.setVerificado(true);
        inmueble.setActivo(true);

        when(documentacionRepository.findByTipoAnexoOrderByFechaSubidaDesc(anyString())).thenReturn(Collections.emptyList());
        when(inmuebleRepository.findByProcesoIdActivos(procesoId)).thenReturn(List.of(inmueble));

        ResponseEntity<?> resp = controller.obtenerInmueblesProceso(procesoId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<InmuebleResponse> lista = (List<InmuebleResponse>) resp.getBody();
        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertFalse(lista.get(0).esMaestro());
        assertFalse(lista.get(0).verificado());
        assertEquals("CARGADO", lista.get(0).estadoConeau());
        verify(inmuebleRepository, never()).findMaestrosActivos();
        verify(inmuebleRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /inmuebles con esMaestro=false y procesoId guarda inmueble, espacio y subespacio aislados en el proceso")
    void registrarInmuebleEnProcesoRespetaAislamientoYSubespacios() {
        authenticateRole("INFRAESTRUCTURA");
        UUID procesoId = UUID.randomUUID();
        InstitucionJpaEntity inst = new InstitucionJpaEntity();
        inst.setId(UUID.randomUUID());

        when(institucionRepository.findAll()).thenReturn(List.of(inst));
        when(inmuebleRepository.save(any(InmuebleJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(espacioFisicoRepository.save(any(EspacioFisicoJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        EspacioFisicoRequest subReq = new EspacioFisicoRequest(
                null, null, "Cabina Audio", "Cabina", 1, 4, 4,
                new BigDecimal("12.00"), "Consola", "CARGADO", false, null, Collections.emptyList()
        );
        EspacioFisicoRequest espReq = new EspacioFisicoRequest(
                null, null, "Laboratorio Multimedia", "Lab", 1, 20, 20,
                new BigDecimal("60.00"), "PCs", "CARGADO", true, null, List.of(subReq)
        );
        RegistrarInmuebleRequest req = new RegistrarInmuebleRequest(
                1, "Edificio Anexo", "Av. Perón 100", "Av. Perón", "100",
                null, null, null, null,
                2020, 2020, true, "PROPIO", "Propiedad", false,
                false, procesoId,
                new BigDecimal("500.00"), new BigDecimal("800.00"),
                null, "CARGADO", null, List.of(espReq), Collections.emptyList()
        );

        ResponseEntity<InmuebleResponse> resp = controller.registrarInmueble(req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertFalse(resp.getBody().esMaestro());
        assertEquals(procesoId, resp.getBody().procesoId());
        verify(espacioFisicoRepository, times(2)).save(argThat(esp ->
                Boolean.FALSE.equals(esp.getEsMaestro()) && procesoId.equals(esp.getProcesoId())
        ));
    }

    @Test
    @DisplayName("POST /procesos/{procesoId}/infraestructura/verificar-y-remitir debe copiar el archivo RG-005 a la carpeta de la carrera sin duplicar filas en inmueble")
    void verificarYRemitirCopiaArchivoEnCarpetaDeCarrera() {
        UUID procesoId = UUID.randomUUID();
        inmueble.setEsMaestro(true);
        inmueble.setVerificado(false);
        inmueble.setActivo(true);
        inmueble.getEspacios().add(espacioPadre);

        when(inmuebleRepository.findMaestrosActivos()).thenReturn(List.of(inmueble));
        when(documentacionRepository.findByTipoAnexoOrderByFechaSubidaDesc(anyString())).thenReturn(Collections.emptyList());
        when(documentacionRepository.save(any(InmuebleDocumentacionRespaldoJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.empty());

        ResponseEntity<VerificarInfraestructuraResponse> resp = controller.verificarYRemitirInfraestructura(procesoId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().verificado());
        assertFalse(inmueble.getVerificado());
        verify(inmuebleRepository, never()).save(any());
        verify(documentacionRepository, times(1)).save(argThat(doc ->
                doc.getTipoAnexo() != null && doc.getTipoAnexo().startsWith("SNAPSHOT_RG005:")
                        && doc.getRutaRepositorio().contains("03_01-Infraestructura")
        ));
    }
}
