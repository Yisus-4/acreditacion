package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.infrastructure.autoevaluacion.dto.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

        espacioPadre = new EspacioFisicoJpaEntity();
        espacioPadre.setId(UUID.randomUUID());
        espacioPadre.setInmueble(inmueble);
        espacioPadre.setDenominacion("Lab Alta Complejidad");
        espacioPadre.setCantidad(1);
        espacioPadre.setCapacidadPersonasSentadas(60);
        espacioPadre.setSuperficieM2(new BigDecimal("180.00"));
        espacioPadre.setTieneSubespacios(true);
        espacioPadre.setObservacionHigieneSeguridad("Ventilación forzada");
        espacioPadre.setActivo(true);

        subespacio = new EspacioFisicoJpaEntity();
        subespacio.setId(UUID.randomUUID());
        subespacio.setInmueble(inmueble);
        subespacio.setEspacioPadre(espacioPadre);
        subespacio.setDenominacion("Área Microscopía");
        subespacio.setCantidad(1);
        subespacio.setCapacidadPersonasSentadas(15);
        subespacio.setSuperficieM2(new BigDecimal("40.00"));
        subespacio.setTieneSubespacios(false);
        subespacio.setObservacionHigieneSeguridad("Campana extractora OK");
        subespacio.setActivo(true);
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
    @DisplayName("Debe consultar espacios por rango de metros cuadrados (m²)")
    void consultarEspaciosPorRangoM2() {
        when(espacioFisicoRepository.consultarPorFiltros(isNull(), eq(new BigDecimal("30.00")), eq(new BigDecimal("100.00")), isNull()))
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
    }

    @Test
    @DisplayName("GET /procesos/{procesoId}/inmuebles debe mostrar el padrón general con verificado=false mientras la carrera no haya confirmado su copia")
    void obtenerInmueblesProcesoSinSnapshotMuestraTroncoPendiente() {
        UUID procesoId = UUID.randomUUID();
        inmueble.setEsMaestro(true);
        inmueble.setVerificado(true); // Incluso si en BD maestro tuviera true, debe exponer false como pendiente
        inmueble.setActivo(true);

        when(documentacionRepository.findByTipoAnexoOrderByFechaSubidaDesc(anyString())).thenReturn(Collections.emptyList());
        when(inmuebleRepository.findMaestrosActivos()).thenReturn(List.of(inmueble));

        ResponseEntity<?> resp = controller.obtenerInmueblesProceso(procesoId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<InmuebleResponse> lista = (List<InmuebleResponse>) resp.getBody();
        assertNotNull(lista);
        assertEquals(1, lista.size());
        assertTrue(lista.get(0).esMaestro());
        assertFalse(lista.get(0).verificado());
        assertEquals("CARGADO", lista.get(0).estadoConeau());
        verify(inmuebleRepository, never()).save(any());
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
