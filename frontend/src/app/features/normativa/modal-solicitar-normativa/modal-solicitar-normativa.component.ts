import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SolicitarNormativaRequest } from '../../../core/normativa/models/normativa.model';

@Component({
  selector: 'app-modal-solicitar-normativa',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modal-solicitar-normativa.component.html',
  styleUrls: ['./modal-solicitar-normativa.component.scss'],
})
export class ModalSolicitarNormativaComponent {
  @Input({ required: true }) abierto = false;
  @Input() procesoCodigo = '';
  @Input() carreraNombre = '';

  @Output() confirmar = new EventEmitter<SolicitarNormativaRequest>();
  @Output() cancelar = new EventEmitter<void>();

  observaciones = signal<string>('');
  plazoDiasHabiles = signal<number>(5);
  errorMsg = signal<string | null>(null);

  onConfirmar(): void {
    const plazo = this.plazoDiasHabiles();
    if (!plazo || plazo < 1 || plazo > 5) {
      this.errorMsg.set('El plazo oficial no puede ser mayor a 5 días hábiles administrativos.');
      return;
    }

    this.errorMsg.set(null);
    this.confirmar.emit({
      observaciones: this.observaciones().trim() || undefined,
      plazoDiasHabiles: plazo,
    });
  }

  onCancelar(): void {
    this.errorMsg.set(null);
    this.cancelar.emit();
  }
}
