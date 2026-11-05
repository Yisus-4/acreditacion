import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SubnavAutoevaluacionComponent } from '../subnav-autoevaluacion/subnav-autoevaluacion.component';

interface ActividadDimension {
  nombre: string;
  responsable: string;
  instrumento: string;
  estado: string;
  dependencia?: string;
}

interface InfoDimension {
  titulo: string;
  descripcion: string;
  alertaRequisito?: { titulo: string; mensaje: string };
  actividades: ActividadDimension[];
}

@Component({
  selector: 'app-dimension-detalle',
  standalone: true,
  imports: [SubnavAutoevaluacionComponent, RouterLink],
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
        { nombre: 'Gestionar información sobre ámbitos de formación práctica y unidades demostrativas', responsable: 'Comisión de Carrera / AEA', instrumento: 'Formulario CONEAU (RG-006)', estado: 'Pendiente' },
        { nombre: 'Gestionar información de ámbitos de práctica internos de la carrera', responsable: 'Director de Carrera', instrumento: 'Registro de Laboratorios', estado: 'Pendiente', dependencia: 'Requiere Ámbitos maestro (RG-006)' },
        { nombre: 'Gestionar información de ámbitos de práctica externos de la carrera', responsable: 'Secretaría de Extensión / Carrera', instrumento: 'Convenios de Práctica', estado: 'Pendiente', dependencia: 'Requiere Ámbitos maestro (RG-006)' },
        { nombre: 'Gestionar información de unidades demostrativas de la carrera', responsable: 'Área Académica', instrumento: 'Ficha Técnica', estado: 'Pendiente', dependencia: 'Requiere Ámbitos maestro (RG-006)' },
        { nombre: 'Gestionar información de centros de atención médica (CAM) de la carrera', responsable: 'Dirección CAM / Escuela de Salud', instrumento: 'Habilitación Sanitaria', estado: 'Pendiente', dependencia: 'Requiere Ámbitos maestro (RG-006)' },
      ],
    },
    'organizacion-ua': {
      titulo: 'Organización, coordinación y gestión de la UA',
      descripcion: 'Estructura orgánico-funcional de la Unidad Académica, sistemas de procesamiento y perfeccionamiento no-docente.',
      actividades: [
        { nombre: 'Gestionar información sobre estructura, organización gestión de la UA', responsable: 'Decanato / Dirección de Escuela', instrumento: 'Organigrama y Resoluciones (GT-023)', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre política de perfeccionamiento del personal de apoyo no-docente', responsable: 'Recursos Humanos / UA', instrumento: 'Plan de Capacitación (GT-024)', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre sistemas de registro y procesamiento de la UA', responsable: 'Área de Informática y Sistemas', instrumento: 'Sistemas SIU / Gestión (GT-004)', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre educación a distancia de la UA', responsable: 'Área de Educación a Distancia', instrumento: 'SIED Institucional (GT-005)', estado: 'Pendiente' },
      ],
    },
    'estructura-curricular': {
      titulo: 'Estructura curricular, organización y gestión de la carrera',
      descripcion: 'Planes de estudio, programas analíticos, comisiones de seguimiento curricular y régimen de correlatividades.',
      alertaRequisito: {
        titulo: 'Requisitos de Secuenciamiento PRG-002',
        mensaje: 'La gestión de información de las asignaturas (FT-003) requiere como insumos previos la verificación bibliográfica de biblioteca (RG-014 / Actividad 4.4) y los cuadros de estudiantes y graduados (Actividad 9.1).',
      },
      actividades: [
        { nombre: 'Gestionar información sobre mecanismos o instancias de gestión y seguimiento relacionadas a la carrera', responsable: 'Director de Carrera / Comisión Curricular', instrumento: 'Actas de Seguimiento (GT-007A)', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre organización y gestión de la carrera', responsable: 'Dirección de Carrera', instrumento: 'Manual de Procedimientos (GT-001)', estado: 'Pendiente' },
        { nombre: 'Gestionar información de planes de estudio', responsable: 'Secretaría Académica', instrumento: 'Resolución Ministerial / Plan (FT-001)', estado: 'Pendiente' },
        { nombre: 'Gestionar programas analíticos de las asignaturas', responsable: 'Docentes Titulares / Carrera', instrumento: 'Programas Vigentes en PDF', estado: 'Pendiente' },
        { nombre: 'Gestionar información de las asignaturas', responsable: 'Secretaría Académica / Docentes', instrumento: 'Fichas FT-003', estado: 'Pendiente', dependencia: 'Requiere Biblioteca RG-014 y Cuadros SGA' },
        { nombre: 'Gestionar información sobre comisiones de planificación y seguimiento de la carrera', responsable: 'Comisión de Seguimiento Curricular', instrumento: 'Resolución de Designación (GT-006)', estado: 'Pendiente' },
      ],
    },
    'estudiantes-graduados': {
      titulo: 'Estudiantes y graduados',
      descripcion: 'Datos de matrícula, rendimiento académico, desgranamiento, programas de tutoría, becas y vinculación con graduados.',
      actividades: [
        { nombre: 'Gestionar cuadros con información de estudiantes y graduados', responsable: 'Dirección de Alumnos / Informática', instrumento: 'Reportes SIU Guaraní', estado: 'Pendiente', dependencia: 'Requiere Fichas curriculares generadas (FT-001)' },
        { nombre: 'Gestionar información sobre actividad académica, seguimiento y evaluación de los estudiantes', responsable: 'Secretaría Académica', instrumento: 'Régimen de Correlatividades y Promoción (GT-012)', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre seguimiento y perfeccionamiento de graduados', responsable: 'Área de Graduados / Carrera', instrumento: 'Encuestas y Cursos Posgrado (GT-015)', estado: 'Pendiente', dependencia: 'Requiere Registro de Capacitaciones RG-011' },
        { nombre: 'Gestionar información sobre instancias de apoyo a estudiantes (Becas)', responsable: 'Bienestar Universitario', instrumento: 'Programa de Becas (RG-015 / GT-014)', estado: 'Pendiente' },
        { nombre: 'Gestionar información sobre instancias de apoyo a estudiantes (tutorías)', responsable: 'Coordinación de Tutorías', instrumento: 'Plan de Acompañamiento (GT-007B)', estado: 'Pendiente' },
      ],
    },
    'cuerpo-academico': {
      titulo: 'Conformación y evaluación del cuerpo académico',
      descripcion: 'Planta docente, categorizaciones de investigación, vinculaciones y antecedentes curriculares CVAr.',
      actividades: [
        { nombre: 'Gestionar información sobre conformación y evaluación del cuerpo académico', responsable: 'Secretaría Académica / Concursos', instrumento: 'Planta Docente (RG-010)', estado: 'Pendiente' },
        { nombre: 'Gestionar vinculaciones docentes', responsable: 'Dirección de Carrera', instrumento: 'Convenios e Importación CVar', estado: 'Pendiente', dependencia: 'Requiere Conformación cuerpo docente (RG-010)' },
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
    const info = this.datosDimension();
    const texto = this.filtroTexto().trim().toLowerCase();
    if (!texto) {
      return info.actividades;
    }
    return info.actividades.filter(
      (a) =>
        a.nombre.toLowerCase().includes(texto) ||
        a.responsable.toLowerCase().includes(texto) ||
        a.instrumento.toLowerCase().includes(texto)
    );
  });

  iniciarActividad(nombreActividad: string): void {
    alert(`Iniciando relevamiento operativo para: ${nombreActividad}\nEsta función conectará directamente con el módulo de carga de evidencia.`);
  }
}
