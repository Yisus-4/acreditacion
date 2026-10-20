import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  DocumentoNormativaDto,
  METADATOS_DOCUMENTOS_NORMATIVA,
  TipoDocumentoNormativa,
} from '../../../core/normativa/models/normativa.model';

export interface FilaHojaInfo {
  numeroOrden: number;
  tipoDocumento: TipoDocumentoNormativa;
  titulo: string;
  esObligatorio: boolean;
  cargado: boolean;
  documentoId?: string;
  esBorrador?: boolean;
  observado?: boolean;
  motivoObservacion?: string;
  nombreArchivo?: string;
  tamanioMb?: string;
  estadoDocumento: string;
  actualizadoEn?: string;
}

@Component({
  selector: 'app-hoja-info-tabular',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hoja-info-tabular.component.html',
  styleUrls: ['./hoja-info-tabular.component.scss'],
})
export class HojaInfoTabularComponent {
  @Input({ required: true }) planTrabajoId = '';
  @Input() procesoCodigo = '';
  @Input() carreraEsNueva = false;
  @Input() carreraTieneHistorial = false;
  @Input() puedeEditar = false;
  @Input() canManage = false;

  @Input() set documentos(docs: DocumentoNormativaDto[]) {
    this.construirFilas(docs || []);
  }

  @Output() exportarExcel = new EventEmitter<string>();
  @Output() subirDocumento = new EventEmitter<{ tipo: TipoDocumentoNormativa; archivo: File }>();
  @Output() eliminarDocumento = new EventEmitter<string>();
  @Output() descargarDocumento = new EventEmitter<{ documentoId: string; nombreArchivo: string }>();

  readonly filas = signal<FilaHojaInfo[]>([]);
  readonly descargandoExcel = signal<boolean>(false);

  private construirFilas(docs: DocumentoNormativaDto[]): void {
    const ordenTipos: { tipo: TipoDocumentoNormativa; obligatoria: boolean }[] = [
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
      { tipo: 'RESOLUCION_CONEAU_ANTERIOR', obligatoria: false },
    ];

    const filasConstruidas: FilaHojaInfo[] = ordenTipos
      .filter((item) => !!METADATOS_DOCUMENTOS_NORMATIVA[item.tipo])
      .map((item, idx) => {
        const meta = METADATOS_DOCUMENTOS_NORMATIVA[item.tipo]!;
        const doc = docs.find((d) => d.tipoDocumento === item.tipo);

        let estado = 'Pendiente';
        if (doc) {
          if (doc.observado) estado = 'Observado';
          else if (doc.esBorrador) estado = 'Borrador';
          else estado = 'Cargado';
        }

        return {
          numeroOrden: idx + 1,
          tipoDocumento: item.tipo,
          titulo: meta.titulo,
          esObligatorio: item.obligatoria,
          cargado: !!doc,
          documentoId: doc?.id,
          esBorrador: doc?.esBorrador,
          observado: doc?.observado,
          motivoObservacion: doc?.motivoObservacion,
          nombreArchivo: doc?.nombreOriginal,
          tamanioMb: doc ? (doc.tamanioBytes / 1024 / 1024).toFixed(2) + ' MB' : undefined,
          estadoDocumento: estado,
          actualizadoEn: doc?.actualizadoEn,
        };
      });

    this.filas.set(filasConstruidas);
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
      alert(`El archivo '${file.name}' no es un documento PDF válido.`);
      return;
    }

    const maxSize = 20 * 1024 * 1024;
    if (file.size > maxSize) {
      alert(`El archivo excede el tamaño máximo permitido de 20 MB.`);
      return;
    }

    this.subirDocumento.emit({ tipo, archivo: file });
  }

  onDescargar(docId?: string, nombreArchivo?: string): void {
    if (!docId || !nombreArchivo) return;
    this.descargarDocumento.emit({ documentoId: docId, nombreArchivo });
  }

  onEliminar(docId?: string): void {
    if (!docId) return;
    if (confirm('¿Está seguro de que desea eliminar este documento borrador?')) {
      this.eliminarDocumento.emit(docId);
    }
  }

  onDescargarExcel(): void {
    if (!this.planTrabajoId) return;
    this.descargandoExcel.set(true);
    this.exportarExcel.emit(this.planTrabajoId);
  }

  setDescargaFinalizada(): void {
    this.descargandoExcel.set(false);
  }
}
