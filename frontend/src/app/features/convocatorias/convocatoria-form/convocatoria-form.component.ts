import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import { EstructuraService, CarreraResponse } from '../../../core/estructura/estructura.service';
import { IniciarConvocatoriaRequest, TipoProceso } from '../../../core/convocatorias/models/convocatoria.model';

@Component({
  selector: 'app-convocatoria-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './convocatoria-form.component.html',
  styleUrls: ['./convocatoria-form.component.scss'],
})
export class ConvocatoriaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly router = inject(Router);

  readonly today = new Date().toISOString().split('T')[0];

  form!: FormGroup;
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly loadingCarreras = signal<boolean>(false);
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.initForm();
    this.cargarCarrerasDeGrado();
  }

  private initForm(): void {
    this.form = this.fb.group(
      {
        carreraId: ['', Validators.required],
        nroResolucionConeau: ['', [Validators.required, Validators.pattern(/.*\S.*/)]],
        fechaInicio: [this.today, Validators.required],
        fechaCierre: [
          '',
          [
            Validators.required,
            (control: AbstractControl): ValidationErrors | null => {
              if (!control.value) {
                return null;
              }
              return control.value < this.today ? { fechaCierreAnteriorAHoy: true } : null;
            },
          ],
        ],
        tipoProceso: ['EN_FUNCIONAMIENTO' as TipoProceso, Validators.required],
        observaciones: [''],
      },
      { validators: this.fechasValidator }
    );
  }

  private fechasValidator(group: FormGroup): { [key: string]: boolean } | null {
    const inicio = group.get('fechaInicio')?.value;
    const cierre = group.get('fechaCierre')?.value;
    if (inicio && cierre && cierre < inicio) {
      return { fechaCierreAnterior: true };
    }
    return null;
  }

  private cargarCarrerasDeGrado(): void {
    this.loadingCarreras.set(true);
    this.estructuraService.listarCarreras().subscribe({
      next: (data) => {
        // Filtrar estrictamente nivel GRADO y activas
        const carrerasGrado = data.filter((c) => c.activo && (!c.nivel || c.nivel.toUpperCase() === 'GRADO'));
        this.carreras.set(carrerasGrado);
        this.loadingCarreras.set(false);
      },
      error: () => {
        this.loadingCarreras.set(false);
        this.errorMessage.set('No fue posible cargar las carreras académicas.');
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const formVal = this.form.value;
    const request: IniciarConvocatoriaRequest = {
      carreraId: formVal.carreraId,
      nroResolucionConeau: formVal.nroResolucionConeau.trim(),
      fechaInicio: formVal.fechaInicio,
      fechaCierre: formVal.fechaCierre,
      tipoProceso: formVal.tipoProceso,
      observaciones: formVal.observaciones?.trim() || undefined,
    };

    this.convocatoriaService.iniciarConvocatoria(request).subscribe({
      next: (proceso) => {
        this.submitting.set(false);
        void this.router.navigate(['/acreditacion/convocatorias', proceso.id]);
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(err?.error?.error || 'Ocurrió un error al iniciar la convocatoria.');
      },
    });
  }
}
