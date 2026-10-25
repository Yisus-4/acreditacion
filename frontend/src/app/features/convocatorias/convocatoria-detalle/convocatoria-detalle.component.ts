import { Component, computed, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Auth } from '../../../core/auth/auth';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import {
  ActividadPlanTrabajoDto,
  AREAS_INFORMADAS_LABELS,
  ConvocatoriaDetalle,
  DOCUMENTACION_REMITIDA_LABELS,
  DocumentoInteresDto,
  FichaFormalizacionFt012Dto,
  FUENTES_DOCUMENTACION_INTERES_CATALOGO,
  GuardarCronogramaPlanRequest,
  GuardarRg001Request,
  ItemActividadPlanRequest,
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
  foco: string;
  hoja: 'INFO' | 'CONEAU';
  nombre: string;
  estado: 'COMPLETADA' | 'ACTIVA' | 'PENDIENTE';
}

@Component({
  selector: 'app-convocatoria-detalle',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CompletarActividadModalComponent,
    ElaborarPlanTrabajoModalComponent,
    NormativaDetalleComponent,
  ],
  templateUrl: './convocatoria-detalle.component.html',
  styleUrls: ['./convocatoria-detalle.component.scss'],
})
export class ConvocatoriaDetalleComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly convocatoriaService = inject(ConvocatoriaService);
  private readonly normativaService = inject(NormativaService);
  private readonly auth = inject(Auth);
  private routeSub?: Subscription;
  private idCargadoActualmente: string | null = null;

  readonly today = new Date().toISOString().split('T')[0];
  readonly fuentesInteresCatalogo = FUENTES_DOCUMENTACION_INTERES_CATALOGO;

  readonly convocatoria = signal<ConvocatoriaDetalle | null>(null);
  readonly planTrabajo = signal<PlanTrabajoDto | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly tabActiva = signal<'ACTIVIDADES' | 'PLAN_TRABAJO' | 'SEGUIMIENTO' | 'FICHA'>('ACTIVIDADES');
  readonly actividadFoco = signal<string>('1.1');
  private focoSeleccionadoManualmente = false;
  readonly reiniciando = signal<boolean>(false);

  readonly actividadSeleccionada = signal<ActividadPlanTrabajoDto | null>(null);
  readonly actividadSeleccionadaDrawer = signal<ActividadPlanTrabajoDto | null>(null);
  readonly modalElaborarPlanAbierto = signal<boolean>(false);
  readonly bannerExitoPlan = signal<string | null>(null);
  readonly filtroHoja = signal<'TODAS' | 'INFO' | 'CONEAU'>('TODAS');
  readonly filtroEstado = signal<string>('TODOS');
  readonly busquedaTexto = signal<string>('');

  // Estado Convocatoria de Escuela (Selección y Confirmación del Director de Escuela)
  readonly carrerasSeleccionadasDe = signal<Set<string>>(new Set<string>());
  readonly guardandoConfirmacionDe = signal<boolean>(false);
  readonly errorConfirmacionDe = signal<string | null>(null);

  readonly esConvocatoriaEscuela = computed(() => {
    const c = this.convocatoria();
    if (!c) return false;
    // Si la entidad tiene carreraId, es indudablemente el proceso de una carrera individual
    if (c.carreraId) return false;
    // Convocatoria a nivel de Escuela
    return (
      !!c.unidadAcademicaId ||
      (c.carrerasPropuestas && c.carrerasPropuestas.length > 0) ||
      c.estado === 'PENDIENTE_DIRECTOR_ESCUELA' ||
      c.estado === 'CONFIRMADA_DIRECTOR_ESCUELA'
    );
  });

  readonly carrerasPropuestas = computed(() => {
    return this.convocatoria()?.carrerasPropuestas ?? [];
  });

  readonly carrerasConfirmadasDe = computed(() => {
    return (this.convocatoria()?.carrerasPropuestas ?? []).filter((item) => item.seleccionadaPorDe);
  });

  readonly carrerasNoConfirmadasDe = computed(() => {
    return (this.convocatoria()?.carrerasPropuestas ?? []).filter((item) => !item.seleccionadaPorDe);
  });

  readonly cantidadSeleccionadasDe = computed(() => {
    return this.carrerasSeleccionadasDe().size;
  });

  // Estado Actividad 4 (Documentación de interés)
  readonly documentosAct4 = signal<DocumentoInteresDto[]>([]);
  readonly archivoAct4 = signal<File | null>(null);
  readonly clasificacionAct4 = signal<string>('DOC_PROPIA_AREA');
  readonly descripcionAct4 = signal<string>('');
  readonly subiendoDocAct4 = signal<boolean>(false);
  readonly fuentesSeleccionadasAct4 = signal<string[]>([
    'DOC_PROPIA_AREA',
    'PROCESOS_ACREDITACION_CARRERAS',
    'EVALUACION_FUNCIONES',
    'EVALUACION_INSTITUCIONAL',
  ]);
  readonly observacionesAct4 = signal<string>('');
  readonly guardandoAct4 = signal<boolean>(false);

  // Estado Actividad 5 (Formalización electrónica FT-012 + CONEAU Global ATENEA)
  readonly fichaFt012 = signal<FichaFormalizacionFt012Dto>({
    procesoId: '',
    modalidadDictado: 'PRESENCIAL',
    estado: 'PENDIENTE_SOLICITUD',
    institucionUniversitaria: 'Universidad Nacional de Chilecito',
    autoridadApellido: 'Salcedo',
    autoridadPrimerNombre: 'César',
    autoridadSegundoNombre: 'Alberto',
    autoridadCorreo: 'csalcedo@undec.edu.ar',
    autoridadTelefono: '',
    unidadAcademica: 'Universidad Nacional de Chilecito',
    sedeCalle: '9 de Julio',
    sedeNumero: '22',
    sedePiso: '',
    sedeDepartamento: '',
    sedeCodigoPostal: '5360',
    sedeProvincia: 'La Rioja',
    sedeLocalidad: 'Chilecito',
    sedeDepartamentoPartido: 'Chilecito',
    nexoApellido: '',
    nexoPrimerNombre: '',
    nexoSegundoNombre: '',
    nexoTelefono: '',
    nexoCargo: '',
    nexoCorreo: '',
    carreraNombre: '',
    tituloAsimilado: '',
    cargaHorariaNoPresencial: '',
    anioInicioDictado: null,
    responsableApellido: '',
    responsablePrimerNombre: '',
    responsableSegundoNombre: '',
    responsableTelefono: '',
    responsableCorreo: '',
    responsableCargo: '',
    coordinadorCaApellido: '',
    coordinadorCaPrimerNombre: '',
    coordinadorCaSegundoNombre: '',
    coordinadorCaTelefono: '',
    coordinadorCaCorreo: '',
    observaciones: '',
  });
  readonly solicitandoFt012 = signal<boolean>(false);
  readonly guardandoFt012 = signal<boolean>(false);
  readonly fechaFormalizacionAct6 = signal<string>(new Date().toISOString().split('T')[0]);
  readonly archivoComprobanteAct6 = signal<File | null>(null);
  readonly observacionesFormalizacionAct6 = signal<string>('');
  readonly formalizandoAct6 = signal<boolean>(false);

  readonly normativaDetalle = this.normativaService.detalle;

  // =========================================================================
  // Estado Pestaña 2: Plan de Trabajo y Formulario RG-001
  // =========================================================================
  readonly rg001Data = signal<GuardarRg001Request>({
    nroResolucionConeau: '',
    fechaInicio: '',
    fechaCierre: '',
    fechaLimitePresentacionConeau: '',
    rmEstandares: '',
    areaDisciplinaria: 'Informática / Sistemas',
    modalidadCarrera: 'PRESENCIAL',
    estadoCarrera: 'EN_FUNCIONAMIENTO',
    faseEvaluacion: 'PRIMERA_FASE',
    observaciones: '',
  });
  readonly guardandoRg001Borrador = signal<boolean>(false);
  readonly enviandoRg001 = signal<boolean>(false);

  readonly cronogramaActividades = signal<ItemActividadPlanRequest[]>([]);
  readonly fechaLimiteConeauPlan = signal<string>('');
  readonly cargandoPlantillaPlan = signal<boolean>(false);
  readonly guardandoPlanBorrador = signal<boolean>(false);
  readonly enviandoPlan = signal<boolean>(false);
  readonly finalizandoActividadPlan = signal<boolean>(false);
  readonly filtroHojaPlan = signal<'TODAS' | 'INFO' | 'CONEAU'>('TODAS');

  readonly areasDisciplinariasSugeridas: string[] = [
    'Ingeniería',
    'Informática / Sistemas',
    'Ciencias de la Salud',
    'Ciencias Agrarias',
    'Ciencias Aplicadas',
    'Ciencias Económicas',
    'Ciencias Básicas',
    'Ciencias Humanas y Sociales',
  ];

  readonly rolesDisponibles: string[] = [
    'Área de Evaluación y Acreditación (AEA)',
    'Dirección de Carrera',
    'Comisión Curricular',
    'Secretaría Académica / Técnica',
    'Comité de Autoevaluación',
    'Docentes / Proveedores de Información',
    'Autoridad Institucional',
  ];

  readonly planTrabajoDesbloqueado = computed(() => {
    const ft = this.fichaFt012();
    const ftOk = ft.estado === 'REMITIDA_POR_DC' || ft.estado === 'FORMALIZADA_EN_CONEAU' || !!ft.remitidoEn;
    const norm = this.normativaDetalle();
    const normOk = norm?.estado === 'COMPLETADA';
    return ftOk && normOk;
  });

  readonly estadoRg001 = computed(() => {
    return this.convocatoria()?.registroConvocatoria?.estadoRg001 || 'BORRADOR';
  });

  readonly esRg001Enviado = computed(() => {
    return this.estadoRg001() === 'ENVIADO';
  });

  readonly puedeEditarRg001 = computed(() => {
    if (!this.planTrabajoDesbloqueado()) return false;
    if (!this.esRg001Enviado()) return true;
    return this.canManage();
  });

  readonly estadoEnvioPlan = computed(() => {
    return this.planTrabajo()?.estadoEnvio || this.convocatoria()?.planTrabajo?.estadoEnvio || 'BORRADOR';
  });

  readonly esPlanEnviado = computed(() => {
    return this.estadoEnvioPlan() === 'ENVIADO';
  });

  readonly puedeEditarPlan = computed(() => {
    if (!this.planTrabajoDesbloqueado()) return false;
    if (!this.esPlanEnviado()) return true;
    return this.canManage();
  });

  readonly actividadPlanFinalizada = computed(() => {
    const act = this.actividadElaborarPlan();
    return act?.estado === 'COMPLETADA';
  });

  readonly puedeFinalizarActividadPlan = computed(() => {
    return this.esRg001Enviado() && this.esPlanEnviado() && !this.actividadPlanFinalizada();
  });

  readonly conteoPlanInfo = computed(() => {
    return this.cronogramaActividades().filter((a) => a.hoja === 'INFO').length;
  });

  readonly conteoPlanConeau = computed(() => {
    return this.cronogramaActividades().filter((a) => a.hoja === 'CONEAU').length;
  });

  readonly conteoPlanTotal = computed(() => {
    return this.cronogramaActividades().length;
  });

  readonly cronogramaActividadesFiltradas = computed(() => {
    const hoja = this.filtroHojaPlan();
    const acts = this.cronogramaActividades();
    if (hoja === 'TODAS') return acts;
    return acts.filter((a) => a.hoja === hoja);
  });

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

  readonly esDirectorEscuela = computed(() => {
    const session = this.auth.currentSession;
    if (!session?.user?.roleCodes) return false;
    return session.user.roleCodes.includes('SCHOOL_DIRECTOR');
  });

  readonly canConfirmarDe = computed(() => {
    const session = this.auth.currentSession;
    if (!session?.user?.roleCodes) return false;
    return (
      session.user.roleCodes.includes('ADMINISTRATOR') ||
      session.user.roleCodes.includes('AEA') ||
      session.user.roleCodes.includes('SCHOOL_DIRECTOR')
    );
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

  readonly actividadDocumentacionInteres = computed(() => {
    const acts = this.planTrabajo()?.actividades ?? [];
    return (
      acts.find(
        (a) =>
          a.codigoActividad === 'ACT-04-IDENTIFICAR-DOCUMENTACION-INTERES' ||
          a.codigoActividad === '1.5' ||
          a.nombre.toLowerCase().includes('documentación de interés')
      ) || null
    );
  });

  readonly actividad2Completa = computed(() => {
    const actNorm = this.actividadNormativa();
    const norm = this.normativaDetalle();
    return actNorm?.estado === 'COMPLETADA' || norm?.estado === 'COMPLETADA';
  });

  readonly actividadFormalizacionElectronica = computed(() => {
    const acts = this.planTrabajo()?.actividades ?? [];
    return (
      acts.find(
        (a) =>
          a.codigoActividad === 'ACT-06-FORMALIZACION-ELECTRONICA' ||
          a.codigoActividad === '1.8' ||
          a.nombre.toLowerCase().includes('formalización electrónica')
      ) || null
    );
  });

  readonly actividadesFiltradas = computed(() => {
    const hoja = this.filtroHoja();
    const estado = this.filtroEstado();
    const busqueda = this.busquedaTexto().trim().toLowerCase();
    const acts = this.planTrabajo()?.actividades ?? [];

    return acts.filter((a) => {
      if (hoja !== 'TODAS' && (a.hoja || 'INFO') !== hoja) {
        return false;
      }
      if (estado !== 'TODOS' && a.estado !== estado) {
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
    return !act2 || act2.estado !== 'COMPLETADA';
  });

  /**
   * 5 actividades oficiales en orden del flujo (Actividad 1 a Actividad 5):
   * 1: Informar sobre el proceso de acreditación (1.1)
   * 2: Gestionar normativa de la carrera (1.3)
   * 3: Elaborar Plan de Trabajo para acreditación de carreras y RG-001 (1.2)
   * 4: Identificar información y documentación de interés (1.5)
   * 5: Formalización electrónica de participación en convocatoria (1.8)
   */
  readonly etapas = computed<EtapaProcesoInfo[]>(() => {
    const actPlan = this.actividadElaborarPlan();
    const act1 = this.actividadReunion();
    const actNorm = this.actividadNormativa();
    const act4 = this.actividadDocumentacionInteres();
    const act5 = this.actividadFormalizacionElectronica();
    const norm = this.normativaDetalle();
    const ft012 = this.fichaFt012();

    const planCompletado = actPlan?.estado === 'COMPLETADA' && !this.requiereElaborarPlan();
    const act1Completa = act1?.estado === 'COMPLETADA';
    const act2Completa = actNorm?.estado === 'COMPLETADA' || norm?.estado === 'COMPLETADA';
    const act4Completa = act4?.estado === 'COMPLETADA';
    const act5Completa = act5?.estado === 'COMPLETADA' || ft012?.estado === 'FORMALIZADA_EN_CONEAU';

    const focoActual = this.actividadFoco();

    return [
      {
        numero: 1,
        foco: '1.1',
        hoja: 'INFO',
        nombre: 'Informar sobre el proceso de acreditación',
        estado: act1Completa ? 'COMPLETADA' : focoActual === '1.1' ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 2,
        foco: '1.3',
        hoja: 'INFO',
        nombre: 'Gestionar normativa de la carrera',
        estado: act2Completa ? 'COMPLETADA' : focoActual === '1.3' ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 3,
        foco: '1.2',
        hoja: 'INFO',
        nombre: 'Elaborar Plan de Trabajo para acreditación de carreras y RG-001',
        estado: planCompletado ? 'COMPLETADA' : focoActual === '1.2' ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 4,
        foco: '1.5',
        hoja: 'INFO',
        nombre: 'Identificar información y documentación de interés',
        estado: act4Completa ? 'COMPLETADA' : focoActual === '1.5' ? 'ACTIVA' : 'PENDIENTE',
      },
      {
        numero: 5,
        foco: '1.8',
        hoja: 'CONEAU',
        nombre: 'Formalización electrónica de participación en convocatoria',
        estado: act5Completa ? 'COMPLETADA' : focoActual === '1.8' ? 'ACTIVA' : 'PENDIENTE',
      },
    ];
  });

  ngOnInit(): void {
    if (!this.canManage() && this.tabActiva() === 'SEGUIMIENTO') {
      this.tabActiva.set('ACTIVIDADES');
    }
    this.routeSub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.cargarDatos(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  irAFormalizar(procesoId: string): void {
    this.cargarDatos(procesoId);
    void this.router.navigate(['/acreditacion/convocatorias', procesoId]);
  }

  cargarDatos(id: string): void {
    if (this.idCargadoActualmente === id && this.convocatoria()) {
      return;
    }
    this.idCargadoActualmente = id;
    this.loading.set(true);
    this.error.set(null);
    this.convocatoria.set(null);
    this.planTrabajo.set(null);

    this.convocatoriaService.obtenerConvocatoria(id).subscribe({
      next: (conv) => {
        this.convocatoria.set(conv);

        // Si es una convocatoria a nivel de Escuela (con carreras propuestas)
        if (conv.carrerasPropuestas && conv.carrerasPropuestas.length > 0) {
          this.initSeleccionDirectorEscuela(conv);
          this.loading.set(false);
          return;
        }

        // Si es un proceso individual de acreditación de carrera
        this.convocatoriaService.obtenerPlanTrabajo(id).subscribe({
          next: (plan) => {
            this.planTrabajo.set(plan);
            this.sincronizarEstadosActividades456();
            this.actualizarFocoPorDefecto();
            this.inicializarRg001YPlan(conv, plan);
            this.loading.set(false);
          },
          error: () => {
            this.inicializarRg001YPlan(conv, null);
            this.loading.set(false);
          },
        });
        this.normativaService.obtenerDetalle(id).subscribe({
          next: () => {
            this.actualizarFocoPorDefecto();
            if (!this.rg001Data().rmEstandares) {
              const normDocs = this.normativaDetalle()?.documentos || [];
              const rmDoc = normDocs.find((d) => d.nombreOriginal.toLowerCase().includes('rm'));
              if (rmDoc) {
                this.actualizarCampoRg001('rmEstandares', rmDoc.nombreOriginal);
              }
            }
          },
          error: () => this.actualizarFocoPorDefecto(),
        });
        this.cargarFichaFormalizacion(id);
      },
      error: (err) => {
        this.idCargadoActualmente = null;
        this.error.set(err?.error?.error || 'No fue posible cargar el detalle de la convocatoria.');
        this.loading.set(false);
      },
    });
  }

  initSeleccionDirectorEscuela(conv: ConvocatoriaDetalle): void {
    const seleccionadas = new Set<string>();
    for (const item of conv.carrerasPropuestas ?? []) {
      if (conv.estado === 'CONFIRMADA_DIRECTOR_ESCUELA') {
        if (item.seleccionadaPorDe) {
          seleccionadas.add(item.carreraId);
        }
      } else {
        // En estado PENDIENTE_DIRECTOR_ESCUELA:
        // Las obligatorias van marcadas por regla institucional,
        // y las seleccionadas por AEA se pre-marcan como sugerencia.
        if (item.esObligatoria || item.seleccionadaPorAea) {
          seleccionadas.add(item.carreraId);
        }
      }
    }
    this.carrerasSeleccionadasDe.set(seleccionadas);
  }

  toggleCarreraDe(carreraId: string, esObligatoria: boolean): void {
    if (esObligatoria) {
      // Las carreras obligatorias no pueden ser desmarcadas
      return;
    }
    const current = new Set(this.carrerasSeleccionadasDe());
    if (current.has(carreraId)) {
      current.delete(carreraId);
    } else {
      current.add(carreraId);
    }
    this.carrerasSeleccionadasDe.set(current);
  }

  estaCarreraSeleccionadaDe(carreraId: string): boolean {
    return this.carrerasSeleccionadasDe().has(carreraId);
  }

  confirmarSeleccionDirectorEscuela(): void {
    const conv = this.convocatoria();
    if (!conv) return;

    const seleccionadas = Array.from(this.carrerasSeleccionadasDe());

    // Validar que todas las obligatorias estén incluidas
    for (const item of conv.carrerasPropuestas ?? []) {
      if (item.esObligatoria && !seleccionadas.includes(item.carreraId)) {
        this.errorConfirmacionDe.set(`La carrera obligatoria "${item.carreraNombre}" debe incluirse obligatoriamente.`);
        return;
      }
    }

    if (seleccionadas.length === 0) {
      this.errorConfirmacionDe.set('Debe seleccionar al menos una carrera para enviar a acreditar.');
      return;
    }

    this.guardandoConfirmacionDe.set(true);
    this.errorConfirmacionDe.set(null);

    this.convocatoriaService.confirmarConvocatoriaDe(conv.id, seleccionadas).subscribe({
      next: (actualizada) => {
        this.convocatoria.set(actualizada);
        this.initSeleccionDirectorEscuela(actualizada);
        this.guardandoConfirmacionDe.set(false);
        this.bannerExitoPlan.set('¡Carreras confirmadas exitosamente por la Dirección de Escuela! Se han iniciado los procesos de acreditación para cada carrera seleccionada.');
      },
      error: (err) => {
        this.errorConfirmacionDe.set(err?.error?.error || 'No fue posible confirmar la selección de carreras.');
        this.guardandoConfirmacionDe.set(false);
      },
    });
  }

  private sincronizarEstadosActividades456(): void {
    const act4 = this.actividadDocumentacionInteres();
    if (act4) {
      this.documentosAct4.set(act4.documentos ?? []);
      if (act4.documentacionRemitida && act4.documentacionRemitida.length > 0) {
        this.fuentesSeleccionadasAct4.set([...act4.documentacionRemitida]);
      }
      if (act4.observaciones || act4.sintesisAcuerdos) {
        this.observacionesAct4.set(act4.observaciones || act4.sintesisAcuerdos || '');
      }
    }
  }

  cargarFichaFormalizacion(procesoId: string): void {
    this.convocatoriaService.obtenerFormalizacion(procesoId).subscribe({
      next: (ficha) => {
        this.fichaFt012.set({
          ...this.fichaFt012(),
          ...ficha,
          autoridadSegundoNombre: ficha.autoridadSegundoNombre ?? '',
          sedePiso: ficha.sedePiso ?? '',
          sedeDepartamento: ficha.sedeDepartamento ?? '',
          responsableCargo: ficha.responsableCargo || '',
        });
        if (ficha.fechaFormalizacionElectronica) {
          this.fechaFormalizacionAct6.set(ficha.fechaFormalizacionElectronica.split('T')[0]);
        }
        if (ficha.observaciones) {
          this.observacionesFormalizacionAct6.set(ficha.observaciones);
        }
        this.actualizarFocoPorDefecto();
      },
      error: () => {},
    });
  }

  private actualizarFocoPorDefecto(): void {
    if (this.focoSeleccionadoManualmente) {
      return;
    }
    const actPlan = this.actividadElaborarPlan();
    const act1 = this.actividadReunion();
    const actNorm = this.actividadNormativa();
    const act4 = this.actividadDocumentacionInteres();
    const act5 = this.actividadFormalizacionElectronica();
    const norm = this.normativaDetalle();
    const ft012 = this.fichaFt012();

    const planCompletado = actPlan?.estado === 'COMPLETADA' && !this.requiereElaborarPlan();
    const act1Completa = act1?.estado === 'COMPLETADA';
    const act2Completa = actNorm?.estado === 'COMPLETADA' || norm?.estado === 'COMPLETADA';
    const act4Completa = act4?.estado === 'COMPLETADA';
    const act5Completa = act5?.estado === 'COMPLETADA' || ft012?.estado === 'FORMALIZADA_EN_CONEAU';

    // Orden del flujo: 1.1 -> 1.3 -> 1.2 -> 1.5 -> 1.8
    if (!act1Completa) {
      this.actividadFoco.set('1.1');
    } else if (!act2Completa) {
      this.actividadFoco.set('1.3');
    } else if (!planCompletado) {
      this.actividadFoco.set('1.2');
    } else if (!act4Completa) {
      this.actividadFoco.set('1.5');
    } else if (!act5Completa) {
      this.actividadFoco.set('1.8');
    } else {
      this.actividadFoco.set('1.1');
    }
  }

  // =========================================================================
  // Helpers para la Tabla de Seguimiento del Plan de Trabajo (PT-001)
  // =========================================================================

  esActividadNormativa(act: ActividadPlanTrabajoDto): boolean {
    return act.codigoActividad === '1.3' || act.nombre.toLowerCase().includes('normativa');
  }

  esActividadFormalizacion(act: ActividadPlanTrabajoDto): boolean {
    return (
      act.codigoActividad === '1.8' ||
      act.codigoActividad === 'ACT-06-FORMALIZACION-ELECTRONICA' ||
      act.nombre.toLowerCase().includes('formalización electrónica')
    );
  }

  esActividadPlanTrabajo(act: ActividadPlanTrabajoDto): boolean {
    return (
      act.codigoActividad === '1.2' ||
      act.codigoActividad === 'ACT-02-ELABORAR-PLAN-TRABAJO' ||
      act.nombre.toLowerCase().includes('plan de trabajo')
    );
  }

  esActividadInformar(act: ActividadPlanTrabajoDto): boolean {
    return (
      act.codigoActividad === '1.1' ||
      act.codigoActividad === 'ACT-01-INFORMAR-PROCESO' ||
      act.codigoActividad === 'ACT-01-REUNION-COORDINACION' ||
      (act.nombre.toLowerCase().includes('informar') && act.hoja === 'INFO')
    );
  }

  esActividadDocumentacionInteres(act: ActividadPlanTrabajoDto): boolean {
    return (
      act.codigoActividad === '1.5' ||
      act.codigoActividad === 'ACT-04-IDENTIFICAR-DOCUMENTACION-INTERES' ||
      act.nombre.toLowerCase().includes('documentación de interés')
    );
  }

  tieneDocumentosNormativa(): boolean {
    return (this.normativaDetalle()?.documentos?.length ?? 0) > 0;
  }

  tieneComprobanteFormalizacion(): boolean {
    const ft = this.fichaFt012();
    return Boolean(ft.comprobanteStoragePath || ft.comprobanteNombre);
  }

  obtenerTextoObservaciones(act: ActividadPlanTrabajoDto): string {
    if (this.esActividadInformar(act)) {
      const parts: string[] = [];
      if (act.fechaReunionCoordinacion) {
        const fecha = new Date(act.fechaReunionCoordinacion).toLocaleDateString('es-AR');
        parts.push(`Reunión con DC: ${fecha}`);
      }
      if (act.observaciones || act.sintesisAcuerdos) {
        parts.push(act.observaciones || act.sintesisAcuerdos || '');
      }
      return parts.length > 0 ? parts.join(' · ') : '—';
    }
    if (this.esActividadNormativa(act)) {
      const norm = this.normativaDetalle();
      const estado = norm?.estado ? this.formatearEstadoSolicitud(norm.estado) : act.estado;
      const count = norm?.documentos?.length ?? 0;
      let text = `${estado} (${count} documentos cargados)`;
      if (norm?.observacionesRevision) {
        text += ` · ${norm.observacionesRevision}`;
      }
      return text;
    }
    if (this.esActividadPlanTrabajo(act)) {
      return act.estado === 'COMPLETADA' && !this.requiereElaborarPlan()
        ? 'Cronograma institucional establecido y RG-001 formalizado'
        : 'Pendiente de formulación (Actividad 3)';
    }
    if (this.esActividadDocumentacionInteres(act)) {
      const parts: string[] = [];
      if (act.documentacionRemitida && act.documentacionRemitida.length > 0) {
        parts.push(`Fuentes: ${act.documentacionRemitida.map((c) => this.formatearDocRemitida(c)).join(', ')}`);
      }
      if (act.observaciones || act.sintesisAcuerdos) {
        parts.push(act.observaciones || act.sintesisAcuerdos || '');
      }
      return parts.length > 0 ? parts.join(' · ') : '—';
    }
    if (this.esActividadFormalizacion(act)) {
      const ft = this.fichaFt012();
      if (ft.fechaFormalizacionElectronica) {
        const fecha = new Date(ft.fechaFormalizacionElectronica).toLocaleDateString('es-AR');
        return `Formalizada en CONEAU Global el ${fecha}${ft.observaciones ? ' · ' + ft.observaciones : ''}`;
      }
      return `Estado FT-012: ${this.formatearEstadoFt012(ft.estado)}`;
    }
    return act.observaciones || act.sintesisAcuerdos || '—';
  }

  formatearAreaInformada(codigo: string): string {
    return AREAS_INFORMADAS_LABELS[codigo] || codigo;
  }

  formatearDocRemitida(codigo: string): string {
    return DOCUMENTACION_REMITIDA_LABELS[codigo] || codigo;
  }

  // =========================================================================
  // Acciones Actividad 4 (Documentación de interés)
  // =========================================================================

  onArchivoAct4Selected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.archivoAct4.set(input.files[0]);
    } else {
      this.archivoAct4.set(null);
    }
  }

  subirDocumentoAct4(): void {
    const c = this.convocatoria();
    const act4 = this.actividadDocumentacionInteres();
    const file = this.archivoAct4();
    if (!c || !act4 || !file) return;

    this.subiendoDocAct4.set(true);
    this.convocatoriaService
      .subirDocumentoInteres(c.id, act4.id, file, this.clasificacionAct4(), this.descripcionAct4())
      .subscribe({
        next: (nuevoDoc) => {
          this.documentosAct4.update((prev) => [...prev, nuevoDoc]);
          this.archivoAct4.set(null);
          this.descripcionAct4.set('');
          this.subiendoDocAct4.set(false);
        },
        error: (err) => {
          this.subiendoDocAct4.set(false);
          alert(err?.error?.error || 'No fue posible adjuntar el documento de interés.');
        },
      });
  }

  eliminarDocumentoAct4(doc: DocumentoInteresDto): void {
    const c = this.convocatoria();
    const act4 = this.actividadDocumentacionInteres();
    if (!c || !act4) return;

    this.convocatoriaService.eliminarDocumentoInteres(c.id, act4.id, doc.id).subscribe({
      next: () => {
        this.documentosAct4.update((prev) => prev.filter((d) => d.id !== doc.id));
      },
      error: (err) => {
        alert(err?.error?.error || 'No fue posible eliminar el documento.');
      },
    });
  }

  descargarDocumentoAct4(doc: DocumentoInteresDto): void {
    const c = this.convocatoria();
    const act4 = this.actividadDocumentacionInteres();
    if (!c || !act4) return;

    this.convocatoriaService.descargarDocumentoInteres(c.id, act4.id, doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.nombreOriginal || 'Documento-Interes.pdf';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => alert('No fue posible descargar el documento.'),
    });
  }

  tieneFuenteAct4(codigo: string): boolean {
    return this.fuentesSeleccionadasAct4().includes(codigo);
  }

  toggleFuenteAct4(codigo: string): void {
    const actuales = this.fuentesSeleccionadasAct4();
    if (actuales.includes(codigo)) {
      this.fuentesSeleccionadasAct4.set(actuales.filter((c) => c !== codigo));
    } else {
      this.fuentesSeleccionadasAct4.set([...actuales, codigo]);
    }
  }

  completarActividad4(): void {
    const c = this.convocatoria();
    const act4 = this.actividadDocumentacionInteres();
    if (!c || !act4) return;
    if (this.fuentesSeleccionadasAct4().length === 0) {
      alert('Debe seleccionar al menos una fuente de información y documentación de interés.');
      return;
    }

    this.guardandoAct4.set(true);
    this.convocatoriaService
      .completarDocumentacionInteres(c.id, act4.id, {
        fuentesSeleccionadas: this.fuentesSeleccionadasAct4(),
        observaciones: this.observacionesAct4(),
      })
      .subscribe({
        next: () => {
          this.guardandoAct4.set(false);
          this.bannerExitoPlan.set(
            'La Actividad 4 (Identificar información y documentación de interés) fue registrada y completada exitosamente en la Hoja INFO.'
          );
          this.cargarDatos(c.id);
        },
        error: (err) => {
          this.guardandoAct4.set(false);
          alert(err?.error?.error || 'No fue posible completar la Actividad 4.');
        },
      });
  }

  descargarDocumentoNormativa(docId: string, nombreArchivo: string): void {
    const c = this.convocatoria();
    if (!c) return;
    this.normativaService.descargarDocumento(c.id, docId, nombreArchivo);
  }

  // =========================================================================
  // Acciones Actividad 5 (Formalización electrónica FT-012 + CONEAU Global)
  // =========================================================================

  actualizarCampoFt012(campo: keyof FichaFormalizacionFt012Dto, valor: any): void {
    this.fichaFt012.update((prev) => ({
      ...prev,
      [campo]: valor,
    }));
  }

  solicitarFt012ADireccionCarrera(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.solicitandoFt012.set(true);
    this.convocatoriaService.solicitarFormalizacion(c.id).subscribe({
      next: (ficha) => {
        this.solicitandoFt012.set(false);
        this.fichaFt012.set(ficha);
        this.bannerExitoPlan.set(
          'Se ha solicitado formalmente a la Dirección de Carrera completar la Ficha de Formalización Electrónica (FT-012).'
        );
        this.cargarDatos(c.id);
      },
      error: (err) => {
        this.solicitandoFt012.set(false);
        alert(err?.error?.error || 'No fue posible solicitar la ficha FT-012.');
      },
    });
  }

  guardarORemitirFt012(enviar: boolean): void {
    const c = this.convocatoria();
    if (!c) return;

    this.guardandoFt012.set(true);
    this.convocatoriaService
      .guardarFichaFormalizacion(c.id, {
        ...this.fichaFt012(),
        enviar,
      })
      .subscribe({
        next: (ficha) => {
          this.guardandoFt012.set(false);
          this.fichaFt012.set(ficha);
          this.bannerExitoPlan.set(
            enviar
              ? 'La Ficha de Formalización Electrónica (FT-012) ha sido remitida completa al Área de Evaluación y Acreditación (AEA).'
              : 'El borrador de la Ficha FT-012 fue guardado correctamente.'
          );
          this.cargarDatos(c.id);
        },
        error: (err) => {
          this.guardandoFt012.set(false);
          alert(err?.error?.error || 'No fue posible guardar la Ficha FT-012.');
        },
      });
  }

  onArchivoComprobanteAct6Selected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.archivoComprobanteAct6.set(input.files[0]);
    } else {
      this.archivoComprobanteAct6.set(null);
    }
  }

  registrarFormalizacionConeauGlobal(): void {
    const c = this.convocatoria();
    const file = this.archivoComprobanteAct6();
    if (!c) return;
    if (!this.fechaFormalizacionAct6()) {
      alert('La fecha de formalización electrónica es obligatoria.');
      return;
    }
    if (!file) {
      alert('Debe adjuntar el Comprobante PDF de formalización electrónica generado desde CONEAU Global ATENEA.');
      return;
    }

    this.formalizandoAct6.set(true);
    this.convocatoriaService
      .formalizarEnConeau(c.id, {
        archivo: file,
        fechaFormalizacionElectronica: this.fechaFormalizacionAct6(),
        observaciones: this.observacionesFormalizacionAct6(),
      })
      .subscribe({
        next: (ficha) => {
          this.formalizandoAct6.set(false);
          this.archivoComprobanteAct6.set(null);
          this.fichaFt012.set(ficha);
          this.bannerExitoPlan.set(
            'La Formalización Electrónica en CONEAU Global ATENEA fue registrada exitosamente, la Fecha de Formalización fue actualizada en el RG-001 y se notificó el comprobante PDF a la Dirección de Carrera.'
          );
          this.cargarDatos(c.id);
        },
        error: (err) => {
          this.formalizandoAct6.set(false);
          alert(err?.error?.error || 'No fue posible registrar la formalización en CONEAU Global.');
        },
      });
  }

  descargarComprobanteConeau(): void {
    const c = this.convocatoria();
    if (!c) return;

    this.convocatoriaService.descargarComprobanteFormalizacion(c.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.fichaFt012().comprobanteNombre || 'Comprobante_Formalizacion_CONEAU_Global.pdf';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert('No fue posible descargar el comprobante de formalización electrónica.');
      },
    });
  }

  // =========================================================================
  // Navegación y Modales Generales
  // =========================================================================

  abrirDrawer(act: ActividadPlanTrabajoDto): void {
    this.actividadSeleccionadaDrawer.set(act);
  }

  cerrarDrawer(): void {
    this.actividadSeleccionadaDrawer.set(null);
  }

  irAGestionarActividadDesdeDrawer(act: ActividadPlanTrabajoDto): void {
    this.cerrarDrawer();
    this.tabActiva.set('ACTIVIDADES');
    if (act.codigoActividad === 'ACT-04-IDENTIFICAR-DOCUMENTACION-INTERES' || act.codigoActividad === '1.5') {
      this.seleccionarActividadFoco('1.5');
    } else if (act.codigoActividad === 'ACT-06-FORMALIZACION-ELECTRONICA' || act.codigoActividad === '1.8') {
      this.seleccionarActividadFoco('1.8');
    }
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
        'La Actividad 3 (Elaborar Plan de Trabajo para acreditación de carreras) fue completada exitosamente y el cronograma tentativo institucional ha quedado establecido formalmente.'
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

  seleccionarTab(tab: 'ACTIVIDADES' | 'PLAN_TRABAJO' | 'SEGUIMIENTO' | 'FICHA'): void {
    if (tab === 'SEGUIMIENTO' && !this.canManage()) {
      this.tabActiva.set('ACTIVIDADES');
      return;
    }
    this.tabActiva.set(tab);
  }

  // =========================================================================
  // Métodos Actividad 3: Formulario RG-001 y Plan de Trabajo (PT-001)
  // =========================================================================

  private inicializarRg001YPlan(conv: ConvocatoriaDetalle, plan: PlanTrabajoDto | null): void {
    const reg = conv.registroConvocatoria;
    const today = new Date().toISOString().split('T')[0];
    const fechaIni = reg?.fechaInicio || today;
    const fechaCie = reg?.fechaCierre || today;
    const fechaLim = reg?.fechaLimitePresentacionConeau || fechaCie;

    let rmSugerida = reg?.rmEstandares || '';
    if (!rmSugerida) {
      const normDocs = this.normativaDetalle()?.documentos || [];
      const rmDoc = normDocs.find((d) => d.nombreOriginal.toLowerCase().includes('rm'));
      if (rmDoc) rmSugerida = rmDoc.nombreOriginal;
    }

    this.rg001Data.set({
      nroResolucionConeau: reg?.nroResolucionConeau || '',
      fechaInicio: fechaIni,
      fechaCierre: fechaCie,
      fechaLimitePresentacionConeau: fechaLim,
      rmEstandares: rmSugerida,
      areaDisciplinaria: reg?.areaDisciplinaria || 'Informática / Sistemas',
      modalidadCarrera: reg?.modalidadCarrera === 'DISTANCIA' ? 'A_DISTANCIA' : reg?.modalidadCarrera || 'PRESENCIAL',
      estadoCarrera: reg?.estadoCarrera || conv.tipoProceso || 'EN_FUNCIONAMIENTO',
      faseEvaluacion: reg?.faseEvaluacion || 'PRIMERA_FASE',
      observaciones: reg?.observaciones || '',
    });

    this.fechaLimiteConeauPlan.set(fechaLim);

    // Cargar cronograma de actividades
    const actsPlan = plan?.actividades || [];
    const existentes = actsPlan.filter((a) => a.numeroOrden >= 3);
    if (existentes.length >= 10) {
      const items: ItemActividadPlanRequest[] = existentes
        .sort((a, b) => a.numeroOrden - b.numeroOrden)
        .map((a) => ({
          numeroOrden: a.numeroOrden,
          codigoActividad: a.codigoActividad,
          nombre: a.nombre,
          hoja: a.hoja || 'INFO',
          fechaInicioEstimada: a.fechaInicioEstimada || fechaIni,
          fechaFinEstimada: a.fechaFinEstimada || fechaLim,
          responsable: a.responsable || 'Área de Evaluación y Acreditación (AEA)',
          prioridad: a.prioridad || 'MEDIA',
        }));
      this.cronogramaActividades.set(items);
    } else {
      this.cargandoPlantillaPlan.set(true);
      this.convocatoriaService.getPlantillaPlanTrabajo(conv.id).subscribe({
        next: (plantilla) => {
          const pItems = plantilla.actividades ?? [
            ...(plantilla.actividadesInfo ?? []),
            ...(plantilla.actividadesConeau ?? []),
          ];
          const items: ItemActividadPlanRequest[] = pItems.map((item, idx) => ({
            numeroOrden: item.numeroOrden ?? item.ordenSugerido ?? idx + 3,
            codigoActividad: item.codigoActividad,
            nombre: item.nombre,
            hoja: item.hoja,
            fechaInicioEstimada: fechaIni,
            fechaFinEstimada: fechaLim,
            responsable: item.responsableSugerido || 'Área de Evaluación y Acreditación (AEA)',
            prioridad: item.prioridadSugerida || 'MEDIA',
          }));
          this.cronogramaActividades.set(items);
          this.cargandoPlantillaPlan.set(false);
        },
        error: () => this.cargandoPlantillaPlan.set(false),
      });
    }
  }

  actualizarCampoRg001(campo: keyof GuardarRg001Request, valor: any): void {
    this.rg001Data.update((prev) => ({
      ...prev,
      [campo]: valor,
    }));
  }

  actualizarActividadCronograma(indexEnFiltradas: number, campo: keyof ItemActividadPlanRequest, valor: any): void {
    const act = this.cronogramaActividadesFiltradas()[indexEnFiltradas];
    if (!act) return;
    const indexReal = this.cronogramaActividades().findIndex((a) => a.codigoActividad === act.codigoActividad);
    if (indexReal === -1) return;
    this.cronogramaActividades.update((prev) => {
      const clon = [...prev];
      clon[indexReal] = {
        ...clon[indexReal],
        [campo]: valor,
      };
      return clon;
    });
  }

  cambiarFiltroHojaPlan(hoja: 'TODAS' | 'INFO' | 'CONEAU'): void {
    this.filtroHojaPlan.set(hoja);
  }

  guardarRg001Borrador(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.guardandoRg001Borrador.set(true);
    this.convocatoriaService.guardarRg001Borrador(c.id, this.rg001Data()).subscribe({
      next: (regActualizado) => {
        this.guardandoRg001Borrador.set(false);
        this.convocatoria.update((prev) => prev ? { ...prev, registroConvocatoria: regActualizado } : null);
        this.bannerExitoPlan.set('El borrador del Formulario RG-001 ha sido guardado exitosamente.');
      },
      error: (err) => {
        this.guardandoRg001Borrador.set(false);
        alert(err?.error?.error || 'No fue posible guardar el borrador del Formulario RG-001.');
      },
    });
  }

  enviarRg001(): void {
    const c = this.convocatoria();
    if (!c) return;
    const data = this.rg001Data();
    if (!data.rmEstandares || !data.rmEstandares.trim()) {
      alert('La Resolución Ministerial que fija los estándares (RM Estándares) es requerida.');
      return;
    }
    if (!data.fechaLimitePresentacionConeau) {
      alert('La Fecha Límite de Presentación ante CONEAU es requerida.');
      return;
    }
    this.enviandoRg001.set(true);
    this.convocatoriaService.enviarRg001(c.id, data).subscribe({
      next: (regActualizado) => {
        this.enviandoRg001.set(false);
        this.convocatoria.update((prev) => prev ? { ...prev, registroConvocatoria: regActualizado } : null);
        this.bannerExitoPlan.set('El Formulario RG-001 ha sido enviado formalmente. Ahora solo puede ser modificado por usuarios AEA.');
      },
      error: (err) => {
        this.enviandoRg001.set(false);
        alert(err?.error?.error || 'No fue posible enviar el Formulario RG-001.');
      },
    });
  }

  guardarPlanTrabajoBorrador(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.guardandoPlanBorrador.set(true);
    const req: GuardarCronogramaPlanRequest = {
      fechaLimiteConeau: this.fechaLimiteConeauPlan() || this.rg001Data().fechaLimitePresentacionConeau,
      actividades: this.cronogramaActividades(),
    };
    this.convocatoriaService.guardarPlanTrabajoBorrador(c.id, req).subscribe({
      next: (planActualizado) => {
        this.guardandoPlanBorrador.set(false);
        this.planTrabajo.set(planActualizado);
        this.bannerExitoPlan.set('El borrador del Plan de Trabajo (PT-001) ha sido guardado exitosamente.');
      },
      error: (err) => {
        this.guardandoPlanBorrador.set(false);
        alert(err?.error?.error || 'No fue posible guardar el borrador del Plan de Trabajo.');
      },
    });
  }

  enviarPlanTrabajo(): void {
    const c = this.convocatoria();
    if (!c) return;
    if (this.cronogramaActividades().length === 0) {
      alert('El cronograma del Plan de Trabajo debe tener al menos una actividad.');
      return;
    }
    this.enviandoPlan.set(true);
    const req: GuardarCronogramaPlanRequest = {
      fechaLimiteConeau: this.fechaLimiteConeauPlan() || this.rg001Data().fechaLimitePresentacionConeau,
      actividades: this.cronogramaActividades(),
    };
    this.convocatoriaService.enviarPlanTrabajo(c.id, req).subscribe({
      next: (planActualizado) => {
        this.enviandoPlan.set(false);
        this.planTrabajo.set(planActualizado);
        this.bannerExitoPlan.set('El Plan de Trabajo ha sido enviado formalmente. Ahora solo puede ser modificado por usuarios AEA.');
      },
      error: (err) => {
        this.enviandoPlan.set(false);
        alert(err?.error?.error || 'No fue posible enviar el Plan de Trabajo.');
      },
    });
  }

  finalizarActividadElaboracionPlan(): void {
    const c = this.convocatoria();
    if (!c) return;
    if (!this.puedeFinalizarActividadPlan()) {
      alert('Para finalizar la actividad, tanto el Formulario RG-001 como el Plan de Trabajo deben estar en estado ENVIADO.');
      return;
    }
    this.finalizandoActividadPlan.set(true);
    this.convocatoriaService.finalizarActividadPlanTrabajo(c.id).subscribe({
      next: (detalleActualizado) => {
        this.finalizandoActividadPlan.set(false);
        this.convocatoria.set(detalleActualizado);
        this.bannerExitoPlan.set('¡Actividad Finalizada! La elaboración del Plan de Trabajo y Formulario RG-001 ha quedado formalmente completada.');
        this.cargarDatos(c.id);
      },
      error: (err) => {
        this.finalizandoActividadPlan.set(false);
        alert(err?.error?.error || 'No fue posible finalizar la actividad.');
      },
    });
  }

  formatearEstadoSolicitud(estado?: string | null): string {
    return formatearEstadoNormativa(estado);
  }

  formatearEstadoFt012(estado?: string | null): string {
    switch (estado) {
      case 'SOLICITADA_A_DC':
      case 'BORRADOR':
        return 'BORRADOR EN CARGA (DC)';
      case 'REMITIDA_POR_DC':
        return 'REMITIDA POR DC (PENDIENTE CONEAU)';
      case 'FORMALIZADA_EN_CONEAU':
        return 'FORMALIZADA EN CONEAU';
      default:
        return 'HABILITADA PARA CARGA (DC)';
    }
  }

  seleccionarActividadFoco(codigo: string): void {
    this.focoSeleccionadoManualmente = true;
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
