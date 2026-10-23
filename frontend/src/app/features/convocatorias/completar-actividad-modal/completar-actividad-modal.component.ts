import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import {
  ActividadPlanTrabajoDto,
  AREAS_INFORMADAS_CATALOGO,
  OpcionCatalogoProceso,
  TipoDocumento,
  TipoPlanTrabajo,
  TipoProceso,
} from '../../../core/convocatorias/models/convocatoria.model';

@Component({
  selector: 'app-completar-actividad-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './completar-actividad-modal.component.html',
  styleUrls: ['./completar-actividad-modal.component.scss'],
})
export class CompletarActividadModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly convocatoriaService = inject(ConvocatoriaService);

  @Input({ required: true }) actividadId!: string;
  @Input({ required: true }) actividadNombre!: string;
  @Input() tipoProceso: TipoProceso = 'EN_FUNCIONAMIENTO';
  @Input() tipoPlan: TipoPlanTrabajo = 'PT_001_ACREDITACION';
  @Input() actividad?: ActividadPlanTrabajoDto | null;
  @Output() cerrado = new EventEmitter<boolean>();

  readonly today: string = this.obtenerFechaHoyLocal();
  readonly areasCatalogo: OpcionCatalogoProceso[] = AREAS_INFORMADAS_CATALOGO;
  readonly areasSeleccionadas = signal<string[]>([]);
  readonly docsSeleccionados = signal<string[]>([]);
  readonly areasTouched = signal<boolean>(false);
  readonly docsTouched = signal<boolean>(false);

  form: FormGroup = this.fb.group({
    fechaReunion: [
      this.today,
      [Validators.required, (control: AbstractControl): ValidationErrors | null => this.validarFechaNoAnteriorAHoy(control)],
    ],
    observaciones: [''],
    tipoDocumento: ['ACTA_REUNION_COORDINACION'],
  });

  private obtenerFechaHoyLocal(): string {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private validarFechaNoAnteriorAHoy(control: AbstractControl): ValidationErrors | null {
    const val = (control.value || '').trim();
    if (!val) return null;
    if (val < this.today) {
      return { fechaAnteriorAHoy: true };
    }
    return null;
  }

  selectedFile: File | null = null;
  readonly fileError = signal<string | null>(null);
  readonly submitting = signal<boolean>(false);
  readonly generalError = signal<string | null>(null);

  get opcionesDocumentacion(): OpcionCatalogoProceso[] {
    if (this.tipoPlan === 'PT_003_SEGUIMIENTO') {
      return [
        {
          codigo: 'ORDENANZAS_Y_NOTAS_TECNICAS',
          etiqueta: 'Ordenanzas que fijan los procedimientos involucrados y notas técnicas',
        },
        {
          codigo: 'RESOLUCION_ACREDITACION_COMPROMISOS',
          etiqueta: 'Resolución de acreditación (o dictamen) con compromisos y recomendaciones',
        },
        {
          codigo: 'RESOLUCIONES_INFORMES_SEGUIMIENTO',
          etiqueta: 'Resoluciones e informes de seguimiento de avance previos',
        },
        {
          codigo: 'PLANES_MEJORA_CONEAU',
          etiqueta: 'Planes de mejora presentados ante CONEAU (última versión)',
        },
      ];
    }

    if (this.tipoProceso === 'NUEVA') {
      return [
        {
          codigo: 'RM_ESTANDARES_CARRERA',
          etiqueta: 'Resolución Ministerial que fija los estándares de la carrera',
        },
        {
          codigo: 'RM_INCLUSION_ART_43_LES',
          etiqueta: 'Resolución Ministerial de inclusión en el art. 43 de la LES',
        },
        {
          codigo: 'ORDENANZAS_Y_NOTAS_TECNICAS',
          etiqueta: 'Ordenanzas de procedimientos y notas técnicas',
        },
      ];
    }

    return [
      {
        codigo: 'RESOLUCION_CONVOCATORIA_CONEAU',
        etiqueta: 'Resolución de convocatoria pública de CONEAU',
      },
      {
        codigo: 'RM_ESTANDARES_CARRERA',
        etiqueta: 'Resolución Ministerial que fija los estándares de la carrera',
      },
      {
        codigo: 'RM_INCLUSION_ART_43_LES',
        etiqueta: 'Resolución Ministerial de inclusión en el art. 43 de la LES',
      },
      {
        codigo: 'ORDENANZAS_Y_NOTAS_TECNICAS',
        etiqueta: 'Ordenanzas que fijan los procedimientos involucrados y notas técnicas',
      },
    ];
  }

  get etiquetaCasoDocumentacion(): string {
    if (this.tipoPlan === 'PT_003_SEGUIMIENTO') {
      return 'Seguimiento de avance de Planes de Mejora (PT-003)';
    }
    if (this.tipoProceso === 'NUEVA') {
      return 'Carrera Nueva (Grado)';
    }
    return 'Carrera en Funcionamiento — 1era o 2da Fase (Grado)';
  }

  ngOnInit(): void {
    const todasLasAreas = this.areasCatalogo.map((a) => a.codigo);
    const todosLosDocs = this.opcionesDocumentacion.map((o) => o.codigo);
    const act = this.actividad;

    if (act) {
      if (act.areasInformadas && act.areasInformadas.length > 0) {
        this.areasSeleccionadas.set([...act.areasInformadas]);
      } else {
        this.areasSeleccionadas.set(todasLasAreas);
      }

      if (act.documentacionRemitida && act.documentacionRemitida.length > 0) {
        const normalizados = act.documentacionRemitida.map((c) => this.mapearCodigoDocModal(c));
        this.docsSeleccionados.set(normalizados);
      } else {
        this.docsSeleccionados.set(todosLosDocs);
      }

      if (act.fechaReunionCoordinacion) {
        const d = new Date(act.fechaReunionCoordinacion);
        if (!isNaN(d.getTime())) {
          const yyyy = d.getFullYear();
          const mm = String(d.getMonth() + 1).padStart(2, '0');
          const dd = String(d.getDate()).padStart(2, '0');
          const fechaGuardada = `${yyyy}-${mm}-${dd}`;
          this.form.patchValue({
            fechaReunion: fechaGuardada >= this.today ? fechaGuardada : this.today,
          });
        }
      }

      const obs = act.observaciones ?? act.sintesisAcuerdos;
      if (obs) {
        this.form.patchValue({ observaciones: obs });
      }
    } else {
      // Preselección completa por defecto de todas las áreas y documentación
      this.areasSeleccionadas.set(todasLasAreas);
      this.docsSeleccionados.set(todosLosDocs);
    }
  }

  private mapearCodigoDocModal(codigo: string): string {
    switch (codigo) {
      case 'RM_ESTANDARES':
        return 'RM_ESTANDARES_CARRERA';
      case 'ORDENANZAS_NOTAS_TECNICAS':
        return 'ORDENANZAS_Y_NOTAS_TECNICAS';
      case 'RESOLUCIONES_INFORME_SEGUIMIENTO':
        return 'RESOLUCIONES_INFORMES_SEGUIMIENTO';
      case 'PLANES_MEJORA_PRESENTADOS':
        return 'PLANES_MEJORA_CONEAU';
      default:
        return codigo;
    }
  }

  isAreaSelected(codigo: string): boolean {
    return this.areasSeleccionadas().includes(codigo);
  }

  toggleArea(codigo: string): void {
    this.areasTouched.set(true);
    this.areasSeleccionadas.update((actual) =>
      actual.includes(codigo) ? actual.filter((c) => c !== codigo) : [...actual, codigo]
    );
  }

  isDocSelected(codigo: string): boolean {
    return this.docsSeleccionados().includes(codigo);
  }

  toggleDoc(codigo: string): void {
    this.docsTouched.set(true);
    this.docsSeleccionados.update((actual) =>
      actual.includes(codigo) ? actual.filter((c) => c !== codigo) : [...actual, codigo]
    );
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fileError.set(null);

    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
      if (!isPdf) {
        this.fileError.set('Solo se admiten documentos de respaldo en formato PDF.');
        this.selectedFile = null;
        input.value = '';
        return;
      }

      const maxSize = 20 * 1024 * 1024;
      if (file.size > maxSize) {
        this.fileError.set('El archivo seleccionado supera el límite institucional de 20 MB.');
        this.selectedFile = null;
        input.value = '';
        return;
      }

      this.selectedFile = file;
    } else {
      this.selectedFile = null;
    }
  }

  quitarArchivo(inputEl: HTMLInputElement): void {
    this.selectedFile = null;
    this.fileError.set(null);
    inputEl.value = '';
  }

  get fechaReunionPreview(): string | null {
    const raw = this.form.get('fechaReunion')?.value;
    if (!raw) return null;
    // Si viene como YYYY-MM-DD desde input[type="date"]
    if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
      const [yyyy, mm, dd] = raw.split('-');
      return `${dd}-${mm}-${yyyy}`;
    }
    const d = new Date(raw);
    if (isNaN(d.getTime())) return null;
    const dia = String(d.getDate()).padStart(2, '0');
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const anio = d.getFullYear();
    return `${dia}-${mes}-${anio}`;
  }

  onSubmit(): void {
    this.areasTouched.set(true);
    this.docsTouched.set(true);

    if (this.form.invalid || this.areasSeleccionadas().length === 0 || this.docsSeleccionados().length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.fileError()) {
      return;
    }

    this.submitting.set(true);
    this.generalError.set(null);

    const rawFecha = (this.form.get('fechaReunion')?.value || '').trim();
    const fechaFormateada = this.fechaReunionPreview || rawFecha;
    const observacionesTexto = (this.form.get('observaciones')?.value || '').trim();
    const tipoDoc = (this.form.get('tipoDocumento')?.value || 'ACTA_REUNION_COORDINACION') as TipoDocumento;

    this.convocatoriaService
      .completarActividad(this.actividadId, {
        fechaReunion: fechaFormateada,
        areasInformadas: this.areasSeleccionadas(),
        documentacionRemitida: this.docsSeleccionados(),
        observaciones: observacionesTexto || undefined,
        sintesisAcuerdos: observacionesTexto || undefined,
        archivo: this.selectedFile,
        tipoDocumento: this.selectedFile ? tipoDoc : undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.cerrado.emit(true);
        },
        error: (err) => {
          this.submitting.set(false);
          this.generalError.set(
            err?.error?.error || 'Ocurrió un error al registrar la comunicación de la actividad.'
          );
        },
      });
  }

  cancelar(): void {
    this.cerrado.emit(false);
  }
}
