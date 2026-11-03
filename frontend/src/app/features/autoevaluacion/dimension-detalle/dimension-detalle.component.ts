import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SubnavAutoevaluacionComponent } from '../subnav-autoevaluacion/subnav-autoevaluacion.component';

interface ActividadDimension {
  nombre: string;
  responsable: string;
  instrumento: string;
  estado: string;
}

interface InfoDimension {
  titulo: string;
  descripcion: string;
  actividades: ActividadDimension[];
}

@Component({
  selector: 'app-dimension-detalle',
  standalone: true,
  imports: [SubnavAutoevaluacionComponent],
  templateUrl: './dimension-detalle.component.html',
  styleUrls: ['../infraestructura/infraestructura.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DimensionDetalleComponent {
  private readonly route = inject(ActivatedRoute);

  readonly filtroTexto = signal<string>('');
  readonly dimensionKey = signal<string>('');

  private readonly dimensionesConfig: Record<string, InfoDimension> = {
    'ambitos-formacion': {
      titulo: 'Ámbitos de formación teórico-práctica',
      descripcion: 'Relevamiento de unidades demostrativas, centros de práctica internos y externos, y centros de atención médica (CAM).',
      actividades: [
        { nombre: 'Gestionar información sobre ámbitos de formación práctica y unidades demostrativas', responsable: 'Comisión de Carrera / AEA', instrumento: 'Formulario CONEAU', estado: 'Pendiente' },
        { nombre: 'Gestionar información de ámbitos de práctica internos de la carrera', responsable: 'Director de Carrera', instrumento: 'Registro de Laboratorios', estado: 'Pendiente' },
        { nombre: 'Gestionar información de ámbitos de práctica externos de la carrera', responsable: 'Secretaría de Extensión / Carrera', instrumento: 'Convenios de Práctica', estado: 'Pendiente' },
        { nombre: 'Gestionar información de unidades demostrativas de la carrera', responsable: 'Área Académica', instrumento: 'Ficha Técnica', estado: 'Pendiente' },
        { nombre: 'Gestionar información de centros de atención médica (CAM) de la carrera', responsable: 'Dirección CAM / Escuela de Salud', instrumento: 'Habilitación Sanitaria', estado: 'Pendiente' },
      ],
    },
    'organizacion-ua': {
      titulo: 'Organización, coordinación y gestión de la UA',
      descripcion: 'Estructura orgánico-funcional de la Unidad Académica, sistemas de procesamiento y perfeccionamiento no-docente.',
      actividades: [
        { nombre: 'Gestionar información sobre estructura, organización gestión de la UA', responsable: 'Decanato / Dirección de Escuela', instrumento: 'Organigrama y Resoluciones', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre política de perfeccionamiento del personal de apoyo no-docente', responsable: 'Recursos Humanos / UA', instrumento: 'Plan de Capacitación', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre sistemas de registro y procesamiento de la UA', responsable: 'Área de Informática y Sistemas', instrumento: 'Sistemas SIU / Gestión', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre educación a distancia de la UA', responsable: 'Área de Educación a Distancia', instrumento: 'SIED Institucional', estado: 'Pendiente' },
      ],
    },
    'estructura-curricular': {
      titulo: 'Estructura curricular, organización y gestión de la carrera',
      descripcion: 'Planes de estudio, programas analíticos, comisiones de seguimiento curricular y régimen de correlatividades.',
      actividades: [
        { nombre: 'Gestionar información sobre mecanismos o instancias de gestión y seguimiento relacionadas a la carrera', responsable: 'Director de Carrera / Comisión Curricular', instrumento: 'Actas de Seguimiento', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre organización y gestión de la carrera', responsable: 'Dirección de Carrera', instrumento: 'Manual de Procedimientos', estado: 'Pendiente' },
        { nombre: 'Gestionar información de planes de estudio', responsable: 'Secretaría Académica', instrumento: 'Resolución Ministerial / Plan', estado: 'Pendiente' },
        { nombre: 'Gestionar programas analíticos de las asignaturas', responsable: 'Docentes Titulares / Carrera', instrumento: 'Programas Vigentes', estado: 'Pendiente' },
        { nombre: 'Gestionar información de las asignaturas', responsable: 'Secretaría Académica', instrumento: 'Cargas Horarias y Contenidos', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre comisiones de planificación y seguimiento de la carrera', responsable: 'Comisión de Seguimiento Curricular', instrumento: 'Resolución de Designación', estado: 'Pendiente' },
      ],
    },
    'estudiantes-graduados': {
      titulo: 'Estudiantes y graduados',
      descripcion: 'Datos de matrícula, rendimiento académico, desgranamiento, programas de tutoría, becas y vinculación con graduados.',
      actividades: [
        { nombre: 'Gestionar cuadros con información de estudiantes y graduados', responsable: 'Dirección de Alumnos / Informática', instrumento: 'Reportes SIU Guaraní', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre actividad académica, seguimiento y evaluación de los estudiantes', responsable: 'Secretaría Académica', instrumento: 'Régimen de Correlatividades y Promoción', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre seguimiento y perfeccionamiento de graduados', responsable: 'Área de Graduados / Carrera', instrumento: 'Encuestas y Cursos Posgrado', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre instancias de apoyo a estudiantes (Becas)', responsable: 'Bienestar Universitario', instrumento: 'Programa de Becas', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre instancias de apoyo a estudiantes (tutorías)', responsable: 'Coordinación de Tutorías', instrumento: 'Plan de Acompañamiento', estado: 'Pendiente' },
      ],
    },
    'cuerpo-academico': {
      titulo: 'Conformación y evaluación del cuerpo académico',
      descripcion: 'Planta docente, categorizaciones de investigación, vinculaciones y antecedentes curriculares CVAr.',
      actividades: [
        { nombre: 'Gestionar información sobre conformación y evaluación del cuerpo académico', responsable: 'Secretaría Académica / Concursos', instrumento: 'Planta Docente y Evaluaciones', estado: 'Pendiente' },
        { nombre: 'Gestionar vinculaciones docentes', responsable: 'Dirección de Carrera', instrumento: 'Convenios Interinstitucionales', estado: 'Pendiente' },
      ],
    },
  };

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const key = params.get('dimension') || 'ambitos-formacion';
      this.dimensionKey.set(key);
    });
  }

  readonly datosDimension = computed<InfoDimension>(() => {
    const key = this.dimensionKey();
    return this.dimensionesConfig[key] || this.dimensionesConfig['ambitos-formacion'];
  });

  readonly actividadesFiltradas = computed(() => {
    const list = this.datosDimension().actividades;
    const txt = this.filtroTexto().trim().toLowerCase();
    if (!txt) return list;
    return list.filter((a) => a.nombre.toLowerCase().includes(txt) || a.responsable.toLowerCase().includes(txt));
  });

  iniciarActividad(nombre: string): void {
    alert(`Iniciando formulario de gestión para: "${nombre}".`);
  }
}
