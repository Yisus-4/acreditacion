import { Component, computed, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import {
  ConvocatoriaDetalle,
  ElaborarPlanTrabajoRequest,
  FaseEvaluacion,
  HojaPlanTrabajo,
  ItemActividadPlanRequest,
  PlanTrabajoDto,
  PlantillaActividadDto,
  PrioridadActividad,
} from '../../../core/convocatorias/models/convocatoria.model';

@Component({
  selector: 'app-elaborar-plan-trabajo-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './elaborar-plan-trabajo-modal.component.html',
  styleUrls: ['./elaborar-plan-trabajo-modal.component.scss'],
})
export class ElaborarPlanTrabajoModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly convocatoriaService = inject(ConvocatoriaService);

  @Input({ required: true }) procesoId!: string;
  @Input({ required: true }) convocatoria!: ConvocatoriaDetalle;
  @Input() planTrabajo?: PlanTrabajoDto | null;

  @Output() guardado = new EventEmitter<void>();
  @Output() cerrado = new EventEmitter<void>();

  readonly filtroHoja = signal<'TODAS' | 'INFO' | 'CONEAU'>('TODAS');
  readonly loading = signal<boolean>(false);
  readonly submitting = signal<boolean>(false);
  readonly generalError = signal<string | null>(null);
  readonly validationErrors = signal<string[]>([]);
  readonly dropdownAbiertoIndex = signal<number | null>(null);

  readonly rolesDisponibles: string[] = [
    'Área de Evaluación y Acreditación (AEA)',
    'Director de Carrera',
    'Comité de Autoevaluación',
    'Comisión Curricular',
    'Coordinador de Acreditación',
    'Docentes / Proveedores de Información',
    'Secretaría Académica / Técnica',
    'Dirección de Escuela',
    'Autoridad Institucional',
  ];

  toggleDropdown(index: number): void {
    this.dropdownAbiertoIndex.update((curr) => (curr === index ? null : index));
  }

  isRolSelected(ctrl: FormGroup, rol: string): boolean {
    const val = (ctrl.get('responsable')?.value || '').trim();
    const roles = val.split('/').map((s: string) => s.trim()).filter(Boolean);
    return roles.includes(rol);
  }

  toggleRol(ctrl: FormGroup, rol: string): void {
    const val = (ctrl.get('responsable')?.value || '').trim();
    let roles = val ? val.split('/').map((s: string) => s.trim()).filter(Boolean) : [];
    if (roles.includes(rol)) {
      roles = roles.filter((r: string) => r !== rol);
    } else {
      roles.push(rol);
    }
    ctrl.get('responsable')?.setValue(roles.join(' / '));
    ctrl.get('responsable')?.markAsDirty();
  }

  readonly form: FormGroup = this.fb.group({
    rmEstandares: [''],
    faseEvaluacion: ['PRIMERA_FASE', Validators.required],
    fechaLimitePresentacionConeau: ['', Validators.required],
    actividades: this.fb.array([]),
  });

  get actividadesArray(): FormArray {
    return this.form.get('actividades') as FormArray;
  }

  readonly conteoInfo = computed(() => {
    return this.actividadesArray.controls.filter(
      (c) => c.get('hoja')?.value === 'INFO'
    ).length;
  });

  readonly conteoConeau = computed(() => {
    return this.actividadesArray.controls.filter(
      (c) => c.get('hoja')?.value === 'CONEAU'
    ).length;
  });

  readonly conteoTotal = computed(() => {
    return this.actividadesArray.length;
  });

  ngOnInit(): void {
    this.inicializarMetadatos();
    this.cargarActividades();
  }

  private inicializarMetadatos(): void {
    const reg = this.convocatoria.registroConvocatoria;
    if (reg) {
      if (reg.rmEstandares) {
        this.form.patchValue({ rmEstandares: reg.rmEstandares });
      }
      if (reg.faseEvaluacion) {
        this.form.patchValue({ faseEvaluacion: reg.faseEvaluacion });
      }
      if (reg.fechaLimitePresentacionConeau) {
        this.form.patchValue({
          fechaLimitePresentacionConeau: reg.fechaLimitePresentacionConeau,
        });
      } else if (reg.fechaCierre) {
        this.form.patchValue({
          fechaLimitePresentacionConeau: reg.fechaCierre,
        });
      }
    }
  }

  private cargarActividades(): void {
    // Si ya existen actividades cargadas en el plan con orden >= 3, utilizarlas
    const actividadesExistentes = this.planTrabajo?.actividades?.filter(
      (a) => a.numeroOrden >= 3
    );

    if (actividadesExistentes && actividadesExistentes.length > 0) {
      this.poblarActividadesDesdeExistentes(actividadesExistentes);
      return;
    }

    // De lo contrario, solicitar la plantilla oficial sugerida para este proceso
    this.cargarPlantillaSugerida();
  }

  cargarPlantillaSugerida(): void {
    this.loading.set(true);
    this.generalError.set(null);

    this.convocatoriaService.getPlantillaPlanTrabajo(this.procesoId).subscribe({
      next: (plantilla) => {
        const items: PlantillaActividadDto[] =
          plantilla.actividades ?? [
            ...(plantilla.actividadesInfo ?? []),
            ...(plantilla.actividadesConeau ?? []),
          ];

        this.actividadesArray.clear();

        const fechaBaseInicio =
          this.convocatoria.registroConvocatoria?.fechaInicio ||
          new Date().toISOString().substring(0, 10);
        const fechaBaseFin =
          this.form.get('fechaLimitePresentacionConeau')?.value ||
          this.convocatoria.registroConvocatoria?.fechaCierre ||
          fechaBaseInicio;

        items.forEach((item, index) => {
          const orden = item.numeroOrden ?? item.ordenSugerido ?? index + 3;
          this.actividadesArray.push(
            this.crearGrupoActividad({
              numeroOrden: orden,
              codigoActividad: item.codigoActividad,
              nombre: item.nombre,
              hoja: item.hoja,
              fechaInicioEstimada: fechaBaseInicio,
              fechaFinEstimada: fechaBaseFin,
              responsable:
                item.responsableSugerido || 'Área de Evaluación y Acreditación (AEA)',
              prioridad: item.prioridadSugerida || 'MEDIA',
            })
          );
        });

        this.loading.set(false);
      },
      error: (err) => {
        this.generalError.set(
          err?.error?.error ||
            'No se pudo recuperar la plantilla de plan de trabajo.'
        );
        this.loading.set(false);
      },
    });
  }

  private poblarActividadesDesdeExistentes(actividades: any[]): void {
    this.actividadesArray.clear();
    const fechaBaseInicio =
      this.convocatoria.registroConvocatoria?.fechaInicio ||
      new Date().toISOString().substring(0, 10);
    const fechaBaseFin =
      this.form.get('fechaLimitePresentacionConeau')?.value ||
      this.convocatoria.registroConvocatoria?.fechaCierre ||
      fechaBaseInicio;

    actividades.forEach((act) => {
      this.actividadesArray.push(
        this.crearGrupoActividad({
          numeroOrden: act.numeroOrden,
          codigoActividad: act.codigoActividad,
          nombre: act.nombre,
          hoja: act.hoja || 'INFO',
          fechaInicioEstimada: act.fechaInicioEstimada || fechaBaseInicio,
          fechaFinEstimada: act.fechaFinEstimada || fechaBaseFin,
          responsable: act.responsable || 'Área de Evaluación y Acreditación (AEA)',
          prioridad: act.prioridad || 'MEDIA',
        })
      );
    });
  }

  private crearGrupoActividad(item: {
    numeroOrden: number;
    codigoActividad: string;
    nombre: string;
    hoja: HojaPlanTrabajo;
    fechaInicioEstimada: string;
    fechaFinEstimada: string;
    responsable: string;
    prioridad: PrioridadActividad;
  }): FormGroup {
    return this.fb.group({
      numeroOrden: [item.numeroOrden, [Validators.required, Validators.min(1)]],
      codigoActividad: [item.codigoActividad, Validators.required],
      nombre: [item.nombre, Validators.required],
      hoja: [item.hoja, Validators.required],
      fechaInicioEstimada: [item.fechaInicioEstimada, Validators.required],
      fechaFinEstimada: [item.fechaFinEstimada, Validators.required],
      responsable: [item.responsable, Validators.required],
      prioridad: [item.prioridad, Validators.required],
    });
  }

  agregarActividad(): void {
    let maxOrden = 2;
    for (let i = 0; i < this.actividadesArray.length; i++) {
      const orden = Number(this.actividadesArray.at(i).get('numeroOrden')?.value);
      if (orden > maxOrden) {
        maxOrden = orden;
      }
    }
    const nuevoOrden = maxOrden + 1;
    const tabActual = this.filtroHoja();
    const hojaAsignada: HojaPlanTrabajo = tabActual === 'CONEAU' ? 'CONEAU' : 'INFO';

    const fechaBaseInicio =
      this.convocatoria.registroConvocatoria?.fechaInicio ||
      new Date().toISOString().substring(0, 10);
    const fechaBaseFin =
      this.form.get('fechaLimitePresentacionConeau')?.value ||
      this.convocatoria.registroConvocatoria?.fechaCierre ||
      fechaBaseInicio;

    const codStr = nuevoOrden < 10 ? `0${nuevoOrden}` : `${nuevoOrden}`;
    const nuevoGrupo = this.crearGrupoActividad({
      numeroOrden: nuevoOrden,
      codigoActividad: `ACT-${codStr}-NUEVA-ACTIVIDAD`,
      nombre: '',
      hoja: hojaAsignada,
      fechaInicioEstimada: fechaBaseInicio,
      fechaFinEstimada: fechaBaseFin,
      responsable: 'Área de Evaluación y Acreditación (AEA)',
      prioridad: 'MEDIA',
    });

    this.actividadesArray.push(nuevoGrupo);
  }

  eliminarActividad(index: number): void {
    this.actividadesArray.removeAt(index);
  }

  cambiarFiltro(hoja: 'TODAS' | 'INFO' | 'CONEAU'): void {
    this.filtroHoja.set(hoja);
  }

  get actividadesVisibles(): { control: FormGroup; index: number }[] {
    const tab = this.filtroHoja();
    const result: { control: FormGroup; index: number }[] = [];

    for (let i = 0; i < this.actividadesArray.length; i++) {
      const ctrl = this.actividadesArray.at(i) as FormGroup;
      if (tab === 'TODAS' || ctrl.get('hoja')?.value === tab) {
        result.push({ control: ctrl, index: i });
      }
    }

    return result;
  }

  esInvalidaFechaFinEstimada(ctrl: FormGroup): boolean {
    const inicio = ctrl.get('fechaInicioEstimada')?.value;
    const fin = ctrl.get('fechaFinEstimada')?.value;
    const limiteConeau = this.form.get('fechaLimitePresentacionConeau')?.value;

    if (!inicio || !fin) return false;
    if (fin < inicio) return true;
    if (limiteConeau && fin > limiteConeau) return true;
    return false;
  }

  esInvalidaFechaInicioEstimada(ctrl: FormGroup): boolean {
    const inicio = ctrl.get('fechaInicioEstimada')?.value;
    const limiteConeau = this.form.get('fechaLimitePresentacionConeau')?.value;

    if (!inicio || !limiteConeau) return false;
    return inicio > limiteConeau;
  }

  onSubmit(): void {
    this.validationErrors.set([]);
    this.generalError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
    }

    const errors: string[] = [];

    const fase = this.form.get('faseEvaluacion')?.value;
    if (!fase) {
      errors.push('La Fase de Evaluación CONEAU es obligatoria.');
    }

    const fechaLimite = this.form.get('fechaLimitePresentacionConeau')?.value;
    if (!fechaLimite) {
      errors.push('La Fecha Límite de Presentación CONEAU es obligatoria.');
    } else {
      const fechaCierre = this.convocatoria.registroConvocatoria?.fechaCierre;
      if (fechaCierre && fechaLimite < fechaCierre) {
        errors.push(
          `La Fecha Límite CONEAU (${fechaLimite}) no puede ser anterior a la fecha de cierre de la convocatoria (${fechaCierre}).`
        );
      }
    }

    if (this.actividadesArray.length === 0) {
      errors.push('El plan de trabajo debe incluir al menos una actividad de cronograma.');
    }

    for (let i = 0; i < this.actividadesArray.length; i++) {
      const act = this.actividadesArray.at(i);
      const orden = act.get('numeroOrden')?.value;
      const nombre = act.get('nombre')?.value;
      const inicio = act.get('fechaInicioEstimada')?.value;
      const fin = act.get('fechaFinEstimada')?.value;
      const resp = act.get('responsable')?.value;

      if (!nombre || !nombre.trim()) {
        errors.push(`La actividad orden #${orden} requiere un nombre descriptivo.`);
      }
      if (!resp || !resp.trim()) {
        errors.push(`La actividad orden #${orden} requiere un responsable asignado.`);
      }
      if (!inicio || !fin) {
        errors.push(`La actividad orden #${orden} requiere fechas estimadas de inicio y fin.`);
      } else {
        if (fin < inicio) {
          errors.push(
            `La actividad orden #${orden} ('${nombre || 'Sin nombre'}') tiene fecha fin (${fin}) anterior a fecha inicio (${inicio}).`
          );
        }
        if (fechaLimite && fin > fechaLimite) {
          errors.push(
            `La actividad orden #${orden} ('${nombre || 'Sin nombre'}') finaliza (${fin}) posterior a la fecha límite CONEAU (${fechaLimite}).`
          );
        }
        if (fechaLimite && inicio > fechaLimite) {
          errors.push(
            `La actividad orden #${orden} ('${nombre || 'Sin nombre'}') inicia (${inicio}) posterior a la fecha límite CONEAU (${fechaLimite}).`
          );
        }
      }
    }

    if (errors.length > 0) {
      this.validationErrors.set(errors);
      return;
    }

    this.submitting.set(true);

    const formVal = this.form.value;
    const itemsRequest: ItemActividadPlanRequest[] = this.actividadesArray.controls.map(
      (ctrl, idx) => {
        const val = ctrl.value;
        return {
          numeroOrden: Number(val.numeroOrden) || idx + 3,
          codigoActividad: val.codigoActividad.trim(),
          nombre: val.nombre.trim(),
          hoja: val.hoja as HojaPlanTrabajo,
          fechaInicioEstimada: val.fechaInicioEstimada,
          fechaFinEstimada: val.fechaFinEstimada,
          responsable: val.responsable.trim(),
          prioridad: val.prioridad as PrioridadActividad,
        };
      }
    );

    const requestPayload: ElaborarPlanTrabajoRequest = {
      rmEstandares: formVal.rmEstandares.trim(),
      faseEvaluacion: formVal.faseEvaluacion as FaseEvaluacion,
      fechaLimitePresentacionConeau: formVal.fechaLimitePresentacionConeau,
      fechaLimiteConeau: formVal.fechaLimitePresentacionConeau,
      actividades: itemsRequest,
    };

    this.convocatoriaService
      .elaborarPlanTrabajo(this.procesoId, requestPayload)
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.guardado.emit();
        },
        error: (err) => {
          this.submitting.set(false);
          this.generalError.set(
            err?.error?.error ||
              err?.error?.message ||
              'Ocurrió un error al formalizar el plan de trabajo y metadatos CONEAU.'
          );
        },
      });
  }

  cancelar(): void {
    this.cerrado.emit();
  }
}
