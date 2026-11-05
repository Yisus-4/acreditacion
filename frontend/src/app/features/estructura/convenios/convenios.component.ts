import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { SubnavGestionInstitucionalComponent } from '../subnav-gestion-institucional/subnav-gestion-institucional.component';
import { ConvenioService } from '../../../core/convenios/convenio.service';
import { ConvenioInstitucional } from '../../../core/convenios/convenio.models';

@Component({
  selector: 'app-convenios',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    SubnavGestionInstitucionalComponent,
  ],
  templateUrl: './convenios.component.html',
  styleUrls: ['../estructura-comun.scss', './convenios.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConveniosComponent implements OnInit {
  private readonly convenioService = inject(ConvenioService);
  private readonly fb = inject(FormBuilder);

  readonly convenios = signal<ConvenioInstitucional[]>([]);
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly searchQuery = signal<string>('');
  readonly filtroEstado = signal<'TODOS' | 'VIGENTES' | 'NO_VIGENTES'>('TODOS');
  readonly filtroTipo = signal<string>('TODOS');

  readonly modalAbierto = signal<boolean>(false);
  readonly convenioSeleccionadoId = signal<string | null>(null);
  readonly guardando = signal<boolean>(false);

  readonly form = this.fb.group({
    expediente: ['', [Validators.required, Validators.maxLength(50)]],
    resolucionRectoral: [''],
    contraparte: ['', [Validators.required, Validators.maxLength(250)]],
    tipoConvenio: ['MARCO', [Validators.required]],
    objeto: ['', [Validators.required]],
    fechaCelebracion: ['', [Validators.required]],
    duracion: [''],
    fechaCese: [''],
    vigente: [true],
    responsableUndec: [''],
    carrerasBeneficiadas: [''],
  });

  readonly conveniosFiltrados = computed(() => {
    let lista = this.convenios();
    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      lista = lista.filter(
        (c) =>
          c.expediente?.toLowerCase().includes(query) ||
          c.contraparte?.toLowerCase().includes(query) ||
          c.objeto?.toLowerCase().includes(query) ||
          c.resolucionRectoral?.toLowerCase().includes(query)
      );
    }

    const estado = this.filtroEstado();
    if (estado === 'VIGENTES') {
      lista = lista.filter((c) => c.vigente);
    } else if (estado === 'NO_VIGENTES') {
      lista = lista.filter((c) => !c.vigente);
    }

    const tipo = this.filtroTipo();
    if (tipo !== 'TODOS') {
      lista = lista.filter((c) => c.tipoConvenio === tipo);
    }

    return lista;
  });

  ngOnInit(): void {
    this.cargarConvenios();
  }

  cargarConvenios(): void {
    this.loading.set(true);
    this.convenioService.listarConvenios().subscribe({
      next: (data) => {
        this.convenios.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Error al cargar convenios institucionales: ' + (err.error?.message || err.message));
        this.loading.set(false);
      },
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  abrirModalNuevo(): void {
    this.convenioSeleccionadoId.set(null);
    this.form.reset({
      expediente: '',
      resolucionRectoral: '',
      contraparte: '',
      tipoConvenio: 'MARCO',
      objeto: '',
      fechaCelebracion: new Date().toISOString().substring(0, 10),
      duracion: '1 año renovable',
      fechaCese: '',
      vigente: true,
      responsableUndec: '',
      carrerasBeneficiadas: '',
    });
    this.modalAbierto.set(true);
  }

  abrirModalEditar(convenio: ConvenioInstitucional): void {
    this.convenioSeleccionadoId.set(convenio.id || null);
    this.form.patchValue({
      expediente: convenio.expediente,
      resolucionRectoral: convenio.resolucionRectoral || '',
      contraparte: convenio.contraparte,
      tipoConvenio: convenio.tipoConvenio || 'MARCO',
      objeto: convenio.objeto,
      fechaCelebracion: convenio.fechaCelebracion ? convenio.fechaCelebracion.substring(0, 10) : '',
      duracion: convenio.duracion || '',
      fechaCese: convenio.fechaCese ? convenio.fechaCese.substring(0, 10) : '',
      vigente: convenio.vigente,
      responsableUndec: convenio.responsableUndec || '',
      carrerasBeneficiadas: convenio.carrerasBeneficiadas || '',
    });
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.convenioSeleccionadoId.set(null);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const formVal = this.form.value;
    const dto: ConvenioInstitucional = {
      expediente: formVal.expediente!,
      resolucionRectoral: formVal.resolucionRectoral || undefined,
      contraparte: formVal.contraparte!,
      tipoConvenio: formVal.tipoConvenio || 'MARCO',
      objeto: formVal.objeto!,
      fechaCelebracion: formVal.fechaCelebracion!,
      duracion: formVal.duracion || undefined,
      fechaCese: formVal.fechaCese || undefined,
      vigente: formVal.vigente ?? true,
      responsableUndec: formVal.responsableUndec || undefined,
      carrerasBeneficiadas: formVal.carrerasBeneficiadas || undefined,
    };

    const id = this.convenioSeleccionadoId();
    if (id) {
      this.convenioService.actualizarConvenio(id, dto).subscribe({
        next: () => {
          this.successMessage.set('Convenio institucional actualizado exitosamente.');
          this.guardando.set(false);
          this.cerrarModal();
          this.cargarConvenios();
          setTimeout(() => this.successMessage.set(null), 4000);
        },
        error: (err) => {
          this.errorMessage.set('Error al actualizar convenio: ' + (err.error?.message || err.message));
          this.guardando.set(false);
        },
      });
    } else {
      this.convenioService.crearConvenio(dto).subscribe({
        next: () => {
          this.successMessage.set('Convenio registrado exitosamente en el padrón.');
          this.guardando.set(false);
          this.cerrarModal();
          this.cargarConvenios();
          setTimeout(() => this.successMessage.set(null), 4000);
        },
        error: (err) => {
          this.errorMessage.set('Error al registrar convenio: ' + (err.error?.message || err.message));
          this.guardando.set(false);
        },
      });
    }
  }

  eliminar(convenio: ConvenioInstitucional): void {
    if (!convenio.id) return;
    const confirmacion = window.confirm(
      `¿Desea dar de baja o eliminar el convenio ${convenio.expediente} con ${convenio.contraparte}?`
    );
    if (!confirmacion) return;

    this.convenioService.eliminarConvenio(convenio.id).subscribe({
      next: () => {
        this.successMessage.set('Convenio eliminado.');
        this.cargarConvenios();
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (err) => {
        this.errorMessage.set('No se pudo eliminar el convenio: ' + (err.error?.message || err.message));
      },
    });
  }
}
