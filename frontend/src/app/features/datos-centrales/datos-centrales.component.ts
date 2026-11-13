import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Auth } from '../../core/auth/auth';
import { EstructuraService, CarreraResponse } from '../../core/estructura/estructura.service';
import {
  DatosCentralesService,
  PlanEstudioMaestro,
  CuerpoAcademicoMaestro,
  ResumenDatosCentrales,
  EstadoVigenciaPlan,
} from '../../core/services/datos-centrales.service';

type TabTipo = 'planes' | 'docentes' | 'infraestructura' | 'ambitos-convenios';

@Component({
  selector: 'app-datos-centrales',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './datos-centrales.component.html',
  styleUrls: ['./datos-centrales.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DatosCentralesComponent implements OnInit {
  private readonly datos = inject(DatosCentralesService);
  private readonly estructura = inject(EstructuraService);
  private readonly auth = inject(Auth);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly tab = signal<TabTipo>('planes');
  readonly resumen = signal<ResumenDatosCentrales | null>(null);
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly carreraId = signal<string>('');
  readonly vigencia = signal<string>('');
  readonly busquedaDocente = signal<string>('');
  readonly planes = signal<PlanEstudioMaestro[]>([]);
  readonly docentes = signal<CuerpoAcademicoMaestro[]>([]);
  readonly docenteSel = signal<CuerpoAcademicoMaestro | null>(null);
  readonly modalPlan = signal<boolean>(false);

  readonly canEdit = computed(() => {
    const roles = this.auth.currentSession?.user?.roleCodes ?? [];
    return roles.some((r) => r === 'ADMIN' || r === 'ADMINISTRATOR' || r === 'AEA');
  });

  readonly planForm = this.fb.group({
    carreraId: ['', Validators.required],
    nombrePlan: ['', [Validators.required, Validators.minLength(3)]],
    codigoResolucion: [''],
    anioAprobacion: [new Date().getFullYear(), [Validators.required, Validators.min(1970)]],
    duracionAnios: [5, [Validators.required, Validators.min(1)]],
    horasTotales: [3000, [Validators.required, Validators.min(100)]],
  });

  ngOnInit(): void {
    this.datos.getResumen().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((r) => this.resumen.set(r));
    this.estructura.listarCarreras().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((cs) => {
      this.carreras.set(cs);
      this.cargarPlanes();
      this.cargarDocentes();
    });
  }

  cargarPlanes(): void {
    const c = this.carreraId() || undefined;
    const v = this.vigencia() || undefined;
    this.datos.getPlanesEstudio(c, v).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((ps) => this.planes.set(ps));
  }

  cambiarVigencia(plan: PlanEstudioMaestro, estado: EstadoVigenciaPlan): void {
    if (!this.canEdit()) return;
    this.datos.cambiarEstadoVigencia(plan.id, { estadoVigencia: estado }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.cargarPlanes();
      this.datos.getResumen().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((r) => this.resumen.set(r));
    });
  }

  guardarPlan(): void {
    if (this.planForm.invalid || !this.canEdit()) return;
    const val = this.planForm.value;
    this.datos.crearPlanEstudio({
      carreraId: val.carreraId!,
      nombrePlan: val.nombrePlan!,
      codigoResolucion: val.codigoResolucion || undefined,
      anioAprobacion: Number(val.anioAprobacion),
      duracionAnios: Number(val.duracionAnios),
      horasTotales: Number(val.horasTotales),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.modalPlan.set(false);
      this.planForm.reset({ carreraId: '', nombrePlan: '', codigoResolucion: '', anioAprobacion: new Date().getFullYear(), duracionAnios: 5, horasTotales: 3000 });
      this.cargarPlanes();
      this.datos.getResumen().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((r) => this.resumen.set(r));
    });
  }

  cargarDocentes(): void {
    const c = this.carreraId() || undefined;
    const q = this.busquedaDocente() || undefined;
    this.datos.getCuerpoAcademico(c, q).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((ds) => this.docentes.set(ds));
  }
}
