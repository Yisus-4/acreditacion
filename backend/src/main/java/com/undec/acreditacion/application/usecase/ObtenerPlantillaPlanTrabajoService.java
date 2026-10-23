package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ObtenerPlantillaPlanTrabajoUseCase;
import com.undec.acreditacion.application.input.PlantillaActividadDto;
import com.undec.acreditacion.application.input.PlantillaPlanTrabajoDto;
import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.PrioridadActividad;
import com.undec.acreditacion.domain.entities.TipoPlanTrabajo;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de aplicación PURO (POJO).
 * Proveedor de plantillas predefinidas para planes de trabajo de GRADO (H-02).
 */
public class ObtenerPlantillaPlanTrabajoService implements ObtenerPlantillaPlanTrabajoUseCase {

    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    public ObtenerPlantillaPlanTrabajoService(PlanTrabajoRepositoryPort planTrabajoRepository) {
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
    }

    @Override
    public PlantillaPlanTrabajoDto obtenerPorProcesoId(UUID procesoId) {
        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de trabajo para el proceso ID: " + procesoId));
        return obtenerPorTipo(plan.getTipo());
    }

    @Override
    public PlantillaPlanTrabajoDto obtenerPorTipo(TipoPlanTrabajo tipoPlan) {
        if (tipoPlan == null) {
            tipoPlan = TipoPlanTrabajo.PT_001_ACREDITACION;
        }

        if (tipoPlan == TipoPlanTrabajo.PT_003_SEGUIMIENTO) {
            return construirPlantillaSeguimiento();
        }

        return construirPlantillaAcreditacion();
    }

