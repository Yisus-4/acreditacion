import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Auth } from '../../../core/auth/auth';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import {
  ActividadPlanTrabajoDto,
  ConvocatoriaDetalle,
  PlanTrabajoDto,
} from '../../../core/convocatorias/models/convocatoria.model';
import { CompletarActividadModalComponent } from '../completar-actividad-modal/completar-actividad-modal.component';
import { ElaborarPlanTrabajoModalComponent } from '../elaborar-plan-trabajo-modal/elaborar-plan-trabajo-modal.component';
import { NormativaDetalleComponent } from '../../normativa/normativa-detalle/normativa-detalle.component';
import { NormativaService } from '../../../core/normativa/services/normativa.service';
import { BadgePlazoDto, formatearEstadoNormativa } from '../../../core/normativa/models/normativa.model';

export interface SiguientePasoInfo {
  codigo: string;
  etapa: string;
  responsable: string;
  plazo: string;
  badgePlazo?: BadgePlazoDto;
  texto: string;
  botonTexto: string;
  accionTipo: 'REUNION' | 'NORMATIVA' | 'REVISION_NORMATIVA' | 'PLAN' | 'SEGUIMIENTO';
  actividad: ActividadPlanTrabajoDto | null;
}

export interface EtapaProcesoInfo {
  numero: number;
  nombre: string;
  estado: 'COMPLETADA' | 'ACTIVA' | 'PENDIENTE';
}

