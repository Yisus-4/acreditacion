package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ElaborarPlanTrabajoCommand;
import com.undec.acreditacion.application.input.ElaborarPlanTrabajoUseCase;
import com.undec.acreditacion.application.input.ItemActividadPlanCommand;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación PURO (POJO).
 * Sin anotaciones de Spring (@Service, @Transactional) para salvaguardar la pureza hexagonal (H-02).
 */
public class ElaborarPlanTrabajoService implements ElaborarPlanTrabajoUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    public ElaborarPlanTrabajoService(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.registroRepository = Objects.requireNonNull(registroRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
    }

    @Override
    public PlanTrabajo elaborar(ElaborarPlanTrabajoCommand command) {
        if (command == null) {
            throw new DomainValidationException("El comando para elaborar plan de trabajo es obligatorio");
        }
        if (command.procesoId() == null) {
            throw new DomainValidationException("El ID del proceso de acreditación es obligatorio");
        }
        if (command.usuarioAeaId() == null) {
            throw new DomainValidationException("El usuario AEA responsable es obligatorio");
        }

        // 1. Validar que el proceso de acreditación exista y se encuentre en estado INICIADO o activo
        ProcesoAcreditacion proceso = procesoRepository.findById(command.procesoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + command.procesoId()));

        if (proceso.getEstado() != EstadoProcesoAcreditacion.INICIADO &&
            proceso.getEstado() != EstadoProcesoAcreditacion.EN_AUTOEVALUACION) {
            throw new DomainValidationException("El proceso de acreditación debe encontrarse en estado INICIADO o activo para elaborar su plan");
        }

        // 2. Recuperar y actualizar RegistroConvocatoria (RG-001) con metadatos CONEAU ampliados
        RegistroConvocatoria registro = registroRepository.findByProcesoId(command.procesoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el registro de convocatoria para el proceso ID: " + command.procesoId()));

        registro.completarDatosConeau(command.rmEstandares(), command.faseEvaluacion(), command.fechaLimiteConeau());
        registroRepository.save(registro);

        // 3. Recuperar PlanTrabajo
        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(command.procesoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de trabajo para el proceso ID: " + command.procesoId()));

        // 4. Transformar los ítems de cronograma a entidades de dominio ActividadPlanTrabajo
        List<ActividadPlanTrabajo> actividadesCronograma = new ArrayList<>();
        if (command.actividades() != null) {
            for (ItemActividadPlanCommand item : command.actividades()) {
                ActividadPlanTrabajo act = ActividadPlanTrabajo.crearActividadCronograma(
                        plan.getId(),
                        item.numeroOrden(),
                        item.codigoActividad(),
                        item.nombre(),
                        item.hoja(),
                        item.fechaInicioEstimada(),
                        item.fechaFinEstimada(),
                        item.responsable(),
                        item.prioridad()
                );
                actividadesCronograma.add(act);
            }
        }

        // 5. Configurar cronograma tentativo en el plan, validando coherencia temporal y completando ACT-02
        plan.establecerCronogramaTentativo(actividadesCronograma, command.fechaLimiteConeau(), command.usuarioAeaId());

        // 6. Persistir plan actualizado
        return planTrabajoRepository.save(plan);
    }
}
