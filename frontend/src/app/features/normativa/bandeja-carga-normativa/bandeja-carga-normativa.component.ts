import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  DocumentoNormativaDto,
  METADATOS_DOCUMENTOS_NORMATIVA,
  TipoDocumentoMeta,
  TipoDocumentoNormativa,
} from '../../../core/normativa/models/normativa.model';

export interface RanuraDocumento {
  meta: TipoDocumentoMeta;
  esObligatorio: boolean;
  documentoCargado?: DocumentoNormativaDto;
}

@Component({
  selector: 'app-bandeja-carga-normativa',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bandeja-carga-normativa.component.html',
  styleUrls: ['./bandeja-carga-normativa.component.scss'],
})
export class BandejaCargaNormativaComponent {
  @Input({ required: true }) procesoId = '';
  @Input({ required: true }) set documentos(val: DocumentoNormativaDto[]) {
    this.documentosSignal.set(val || []);
  }
  @Input() carreraEsNueva = false;
  @Input() carreraTieneHistorial = false;
  @Input() puedeEditar = false;
  @Input() loading = false;

  @Output() subirDocumento = new EventEmitter<{ tipo: TipoDocumentoNormativa; archivo: File }>();
  @Output() eliminarDocumento = new EventEmitter<string>();
  @Output() descargarDocumento = new EventEmitter<{ documentoId: string; nombreArchivo: string }>();

  readonly documentosSignal = signal<DocumentoNormativaDto[]>([]);
  readonly uploadError = signal<string | null>(null);

  readonly ranurasNormativaGeneral = computed<RanuraDocumento[]>(() => {
    const lista: { tipo: TipoDocumentoNormativa; obligatoria: boolean }[] = [
      { tipo: 'CREACION_CARRERA', obligatoria: true },
      { tipo: 'ORDENANZA_PLAN_ESTUDIOS', obligatoria: true },
      { tipo: 'RESOLUCION_DIRECTOR_CARRERA', obligatoria: true },
      { tipo: 'RESOLUCION_COMISION_CURRICULAR', obligatoria: true },
      { tipo: 'PLAN_DESARROLLO_CARRERA', obligatoria: true },
      { tipo: 'RM_VALIDEZ_NACIONAL', obligatoria: !this.carreraEsNueva },
      { tipo: 'APROBACION_PROGRAMAS_ANALITICOS', obligatoria: !this.carreraEsNueva },
      { tipo: 'REGLAMENTO_CARRERA', obligatoria: false },
      { tipo: 'ACTA_COMISION_CURRICULAR', obligatoria: false },
      { tipo: 'RM_ESTANDARES_CARRERA', obligatoria: false },
      { tipo: 'RM_INCLUSION_ART_43', obligatoria: false },
    ];

    return lista
      .filter((item) => !!METADATOS_DOCUMENTOS_NORMATIVA[item.tipo])
      .map((item) => this.construirRanura(item.tipo, item.obligatoria));
  });

  readonly ranurasEvaluacionesAnteriores = computed<RanuraDocumento[]>(() => {
    const lista: { tipo: TipoDocumentoNormativa; obligatoria: boolean }[] = [
      { tipo: 'RESOLUCION_CONEAU_ANTERIOR', obligatoria: false },
    ];

    return lista
      .filter((item) => !!METADATOS_DOCUMENTOS_NORMATIVA[item.tipo])
      .map((item) => this.construirRanura(item.tipo, item.obligatoria));
  });

  private construirRanura(tipo: TipoDocumentoNormativa, esObligatorio: boolean): RanuraDocumento {
    const doc = this.documentosSignal().find((d) => d.tipoDocumento === tipo);
    const meta = METADATOS_DOCUMENTOS_NORMATIVA[tipo]!;
    return {
      meta,
      esObligatorio,
      documentoCargado: doc,
    };
  }

  onFileSelected(event: Event, tipo: TipoDocumentoNormativa): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    input.value = '';

    const isPdf =
      file.type === 'application/pdf' ||
      file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf) {
      this.uploadError.set(`El archivo '${file.name}' no es un documento PDF válido.`);
      return;
    }

    const maxSize = 20 * 1024 * 1024;
    if (file.size > maxSize) {
      this.uploadError.set(`El archivo excede el tamaño máximo permitido de 20 MB.`);
      return;
    }

    this.uploadError.set(null);
    this.subirDocumento.emit({ tipo, archivo: file });
  }

  onDescargar(doc: DocumentoNormativaDto): void {
    this.descargarDocumento.emit({
      documentoId: doc.id,
      nombreArchivo: doc.nombreOriginal,
    });
  }

  onEliminar(docId: string): void {
    if (confirm('¿Está seguro de que desea eliminar este documento borrador?')) {
      this.eliminarDocumento.emit(docId);
    }
  }
}
