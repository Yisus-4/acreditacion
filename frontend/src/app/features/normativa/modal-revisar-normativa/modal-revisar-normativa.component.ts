import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  DocumentoNormativaDto,
  RevisarNormativaRequest,
  DocumentoObservadoDto,
} from '../../../core/normativa/models/normativa.model';

interface ItemDocumentoObservado {
  documentoId: string;
  nombreOriginal: string;
  tipoDocumento: string;
  seleccionado: boolean;
  motivo: string;
}

@Component({
  selector: 'app-modal-revisar-normativa',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modal-revisar-normativa.component.html',
  styleUrls: ['./modal-revisar-normativa.component.scss'],
})
export class ModalRevisarNormativaComponent {
  @Input({ required: true }) abierto = false;
  @Input() procesoCodigo = '';
  @Input() carreraNombre = '';
  @Input() submitting = false;
  @Input() serverError: string | null = null;

  @Input() set documentos(docs: DocumentoNormativaDto[]) {
    if (docs) {
      this.itemsObservacion.set(
        docs.map((d) => ({
          documentoId: d.id,
          nombreOriginal: d.nombreOriginal,
          tipoDocumento: d.tipoDocumento,
          seleccionado: d.observado ?? false,
          motivo: d.motivoObservacion ?? '',
        }))
      );
    } else {
      this.itemsObservacion.set([]);
    }
  }

  @Output() confirmar = new EventEmitter<RevisarNormativaRequest>();
  @Output() cancelar = new EventEmitter<void>();

  decision = signal<'APROBADO' | 'REQUIERE_CORRECCIONES'>('APROBADO');
  observaciones = signal<string>('');
  itemsObservacion = signal<ItemDocumentoObservado[]>([]);
  errorMsg = signal<string | null>(null);

  toggleItem(index: number): void {
    const items = [...this.itemsObservacion()];
    items[index].seleccionado = !items[index].seleccionado;
    this.itemsObservacion.set(items);
  }

  setMotivo(index: number, motivo: string): void {
    const items = [...this.itemsObservacion()];
    items[index].motivo = motivo;
    this.itemsObservacion.set(items);
  }

  onConfirmar(): void {
    const dec = this.decision();
    let obs = this.observaciones().trim();

    if (dec === 'REQUIERE_CORRECCIONES') {
      const seleccionados = this.itemsObservacion().filter((item) => item.seleccionado);
      const motivosPuntuales = seleccionados
        .map((item) => item.motivo.trim())
        .filter((m) => m.length > 0);

      if (!obs && motivosPuntuales.length > 0) {
        obs = motivosPuntuales.join('; ');
        this.observaciones.set(obs);
      }

      if (!obs) {
        this.errorMsg.set(
          'Debe ingresar las observaciones generales de revisión o detallar el motivo en al menos un documento observado.'
        );
        return;
      }

      const observados: DocumentoObservadoDto[] = seleccionados.map((item) => ({
        documentoId: item.documentoId,
        motivo: item.motivo.trim() || obs,
      }));

      this.errorMsg.set(null);
      this.confirmar.emit({
        decision: dec,
        observaciones: obs,
        documentosObservados: observados,
      });
    } else {
      this.errorMsg.set(null);
      this.confirmar.emit({
        decision: 'APROBADO',
        observaciones: obs || undefined,
        documentosObservados: [],
      });
    }
  }

  onCancelar(): void {
    this.errorMsg.set(null);
    this.cancelar.emit();
  }
}
