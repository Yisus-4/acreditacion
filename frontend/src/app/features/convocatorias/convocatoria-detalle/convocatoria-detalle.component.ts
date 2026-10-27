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
  FichaFormalizacionFt012Dto,
  GuardarCronogramaPlanRequest,
  GuardarRg001Request,
  RegistroCaDto,
  MiembroCaDto,
  GuardarRegistroCaBorradorRequest,
  RolClaustroCa,
  ItemActividadPlanRequest,
  PlanTrabajoDto,
} from '../../../core/convocatorias/models/convocatoria.model';
import { CompletarActividadModalComponent } from '../completar-actividad-modal/completar-actividad-modal.component';
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

@Component({
  selector: 'app-convocatoria-detalle',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CompletarActividadModalComponent,
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

  readonly convocatoria = signal<ConvocatoriaDetalle | null>(null);
  readonly planTrabajo = signal<PlanTrabajoDto | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly tabActiva = signal<'ACTIVIDADES' | 'PLAN_TRABAJO' | 'COMISION_AUTOEVALUACION' | 'SEGUIMIENTO'>('ACTIVIDADES');

  // Estado Comisión de Autoevaluación (CA · RG-002)
  readonly registroCa = signal<RegistroCaDto | null>(null);
  readonly cargandoRegistroCa = signal<boolean>(false);
  readonly guardandoCaBorrador = signal<boolean>(false);
  readonly remitirCaCargando = signal<boolean>(false);
  readonly aprobandoCa = signal<boolean>(false);
  readonly observandoCa = signal<boolean>(false);
  readonly motivoObservacionCa = signal<string>('');
  readonly modalObservarCaAbierto = signal<boolean>(false);
  readonly miembrosCa = signal<MiembroCaDto[]>([]);
  readonly coincideCurricularCa = signal<boolean>(false);
  readonly archivoResolucionCa = signal<File | null>(null);
  readonly bannerExitoCa = signal<string | null>(null);

  readonly conformacionCaDesbloqueada = computed(() => {
    return this.actividadPlanFinalizada() && this.esRg001Enviado();
  });

  readonly puedeEditarCa = computed(() => {
    if (!this.conformacionCaDesbloqueada()) return false;
    const est = this.registroCa()?.estado || 'PENDIENTE_SOLICITUD';
    if (est === 'APROBADA') return false;
    return true;
  });

  readonly actividadFoco = signal<string>('1.1');
  private focoSeleccionadoManualmente = false;
  readonly reiniciando = signal<boolean>(false);

  readonly actividadSeleccionada = signal<ActividadPlanTrabajoDto | null>(null);
  readonly actividadSeleccionadaDrawer = signal<ActividadPlanTrabajoDto | null>(null);
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

  // Estado Formalización electrónica FT-012
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
  readonly guardandoFt012 = signal<boolean>(false);

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

  readonly esFt012Enviada = computed(() => {
    const ft = this.fichaFt012();
    return ft.estado === 'REMITIDA_POR_DC' || ft.estado === 'FORMALIZADA_EN_CONEAU' || !!ft.remitidoEn;
  });

  readonly puedeEditarFt012 = computed(() => {
    if (this.esFt012Enviada()) {
      return this.canManage(); // Solo AEA o ADMINISTRATOR pueden editar una vez remitida
    }
    return true; // DC y otros pueden editar mientras esté en borrador
  });

  readonly planTrabajoDesbloqueado = computed(() => {
    const ftOk = this.esFt012Enviada();
    const norm = this.normativaDetalle();
    const actNorm = this.actividadNormativa();
    const normOk = norm?.estado === 'COMPLETADA' || actNorm?.estado === 'COMPLETADA' || this.actividad2Completa();
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

  readonly actividad2Completa = computed(() => {
    const actNorm = this.actividadNormativa();
    const norm = this.normativaDetalle();
    return actNorm?.estado === 'COMPLETADA' || norm?.estado === 'COMPLETADA';
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

  cargarDatos(id: string, autoAvanzar: boolean = false): void {
    if (this.idCargadoActualmente === id && this.convocatoria() && !autoAvanzar) {
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
        this.cargarFichaFormalizacion(id, autoAvanzar);
        this.cargarRegistroCa(id);
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

  cargarFichaFormalizacion(procesoId: string, autoAvanzar: boolean = false): void {
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
        this.actualizarFocoPorDefecto();

        // Si la etapa de formalización y normativa está completada, avanzar a la carga del RG-001 y Plan de Trabajo
        if (autoAvanzar || (!this.focoSeleccionadoManualmente && this.tabActiva() === 'ACTIVIDADES')) {
          if (this.planTrabajoDesbloqueado() && !this.actividadPlanFinalizada()) {
            this.tabActiva.set('PLAN_TRABAJO');
          }
        }
      },
      error: () => {},
    });
  }

  onNormativaActualizada(): void {
    const c = this.convocatoria();
    if (c) {
      this.cargarDatos(c.id, true);
    }
  }

  private actualizarFocoPorDefecto(): void {
    if (this.focoSeleccionadoManualmente) {
      return;
    }
    const act1 = this.actividadReunion();
    const actNorm = this.actividadNormativa();
    const norm = this.normativaDetalle();

    const act1Completa = act1?.estado === 'COMPLETADA';
    const act2Completa = actNorm?.estado === 'COMPLETADA' || norm?.estado === 'COMPLETADA';

    if (!act1Completa) {
      this.actividadFoco.set('1.1');
    } else if (!act2Completa) {
      this.actividadFoco.set('1.3');
    } else {
      this.actividadFoco.set('1.2');
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
  // Acciones Formalización electrónica FT-012
  // =========================================================================

  actualizarCampoFt012(campo: keyof FichaFormalizacionFt012Dto, valor: any): void {
    this.fichaFt012.update((prev) => ({
      ...prev,
      [campo]: valor,
    }));
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
              ? 'La Ficha de Formalización Electrónica (FT-012) ha sido remitida completa al Área de Evaluación y Acreditación (AEA). La posibilidad de modificar los datos se encuentra cerrada para la Dirección de Carrera.'
              : 'El borrador de la Ficha FT-012 fue guardado correctamente.'
          );
          this.cargarDatos(c.id, enviar);
        },
        error: (err) => {
          this.guardandoFt012.set(false);
          alert(err?.error?.error || 'No fue posible guardar la Ficha FT-012.');
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

  seleccionarTab(tab: 'ACTIVIDADES' | 'PLAN_TRABAJO' | 'COMISION_AUTOEVALUACION' | 'SEGUIMIENTO'): void {
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

  // ==========================================
  // Métodos Comisión de Autoevaluación (RG-002)
  // ==========================================

  cargarRegistroCa(procesoId: string): void {
    this.cargandoRegistroCa.set(true);
    this.convocatoriaService.obtenerRegistroCa(procesoId).subscribe({
      next: (ca) => {
        this.cargandoRegistroCa.set(false);
        this.registroCa.set(ca);
        this.coincideCurricularCa.set(ca.coincideComisionCurricular);
        if (ca.miembros && ca.miembros.length > 0) {
          this.miembrosCa.set(ca.miembros.map(m => ({ ...m })));
        } else {
          this.miembrosCa.set([]);
        }
      },
      error: () => {
        this.cargandoRegistroCa.set(false);
      }
    });
  }

  solicitarConformacionCa(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.cargandoRegistroCa.set(true);
    this.convocatoriaService.solicitarConformacionCa(c.id).subscribe({
      next: (ca) => {
        this.cargandoRegistroCa.set(false);
        this.registroCa.set(ca);
        this.bannerExitoCa.set('Se ha emitido la solicitud formal de conformación de CA a la Dirección de Carrera.');
      },
      error: (err) => {
        this.cargandoRegistroCa.set(false);
        alert(err?.error?.error || 'No fue posible solicitar la conformación de la CA.');
      }
    });
  }

  descargarInstructivoIt001(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.convocatoriaService.descargarInstructivoIt001(c.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'IT-001_Instructivo_Cuentas_CVar_CONEAU_Global.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => alert('No se pudo descargar el instructivo IT-001.')
    });
  }

  agregarMiembroCa(): void {
    const actuales = this.miembrosCa();
    const nuevo: MiembroCaDto = {
      apellido: '',
      primerNombre: '',
      segundoNombre: '',
      dniCuil: '',
      correo: '',
      telefono: '',
      claustroRol: actuales.length === 0 ? 'COORDINADOR' : 'DOCENTE',
      esDocente: true,
      cuentaConeauGlobalCreada: false,
      cuentaCvarVinculada: false,
      numeroOrden: actuales.length + 1
    };
    this.miembrosCa.set([...actuales, nuevo]);
  }

  eliminarMiembroCa(index: number): void {
    const actuales = this.miembrosCa().filter((_, i) => i !== index);
    const reordenados = actuales.map((m, idx) => ({ ...m, numeroOrden: idx + 1 }));
    this.miembrosCa.set(reordenados);
  }

  actualizarCampoMiembro(index: number, campo: keyof MiembroCaDto, valor: any): void {
    const actuales = [...this.miembrosCa()];
    if (!actuales[index]) return;
    actuales[index] = { ...actuales[index], [campo]: valor };
    if (campo === 'esDocente' && !valor) {
      actuales[index].cuentaCvarVinculada = false;
    }
    this.miembrosCa.set(actuales);
  }

  onResolucionCaFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.type !== 'application/pdf') {
        alert('El documento de la resolución de designación debe ser formato PDF.');
        input.value = '';
        return;
      }
      this.archivoResolucionCa.set(file);
    }
  }

  guardarBorradorCa(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.guardandoCaBorrador.set(true);
    const req: GuardarRegistroCaBorradorRequest = {
      coincideCurricular: this.coincideCurricularCa(),
      miembros: this.miembrosCa()
    };
    this.convocatoriaService.guardarRegistroCaBorrador(c.id, req).subscribe({
      next: (ca) => {
        this.guardandoCaBorrador.set(false);
        this.registroCa.set(ca);
        this.bannerExitoCa.set('Borrador de la Comisión de Autoevaluación guardado correctamente.');
      },
      error: (err) => {
        this.guardandoCaBorrador.set(false);
        alert(err?.error?.error || 'No fue posible guardar el borrador de la CA.');
      }
    });
  }

  remitirCaAea(): void {
    const c = this.convocatoria();
    if (!c) return;
    const miembros = this.miembrosCa();
    if (miembros.length === 0) {
      alert('Debe agregar al menos un miembro para conformar la Comisión de Autoevaluación.');
      return;
    }
    const tieneCoord = miembros.some(m => m.claustroRol === 'COORDINADOR');
    if (!tieneCoord) {
      alert('La Comisión de Autoevaluación debe contar con al menos un miembro con rol COORDINADOR.');
      return;
    }
    for (const m of miembros) {
      if (!m.apellido?.trim() || !m.primerNombre?.trim() || !m.dniCuil?.trim() || !m.correo?.trim()) {
        alert('Todos los miembros deben tener completos Apellido, Nombre, DNI/CUIL y Correo.');
        return;
      }
      if (m.esDocente && !m.cuentaCvarVinculada) {
        alert(`El miembro docente ${m.apellido}, ${m.primerNombre} debe tener confirmada la vinculación con CVar.`);
        return;
      }
    }
    const archivo = this.archivoResolucionCa();
    const caActual = this.registroCa();
    if (!archivo && (!caActual?.resolucionStoragePath)) {
      alert('Es obligatorio adjuntar la Resolución de designación (PDF) para remitir a AEA.');
      return;
    }

    this.remitirCaCargando.set(true);
    const formData = new FormData();
    formData.append('coincideCurricular', String(this.coincideCurricularCa()));
    formData.append('miembros', JSON.stringify(miembros));
    if (archivo) {
      formData.append('resolucion', archivo);
    }

    this.convocatoriaService.remitirRegistroCa(c.id, formData).subscribe({
      next: (ca) => {
        this.remitirCaCargando.set(false);
        this.registroCa.set(ca);
        this.archivoResolucionCa.set(null);
        this.bannerExitoCa.set('La conformación de la CA ha sido remitida a AEA para su auditoría y aprobación formal.');
      },
      error: (err) => {
        this.remitirCaCargando.set(false);
        alert(err?.error?.error || 'No fue posible remitir la conformación de la CA.');
      }
    });
  }

  aprobarCa(): void {
    const c = this.convocatoria();
    if (!c) return;
    if (!confirm('¿Confirma la aprobación formal de la Comisión de Autoevaluación (RG-002)?')) return;
    this.aprobandoCa.set(true);
    this.convocatoriaService.aprobarRegistroCa(c.id).subscribe({
      next: (ca) => {
        this.aprobandoCa.set(false);
        this.registroCa.set(ca);
        this.bannerExitoCa.set('¡Comisión de Autoevaluación aprobada formalmente! Se ha generado el Registro RG-002 e impactado en el Plan de Trabajo.');
        this.cargarDatos(c.id);
      },
      error: (err) => {
        this.aprobandoCa.set(false);
        alert(err?.error?.error || 'No fue posible aprobar la conformación de la CA.');
      }
    });
  }

  abrirModalObservarCa(): void {
    this.motivoObservacionCa.set('');
    this.modalObservarCaAbierto.set(true);
  }

  cerrarModalObservarCa(): void {
    this.modalObservarCaAbierto.set(false);
    this.motivoObservacionCa.set('');
  }

  confirmarObservarCa(): void {
    const c = this.convocatoria();
    const motivo = this.motivoObservacionCa().trim();
    if (!c || !motivo) {
      alert('Debe ingresar el detalle u observaciones de las correcciones solicitadas.');
      return;
    }
    this.observandoCa.set(true);
    this.convocatoriaService.observarRegistroCa(c.id, motivo).subscribe({
      next: (ca) => {
        this.observandoCa.set(false);
        this.modalObservarCaAbierto.set(false);
        this.registroCa.set(ca);
        this.bannerExitoCa.set('Se ha devuelto el registro a la Dirección de Carrera con observaciones para subsanación.');
      },
      error: (err) => {
        this.observandoCa.set(false);
        alert(err?.error?.error || 'No fue posible registrar las observaciones.');
      }
    });
  }

  descargarResolucionAdjunta(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.convocatoriaService.descargarResolucionCa(c.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const nombre = this.registroCa()?.resolucionNombreOriginal || 'Resolucion_Designacion_CA.pdf';
        a.download = nombre;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => alert('No fue posible descargar la resolución adjunta.')
    });
  }

}
