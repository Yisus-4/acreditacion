package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.output.CatalogoMiembroComisionDto;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.CatalogoMiembroComisionRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoMiembroComisionUseCasesTest {

    @Mock
    private CatalogoMiembroComisionRepositoryPort catalogoRepository;
    @Mock
    private RegistroConformacionCaRepositoryPort registroCaRepository;
    @Mock
    private ProcesoAcreditacionRepositoryPort procesoRepository;

    private ConsultarCatalogoMiembrosCaService consultarService;
    private ConfirmarRegistroMiembroCaService confirmarService;
    private CancelarRegistroMiembroCaService cancelarService;

    private UUID procesoId;
    private UUID miembroId;
    private UUID usuarioId;
    private ProcesoAcreditacion proceso;

    @BeforeEach
    void setUp() {
        consultarService = new ConsultarCatalogoMiembrosCaService(catalogoRepository);
        confirmarService = new ConfirmarRegistroMiembroCaService(registroCaRepository, procesoRepository, catalogoRepository);
        cancelarService = new CancelarRegistroMiembroCaService(registroCaRepository, procesoRepository);

        procesoId = UUID.randomUUID();
        miembroId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();

        proceso = ProcesoAcreditacion.iniciar(
                UUID.randomUUID(), "CONV-2026", "Acreditación Test",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, usuarioId
        );
    }

    @Test
    @DisplayName("Consultar catálogo busca por término")
    void consultarCatalogo() {
        CatalogoMiembroComision item = new CatalogoMiembroComision(
                UUID.randomUUID(), "Salerno", "Julio Luis", "8.386.710", "20-08386710-9",
                "jsalerno@undec.edu.ar", "salernojulioluis@gmail.com", "Director de Carrera",
                Instant.now()
        );
        when(catalogoRepository.buscar("Salerno")).thenReturn(List.of(item));

        List<CatalogoMiembroComisionDto> res = consultarService.buscar("Salerno");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).apellido()).isEqualTo("Salerno");
        assertThat(res.get(0).nombreCompleto()).isEqualTo("Julio Luis");
        assertThat(res.get(0).cuitPasaporte()).isEqualTo("20-08386710-9");
    }

    @Test
    @DisplayName("Confirmar registro persiste en catálogo y actualiza miembro en CA")
    void confirmarRegistro() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);
        MiembroComisionAutoevaluacion m = new MiembroComisionAutoevaluacion(
                miembroId, null, "Albagli", "Karen", "Janet", "35.850.007",
                "27-35850007-8", "kalbagli91@gmail.com", "albaagrimensura@gmail.com",
                "Docente", null, RolClaustroCa.DOCENTE, true, 1
        );
        ca.guardarBorrador(true, List.of(m));

        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));
        when(catalogoRepository.findByDocumento("35.850.007")).thenReturn(Optional.empty());
        when(catalogoRepository.findByCuitPasaporte("27-35850007-8")).thenReturn(Optional.empty());
        when(registroCaRepository.save(any(RegistroConformacionCa.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroCaDto res = confirmarService.confirmarRegistro(procesoId, miembroId, usuarioId);

        assertThat(res.miembros().get(0).pendienteAltaSistema()).isFalse();

        ArgumentCaptor<CatalogoMiembroComision> captor = ArgumentCaptor.forClass(CatalogoMiembroComision.class);
        verify(catalogoRepository).save(captor.capture());
        CatalogoMiembroComision saved = captor.getValue();
        assertThat(saved.getApellido()).isEqualTo("Albagli");
        assertThat(saved.getNombreCompleto()).isEqualTo("Karen Janet");
        assertThat(saved.getDocumento()).isEqualTo("35.850.007");
        assertThat(saved.getCuitPasaporte()).isEqualTo("27-35850007-8");
    }

    @Test
    @DisplayName("Cancelar registro marca pendienteAltaSistema como false sin persistir en catálogo")
    void cancelarRegistro() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);
        MiembroComisionAutoevaluacion m = new MiembroComisionAutoevaluacion(
                miembroId, null, "Perez", "Juan", null, "12345678",
                "20-12345678-9", "juan@undec.edu.ar", "juan@gmail.com",
                "Docente", null, RolClaustroCa.DOCENTE, true, 1
        );
        ca.guardarBorrador(true, List.of(m));

        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));
        when(registroCaRepository.save(any(RegistroConformacionCa.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroCaDto res = cancelarService.cancelarRegistro(procesoId, miembroId, usuarioId);

        assertThat(res.miembros().get(0).pendienteAltaSistema()).isFalse();
        verifyNoInteractions(catalogoRepository);
    }
}
