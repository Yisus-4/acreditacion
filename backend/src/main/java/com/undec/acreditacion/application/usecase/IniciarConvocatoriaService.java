package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.IniciarConvocatoriaCommand;
import com.undec.acreditacion.application.input.IniciarConvocatoriaUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de aplicación PURO (POJO).
 * Sin anotaciones de Spring (@Service, @Transactional) para salvaguardar la pureza hexagonal (H-02).
 */
public class IniciarConvocatoriaService implements IniciarConvocatoriaUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final CarreraRepositoryPort carreraRepository;

    public IniciarConvocatoriaService(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            CarreraRepositoryPort carreraRepository) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.registroRepository = Objects.requireNonNull(registroRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
    }

    @Override
    public ProcesoAcreditacion iniciar(IniciarConvocatoriaCommand command) {
        // 1. Validar que la carrera exista y esté activa
        Carrera carrera = carreraRepository.findById(command.carreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera con ID: " + command.carreraId()));

        if (!carrera.isActivo()) {
            throw new DomainValidationException("La carrera seleccionada no se encuentra activa");
        }

        // Regla no negociable: nivel estrictamente GRADO
        if (!Carrera.NIVEL_GRADO.equalsIgnoreCase(carrera.getNivel())) {
            throw new DomainValidationException("El proceso de acreditación aplica únicamente a carreras de nivel 'GRADO'");
        }

        // 2. Verificar que no exista otro proceso activo para la misma carrera
        procesoRepository.findActivoByCarreraId(command.carreraId())
                .ifPresent(p -> {
                    throw new BusinessRuleConflictException(
                            "La carrera ya cuenta con un proceso de acreditación activo (Código: " + p.getCodigo() + ")"
                    );
                });

        // 3. Generar código y título formal de convocatoria
        int anio = command.fechaInicio().getYear();
        long correlativo = procesoRepository.countByAnio(anio) + 1;
        String sigla = carrera.getSigla() != null && !carrera.getSigla().isBlank()
                ? carrera.getSigla().trim().toUpperCase()
                : "CARR";
        String codigoConvocatoria = String.format("CONV-%d-%s-%02d", anio, sigla, correlativo);
        String tituloConvocatoria = "Convocatoria de Acreditación - " + carrera.getNombre() + " (" + anio + ")";

        // 4. Crear y persistir ProcesoAcreditacion
        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carrera.getId(),
                codigoConvocatoria,
                tituloConvocatoria,
                command.tipoProceso(),
                command.creadoPor()
        );
        ProcesoAcreditacion procesoGuardado = procesoRepository.save(proceso);

        // 5. Crear y persistir RegistroConvocatoria (RG-001)
        RegistroConvocatoria registro = RegistroConvocatoria.crear(
                procesoGuardado.getId(),
                command.nroResolucionConeau(),
                command.fechaInicio(),
                command.fechaCierre(),
                command.observaciones()
        );
        registroRepository.save(registro);

        // 6. Crear y persistir PlanTrabajo incremental con Actividad 1 en PENDIENTE
        String codigoPlan = String.format("PT-001-%s-%d", sigla, anio);
        PlanTrabajo plan = PlanTrabajo.inicializar(
                procesoGuardado.getId(),
                codigoPlan,
                TipoPlanTrabajo.PT_001_ACREDITACION
        );
        planTrabajoRepository.save(plan);

        return procesoGuardado;
    }
}