@Component({
  selector: 'app-convocatoria-detalle',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CompletarActividadModalComponent,
    ElaborarPlanTrabajoModalComponent,
    NormativaDetalleComponent,
  ],
  templateUrl: './convocatoria-detalle.component.html',
  styleUrls: ['./convocatoria-detalle.component.scss'],
})
export class ConvocatoriaDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly normativaService = inject(NormativaService);
  private readonly auth = inject(Auth);

  readonly convocatoria = signal<ConvocatoriaDetalle | null>(null);
  readonly planTrabajo = signal<PlanTrabajoDto | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly tabActiva = signal<'ACTIVIDADES' | 'SEGUIMIENTO' | 'FICHA'>('ACTIVIDADES');
  readonly actividadFoco = signal<string>('1.3');
  readonly reiniciando = signal<boolean>(false);

  readonly actividadSeleccionada = signal<ActividadPlanTrabajoDto | null>(null);
  readonly actividadSeleccionadaDrawer = signal<ActividadPlanTrabajoDto | null>(null);
  readonly modalElaborarPlanAbierto = signal<boolean>(false);
  readonly bannerExitoPlan = signal<string | null>(null);
  readonly filtroHoja = signal<'TODAS' | 'INFO' | 'CONEAU'>('TODAS');
  readonly filtroEstado = signal<string>('TODOS');
  readonly filtroPrioridad = signal<string>('TODAS');
  readonly busquedaTexto = signal<string>('');

  readonly normativaDetalle = this.normativaService.detalle;

  readonly canManage = computed(() => {
    const session = this.auth.currentSession;
    if (!session?.user?.roleCodes) return false;
    return session.user.roleCodes.includes('ADMINISTRATOR') || session.user.roleCodes.includes('AEA');
  });

  readonly esDirectorCarrera = computed(() => {
    const session = this.auth.currentSession;
    if (!session?.user?.roleCodes) return false;
    return session.user.roleCodes.includes('CAREER_DIRECTOR');
  });

  readonly conteoInfo = computed(() => {
    return this.planTrabajo()?.actividades?.filter((a) => a.hoja === 'INFO').length ?? 0;
  });

  readonly conteoConeau = computed(() => {
    return this.planTrabajo()?.actividades?.filter((a) => a.hoja === 'CONEAU').length ?? 0;
  });

  readonly conteoTotal = computed(() => {
    return this.planTrabajo()?.actividades?.length ?? 0;
  });

  readonly conteoActividadesPendientes = computed(() => {
    const acts = this.planTrabajo()?.actividades ?? [];
    return acts.filter((a) => a.estado === 'PENDIENTE' || a.estado === 'EN_PROGRESO').length;
  });

  readonly actividadReunion = computed(() => {
    const acts = this.planTrabajo()?.actividades ?? [];
    return (
      acts.find(
        (a) =>
          a.codigoActividad === 'ACT-01-REUNION-COORDINACION' ||
          a.codigoActividad === 'ACT-01-INFORMAR-PROCESO' ||
          a.codigoActividad === '1.1' ||
          a.numeroOrden === 1
      ) || null
    );
  });

  readonly actividadElaborarPlan = computed(() => {
    const acts = this.planTrabajo()?.actividades ?? [];
    return (
      acts.find(
        (a) =>
          a.codigoActividad === 'ACT-02-ELABORAR-PLAN-TRABAJO' ||
          a.codigoActividad === '1.2' ||
          (a.numeroOrden === 2 && a.nombre.toLowerCase().includes('plan de trabajo'))
      ) || null
    );
  });

  readonly actividadNormativa = computed(() => {
    const acts = this.planTrabajo()?.actividades ?? [];
    return (
      acts.find(
        (a) =>
          a.codigoActividad === '1.3' ||
          a.codigoActividad === 'ACT-03-NORMATIVA' ||
          a.nombre.toLowerCase().includes('normativa')
      ) || null
    );
  });

  readonly actividadesFiltradas = computed(() => {
    const hoja = this.filtroHoja();
    const estado = this.filtroEstado();
    const prioridad = this.filtroPrioridad();
    const busqueda = this.busquedaTexto().trim().toLowerCase();
    const acts = this.planTrabajo()?.actividades ?? [];

    return acts.filter((a) => {
      if (hoja !== 'TODAS' && (a.hoja || 'INFO') !== hoja) {
        return false;
      }
      if (estado !== 'TODOS' && a.estado !== estado) {
        return false;
      }
      if (prioridad !== 'TODAS' && (a.prioridad || 'MEDIA') !== prioridad) {
        return false;
      }
      if (busqueda) {
        const cod = (a.codigoActividad || '').toLowerCase();
        const nom = (a.nombre || '').toLowerCase();
        const resp = (a.responsable || '').toLowerCase();
        if (!cod.includes(busqueda) && !nom.includes(busqueda) && !resp.includes(busqueda)) {
          return false;
        }
      }
      return true;
    });
  });

  readonly requiereElaborarPlan = computed(() => {
    const plan = this.planTrabajo();
    if (!plan) return false;
    const act2 = plan.actividades?.find((a) => a.codigoActividad === 'ACT-02-ELABORAR-PLAN-TRABAJO');
    if (act2 && act2.estado === 'PENDIENTE') return true;
    const cronogramaActs = plan.actividades?.filter((a) => a.numeroOrden >= 3) ?? [];
    return cronogramaActs.length === 0;
  });

  /**
   * Elemento más importante (DESIGN.md 7.2): Siguiente Paso Requerido
   */
  readonly siguientePaso = computed<SiguientePasoInfo>(() => {
    const act1 = this.actividadReunion();
    const act2 = this.actividadElaborarPlan();
    const act3 = this.actividadNormativa();
    const norm = this.normativaDetalle();

    // 1. Si Hito 1.1 está PENDIENTE
    if (act1 && act1.estado === 'PENDIENTE') {
      return {
        codigo: act1.codigoActividad || '1.1',
        etapa: 'Formalización y Cronograma',
        responsable: 'Área de Evaluación y Acreditación (AEA)',
        plazo: '5 días',
        texto:
          'El Área de Evaluación y Acreditación (AEA) debe registrar la reunión de coordinación inicial con la Dirección de Carrera · Plazo estimado: 5 días',
        botonTexto: 'Registrar Reunión y Subir Acta',
        accionTipo: 'REUNION',
        actividad: act1,
      };
    }

    // 2. Si Hito 1.3 está ENVIADA_A_REVISION
    if (norm?.estado === 'ENVIADA_A_REVISION') {
      return {
        codigo: act3?.codigoActividad || '1.3',
        etapa: 'Gestión de Normativa',
        responsable: 'Área de Evaluación y Acreditación (AEA)',
        plazo: 'Auditoría inmediata',
        texto:
          'El Área de Evaluación y Acreditación (AEA) debe auditar la documentación presentada y emitir dictamen',
        botonTexto: 'Revisar y Dictaminar Normativa',
        accionTipo: 'REVISION_NORMATIVA',
        actividad: act3,
      };
    }

    // 3. Si Hito 1.3 está SOLICITADA o EN_CARGA_BORRADOR o OBSERVADA
    if (
      norm?.estado === 'SOLICITADA' ||
      norm?.estado === 'EN_CARGA_BORRADOR' ||
      norm?.estado === 'OBSERVADA'
    ) {
      const cantDocs = norm?.documentosRequeridos || 8;
      const plazo = norm?.badgePlazo?.etiqueta || '5 días hábiles administrativos';
      return {
        codigo: act3?.codigoActividad || '1.3',
        etapa: 'Gestión de Normativa',
        responsable: 'Dirección de Carrera (DC)',
        plazo: plazo,
        badgePlazo: norm?.badgePlazo,
        texto: `La Dirección de Carrera (DC) debe presentar los ${cantDocs} documentos de normativa solicitados por AEA · Plazo: ${plazo}`,
        botonTexto: 'Gestionar Documentación de Normativa',
        accionTipo: 'NORMATIVA',
        actividad: act3,
      };
    }

    // 4. Si Hito 1.2 está PENDIENTE o requiere elaborar plan
    if (act2 && (act2.estado === 'PENDIENTE' || this.requiereElaborarPlan())) {
      return {
        codigo: act2.codigoActividad || '1.2',
        etapa: 'Formalización y Cronograma',
        responsable: 'Área de Evaluación y Acreditación (AEA)',
        plazo: 'En término',
        texto:
          'El Área de Evaluación y Acreditación (AEA) debe formular el cronograma tentativo institucional y metadatos RG-001',
        botonTexto: 'Ajustar Cronograma y RG-001',
        accionTipo: 'PLAN',
        actividad: act2,
      };
    }

    // 5. Si Hito 1.3 aún no fue solicitado por AEA
    if (act3 && act3.estado !== 'COMPLETADA' && (!norm || norm.estado === 'PENDIENTE_SOLICITUD')) {
      return {
        codigo: act3.codigoActividad || '1.3',
        etapa: 'Gestión de Normativa',
        responsable: 'Área de Evaluación y Acreditación (AEA)',
        plazo: 'Inmediato',
        texto:
          'El Área de Evaluación y Acreditación (AEA) debe solicitar formalmente los 8 documentos de normativa a la Dirección de Carrera',
        botonTexto: 'Gestionar Documentación de Normativa',
        accionTipo: 'NORMATIVA',
        actividad: act3,
      };
    }

    // 6. Si Hito 1.3 está completado
    return {
      codigo: 'FASE-SIGUIENTE',
      etapa: 'Comisión Curricular y Docente',
      responsable: 'Comisión Curricular / AEA',
      plazo: 'En curso',
      texto:
        'La gestión de normativa se encuentra aprobada y completada. El proceso continúa con las actividades de la Comisión Curricular y Docente.',
      botonTexto: 'Ver Tablero de Seguimiento',
      accionTipo: 'SEGUIMIENTO',
      actividad: null,
    };
  });

  /**
   * 5 etapas secuenciales (DESIGN.md 7.3)
   */
  readonly etapas = computed<EtapaProcesoInfo[]>(() => {
    const act1 = this.actividadReunion();
    const act2 = this.actividadElaborarPlan();
    const act3 = this.actividadNormativa();
    const norm = this.normativaDetalle();
    const conv = this.convocatoria();

    // Etapa 1: Formalización y Cronograma
    const etapa1Completa =
      act1?.estado === 'COMPLETADA' &&
      act2?.estado === 'COMPLETADA' &&
      !this.requiereElaborarPlan();

    // Etapa 2: Gestión de Normativa
    const etapa2Completa =
      act3?.estado === 'COMPLETADA' || norm?.estado === 'COMPLETADA';

    // Etapa 3: Comisión Curricular y Docente
    const etapa3Completa =
      etapa2Completa && (conv?.estado === 'EN_AUTOEVALUACION' || conv?.estado === 'FINALIZADO');

    // Etapa 4: Autoevaluación y Talleres
    const etapa4Completa = conv?.estado === 'FINALIZADO';

    // Etapa 5: Dictamen y Presentación CONEAU
    const etapa5Completa = conv?.estado === 'FINALIZADO';

    let etapaActivaNumero = 1;
    if (!etapa1Completa) {
      etapaActivaNumero = 1;
    } else if (!etapa2Completa) {
      etapaActivaNumero = 2;
    } else if (!etapa3Completa) {
      etapaActivaNumero = 3;
    } else if (!etapa4Completa) {
      etapaActivaNumero = 4;
    } else {
      etapaActivaNumero = 5;
    }

    return [
      {
        numero: 1,
        nombre: 'Formalización y Cronograma',
        estado: etapa1Completa ? 'COMPLETADA' : etapaActivaNumero === 1 ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 2,
        nombre: 'Gestión de Normativa',
        estado: etapa2Completa ? 'COMPLETADA' : etapaActivaNumero === 2 ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 3,
        nombre: 'Comisión Curricular y Docente',
        estado: etapa3Completa ? 'COMPLETADA' : etapaActivaNumero === 3 ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 4,
        nombre: 'Autoevaluación y Talleres',
        estado: etapa4Completa ? 'COMPLETADA' : etapaActivaNumero === 4 ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 5,
        nombre: 'Dictamen y Presentación CONEAU',
        estado: etapa5Completa ? 'COMPLETADA' : etapaActivaNumero === 5 ? 'ACTIVA' : 'PENDIENTE',
      },
    ];
  });

  ngOnInit(): void {
    if (!this.canManage() && this.tabActiva() === 'SEGUIMIENTO') {
      this.tabActiva.set('ACTIVIDADES');
    }
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.cargarDatos(id);
    }
  }

  cargarDatos(id: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.convocatoriaService.obtenerConvocatoria(id).subscribe({
      next: (conv) => {
        this.convocatoria.set(conv);
        this.convocatoriaService.obtenerPlanTrabajo(id).subscribe({
          next: (plan) => {
            this.planTrabajo.set(plan);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
        // Cargar detalle de normativa reactivo para badges y estado
        this.normativaService.obtenerDetalle(id).subscribe({
          error: () => {
            // Si es 404, no hay solicitud aún y es normal
          },
        });
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'No fue posible cargar el detalle de la convocatoria.');
        this.loading.set(false);
      },
    });
  }

  ejecutarAccionSiguientePaso(paso: SiguientePasoInfo): void {
    switch (paso.accionTipo) {
      case 'REUNION':
        if (paso.actividad) {
          this.abrirModalCompletar(paso.actividad);
        } else if (this.actividadReunion()) {
          this.abrirModalCompletar(this.actividadReunion()!);
        }
        break;
      case 'PLAN':
        this.abrirModalElaborarPlan();
        break;
      case 'NORMATIVA':
      case 'REVISION_NORMATIVA':
        if (this.tabActiva() === 'SEGUIMIENTO') {
          const act = this.actividadNormativa() || paso.actividad;
          if (act) {
            this.abrirDrawer(act);
          }
        } else {
          this.tabActiva.set('ACTIVIDADES');
          this.actividadFoco.set('1.3');
        }
        break;
      case 'SEGUIMIENTO':
        this.tabActiva.set('SEGUIMIENTO');
        break;
    }
  }

  abrirDrawer(act: ActividadPlanTrabajoDto): void {
    this.actividadSeleccionadaDrawer.set(act);
  }

  cerrarDrawer(): void {
    this.actividadSeleccionadaDrawer.set(null);
  }

  abrirModalCompletar(act: ActividadPlanTrabajoDto): void {
    this.actividadSeleccionada.set(act);
  }

  cerrarModal(completada: boolean): void {
    this.actividadSeleccionada.set(null);
    if (completada) {
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.cargarDatos(id);
      }
    }
  }

  abrirModalElaborarPlan(): void {
    this.bannerExitoPlan.set(null);
    this.modalElaborarPlanAbierto.set(true);
  }

  cerrarModalElaborarPlan(guardado: boolean): void {
    this.modalElaborarPlanAbierto.set(false);
    if (guardado) {
      this.bannerExitoPlan.set(
        'El Hito 1.2 (Elaborar Plan de Trabajo) fue completado exitosamente y el cronograma tentativo institucional ha quedado establecido formalmente.'
      );
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.cargarDatos(id);
      }
    }
  }

  cambiarFiltroHoja(hoja: 'TODAS' | 'INFO' | 'CONEAU'): void {
    this.filtroHoja.set(hoja);
  }

  descargar(documentoId: string, nombreArchivo: string): void {
    this.convocatoriaService.descargarDocumento(documentoId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombreArchivo;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert('No fue posible descargar el documento probatorio.');
      },
    });
  }

  seleccionarTab(tab: 'ACTIVIDADES' | 'SEGUIMIENTO' | 'FICHA'): void {
    if (tab === 'SEGUIMIENTO' && !this.canManage()) {
      this.tabActiva.set('ACTIVIDADES');
      return;
    }
    this.tabActiva.set(tab);
  }

  formatearEstadoSolicitud(estado?: string | null): string {
    return formatearEstadoNormativa(estado);
  }

  seleccionarActividadFoco(codigo: string): void {
    this.actividadFoco.set(codigo);
  }

  confirmarReinicio(): void {
    const c = this.convocatoria();
    if (!c) return;
    const ok = window.confirm(
      `¿Está seguro de que desea reiniciar la convocatoria "${c.codigo}"?\n\n` +
      `Esta acción eliminará todas las actividades del plan de trabajo, las solicitudes de normativa y los documentos cargados para permitir comenzar el procedimiento completamente desde cero.`
    );
    if (!ok) return;

    this.reiniciando.set(true);
    this.convocatoriaService.eliminarConvocatoria(c.id).subscribe({
      next: () => {
        this.reiniciando.set(false);
        this.router.navigate(['/acreditacion/convocatorias']);
      },
      error: (err) => {
        this.reiniciando.set(false);
        alert(err?.error?.error || 'No fue posible reiniciar la convocatoria.');
      },
    });
  }
}