    private PlantillaPlanTrabajoDto construirPlantillaAcreditacion() {
        List<PlantillaActividadDto> info = new ArrayList<>();
        info.add(new PlantillaActividadDto(
                3, "1.3", "Gestionar normativa de la carrera que se presenta a acreditación",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                4, "1.5", "Identificar información y documentación de interés",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                5, "1.6", "Participar del taller de la CONEAU",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                6, "3.1", "Solicitar la conformación de la CA de la carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                7, "3.2", "Formalizar la conformación de CA",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                8, "3.3", "Definir usuarios y permisos de acceso",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                9, "3.4", "Taller de capacitación sobre acreditación de carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                10, "4.1", "Gestionar información sobre infraestructura (inmuebles)",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                11, "4.2", "Gestionar información sobre Higiene y Seguridad",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                12, "4.3", "Gestionar información sobre equipamiento y centros de documentación",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                13, "4.4", "Gestionar información de los recursos bibliográficos físicos y digitales utilizados por la carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director de Carrera / Comité de Autoevaluación"
        ));
        info.add(new PlantillaActividadDto(
                14, "5.1", "Preseleccionar convenios vinculados a la carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                15, "5.2", "Seleccionar convenios vinculados a la carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Director de Carrera / Comité de Autoevaluación"
        ));
        info.add(new PlantillaActividadDto(
                16, "5.3", "Gestionar actos administrativos de convenios",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                17, "6.1", "Gestionar información sobre ámbitos de formación práctica y unidades demostrativas",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                18, "6.2", "Gestionar información de ámbitos de práctica internos de la carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director de Carrera / Comité de Autoevaluación"
        ));
        info.add(new PlantillaActividadDto(
                19, "7.1", "Gestionar información sobre estructura y organización de la UA",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        info.add(new PlantillaActividadDto(
                20, "8.2", "Gestionar información sobre estructura de gobierno y gestión de la carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director de Carrera / Comisión Curricular"
        ));
        info.add(new PlantillaActividadDto(
                21, "8.3", "Gestionar información de planes de estudio",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director de Carrera / Comisión Curricular"
        ));
        info.add(new PlantillaActividadDto(
                22, "8.4", "Gestionar programas analíticos de las asignaturas",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director de Carrera / Comisión Curricular"
        ));
        info.add(new PlantillaActividadDto(
                23, "9.1", "Gestionar cuadros con información de alumnos y graduados",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Secretaría Académica / Técnica"
        ));
        info.add(new PlantillaActividadDto(
                24, "9.2", "Gestionar información sobre actividad académica, seguimiento y evaluación de estudiantes",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                25, "10.1", "Gestionar información sobre conformación y evaluación del cuerpo académico",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                26, "10.6", "Gestionar vinculaciones docentes",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Director de Carrera / Comisión Curricular"
        ));
        info.add(new PlantillaActividadDto(
                27, "11.1", "Gestionar información sobre política institucional de investigación",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                28, "12.1", "Gestionar información sobre política institucional de vinculación con el medio",
                HojaPlanTrabajo.INFO, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                29, "13.1", "Analizar la situación actual de la carrera e identificar los déficits",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Comité de Autoevaluación / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                30, "13.2", "Organizar actividades para compartir y analizar la información (talleres)",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Comité de Autoevaluación / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                31, "13.3", "Elaborar el informe de autoevaluación de la carrera",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Comité de Autoevaluación / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                32, "13.4", "Elaborar plan de mejoras",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Comité de Autoevaluación / Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                33, "13.5", "Formalizar el Informe de Autoevaluación y el Plan de Mejoras",
                HojaPlanTrabajo.INFO, PrioridadActividad.ALTA, "Comité de Autoevaluación / Director de Carrera"
        ));

        List<PlantillaActividadDto> coneau = new ArrayList<>();
        coneau.add(new PlantillaActividadDto(
                34, "1.8", "Formalización electrónica de participación en la convocatoria",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                36, "2.1", "Creación de la solicitud de acreditación",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                37, "2.2", "Habilitar usuarios de la solicitud de acreditación",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                38, "2.3", "Recuperación de la información cargada en acreditaciones anteriores",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                39, "2.4", "Verificar información faltante de la solicitud",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                40, "2.5", "Presentación de la solicitud de acreditación",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA) / Autoridad Institucional"
        ));
        coneau.add(new PlantillaActividadDto(
                41, "4.3", "Completar el sistema web CONEAU Global ATENEA (Infraestructura)",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                42, "8.5", "Completar el sistema web CONEAU Global ATENEA (Plan de estudios y asignaturas)",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Director de Carrera / Comisión Curricular / Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                43, "9.6", "Completar el sistema web CONEAU Global ATENEA (Alumnos y graduados)",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.MEDIA, "Secretaría Académica / Técnica / Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                44, "10.4", "Completar el sistema web CONEAU Global ATENEA (Cuerpo académico)",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        coneau.add(new PlantillaActividadDto(
                45, "11.4", "Completar el sistema web CONEAU Global ATENEA (Investigación)",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        coneau.add(new PlantillaActividadDto(
                46, "12.4", "Completar el sistema web CONEAU Global ATENEA (Vinculación con el medio)",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.MEDIA, "Área de Evaluación y Acreditación (AEA) / Director de Carrera"
        ));
        coneau.add(new PlantillaActividadDto(
                47, "13.5", "Completar el sistema web CONEAU Global ATENEA (Autoevaluación y Planes de Mejora)",
                HojaPlanTrabajo.CONEAU, PrioridadActividad.ALTA, "Comité de Autoevaluación / Director de Carrera / Área de Evaluación y Acreditación (AEA)"
        ));

        return new PlantillaPlanTrabajoDto(TipoPlanTrabajo.PT_001_ACREDITACION, info, coneau);
    }

    private PlantillaPlanTrabajoDto construirPlantillaSeguimiento() {
        List<PlantillaActividadDto> info = new ArrayList<>();
        info.add(new PlantillaActividadDto(
                3,
                "ACT-03-SEG-COMPROMISOS",
                "Relevamiento y diagnóstico de compromisos de mejora",
                HojaPlanTrabajo.INFO,
                PrioridadActividad.ALTA,
                "Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                4,
                "ACT-04-SEG-DOCENTES",
                "Monitoreo del cuerpo docente y dedicaciones",
                HojaPlanTrabajo.INFO,
                PrioridadActividad.MEDIA,
                "Director de Carrera"
        ));
        info.add(new PlantillaActividadDto(
                5,
                "ACT-05-SEG-INFRA",
                "Verificación de mejoras en infraestructura y equipamiento",
                HojaPlanTrabajo.INFO,
                PrioridadActividad.MEDIA,
                "Secretaría Académica"
        ));
        info.add(new PlantillaActividadDto(
                6,
                "ACT-06-SEG-INFORME",
                "Redacción del informe de seguimiento y planes de mejora",
                HojaPlanTrabajo.INFO,
                PrioridadActividad.ALTA,
                "Comisión de Seguimiento / Área de Evaluación y Acreditación (AEA)"
        ));

        List<PlantillaActividadDto> coneau = new ArrayList<>();
        coneau.add(new PlantillaActividadDto(
                7,
                "ACT-07-SEG-CARGA-COMP",
                "Carga de compromisos cumplidos en CONEAU Global",
                HojaPlanTrabajo.CONEAU,
                PrioridadActividad.ALTA,
                "Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                8,
                "ACT-08-SEG-ACT-FICHAS",
                "Actualización de fichas docentes y curriculares",
                HojaPlanTrabajo.CONEAU,
                PrioridadActividad.MEDIA,
                "Comisión de Seguimiento / Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                9,
                "ACT-09-SEG-CARGA-INF",
                "Carga de informe de seguimiento y probanzas",
                HojaPlanTrabajo.CONEAU,
                PrioridadActividad.ALTA,
                "Comisión de Seguimiento / Área de Evaluación y Acreditación (AEA)"
        ));
        coneau.add(new PlantillaActividadDto(
                10,
                "ACT-10-SEG-FORMALIZAR",
                "Formalización y presentación de seguimiento ante CONEAU",
                HojaPlanTrabajo.CONEAU,
                PrioridadActividad.ALTA,
                "Autoridad Institucional / Área de Evaluación y Acreditación (AEA)"
        ));

        return new PlantillaPlanTrabajoDto(TipoPlanTrabajo.PT_003_SEGUIMIENTO, info, coneau);
    }
}
