import { Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';

@Component({
  selector: 'app-completar-actividad-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './completar-actividad-modal.component.html',
  styleUrls: ['./completar-actividad-modal.component.scss'],
})
export class CompletarActividadModalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly convocatoriaService = inject(ConvocatoriaService);

  @Input({ required: true }) actividadId!: string;
  @Input({ required: true }) actividadNombre!: string;
  @Output() cerrado = new EventEmitter<boolean>();

  form: FormGroup = this.fb.group({
    fechaReunion: ['', Validators.required],
    sintesisAcuerdos: ['', [Validators.required, Validators.minLength(10)]],
    tipoDocumento: ['ACTA_REUNION_COORDINACION', Validators.required],
  });

  selectedFile: File | null = null;
  readonly fileError = signal<string | null>(null);
  readonly submitting = signal<boolean>(false);
  readonly generalError = signal<string | null>(null);

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fileError.set(null);

    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Validación estricta de PDF
      const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
      if (!isPdf) {
        this.fileError.set('Solo se admiten documentos probatorios en formato PDF.');
        this.selectedFile = null;
        input.value = '';
        return;
      }

      // Validación de tamaño máximo 20 MB (20 * 1024 * 1024)
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

  get fechaReunionPreview(): string | null {
    const raw = this.form.get('fechaReunion')?.value;
    if (!raw) return null;
    const d = new Date(raw);
    if (isNaN(d.getTime())) return null;
    const dia = String(d.getDate()).padStart(2, '0');
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const anio = d.getFullYear();
    const hora = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    return `${dia}/${mes}/${anio} ${hora}:${min} hs`;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.selectedFile) {
      this.fileError.set('Debe adjuntar el archivo PDF firmado de la reunión o normativa.');
      return;
    }

    this.submitting.set(true);
    this.generalError.set(null);

    const rawFecha = this.form.get('fechaReunion')?.value;
    let fechaParaEnviar = rawFecha;
    if (rawFecha) {
      const d = new Date(rawFecha);
      if (!isNaN(d.getTime())) {
        fechaParaEnviar = d.toISOString();
      }
    }

    const formData = new FormData();
    formData.append('fechaReunion', fechaParaEnviar);
    formData.append('sintesisAcuerdos', this.form.get('sintesisAcuerdos')?.value.trim());
    formData.append('tipoDocumento', this.form.get('tipoDocumento')?.value);
    formData.append('archivo', this.selectedFile);

    this.convocatoriaService.completarActividad(this.actividadId, formData).subscribe({
      next: () => {
        this.submitting.set(false);
        this.cerrado.emit(true);
      },
      error: (err) => {
        this.submitting.set(false);
        this.generalError.set(err?.error?.error || 'Ocurrió un error al registrar la actividad.');
      },
    });
  }

  cancelar(): void {
    this.cerrado.emit(false);
  }
}
