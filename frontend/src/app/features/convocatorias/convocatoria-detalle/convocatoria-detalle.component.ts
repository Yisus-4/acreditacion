import { Component, computed, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Auth } from '../../../core/auth/auth';
import { ConvocatoriaService } from '../../../core/convocatorias/convocatoria.service';
import {
  ActividadPlanTrabajoDto,
  EstadoActividad,
  AREAS_INFORMADAS_LABELS,
  ConvocatoriaDetalle,
  ConvocatoriaCarreraItem,
  DOCUMENTACION_REMITIDA_LABELS,
  FichaFormalizacionFt012Dto,
  GuardarCronogramaPlanRequest,
  GuardarRg001Request,
  RegistroCaDto,
  MiembroCaDto,
  CatalogoMiembroComisionDto,
  GuardarRegistroCaBorradorRequest,
  RolClaustroCa,
  ItemActividadPlanRequest,
  PlanTrabajoDto,
} from '../../../core/convocatorias/models/convocatoria.model';
import { UsuariosAteneaService } from '../../../core/convocatorias/usuarios-atenea.service';
import { ItemMatrizPermisoAteneaDto, TipoPermisoMatrizAtenea } from '../../../core/convocatorias/models/usuarios-atenea.model';
import {
  RegistroUsuariosAteneaDto,
  UsuarioPermisoAteneaDto,
  GrupoUsuarioAtenea,
  TipoAccesoAtenea,
} from '../../../core/convocatorias/models/usuarios-atenea.model';
import { CompletarActividadModalComponent } from '../completar-actividad-modal/completar-actividad-modal.component';
import { NormativaDetalleComponent } from '../../normativa/normativa-detalle/normativa-detalle.component';
import { NormativaService } from '../../../core/normativa/services/normativa.service';
import {
  BadgePlazoDto,
  DocumentoNormativaDto,
  formatearEstadoNormativa,
  METADATOS_DOCUMENTOS_NORMATIVA,
  TipoDocumentoNormativa,
} from '../../../core/normativa/models/normativa.model';
import { NotificationService } from '../../../core/notifications/notification.service';

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
  private readonly notificationService = inject(NotificationService);
  private readonly usuariosAteneaService = inject(UsuariosAteneaService);
  private readonly auth = inject(Auth);
  private routeSub?: Subscription;
  private idCargadoActualmente: string | null = null;

  readonly today = new Date().toISOString().split('T')[0];

  readonly convocatoria = signal<ConvocatoriaDetalle | null>(null);
  readonly planTrabajo = signal<PlanTrabajoDto | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly tabActiva = signal<'ACTIVIDADES' | 'PLAN_TRABAJO' | 'COMISION_AUTOEVALUACION' | 'USUARIOS_ATENEA' | 'SEGUIMIENTO'>('ACTIVIDADES');

  // RG-003: Usuarios y permisos ATENEA
  readonly usuariosAteneaDesbloqueado = computed(() => {
    const est = this.registroCa()?.estado;
    return est === 'REMITIDA_A_AEA' || est === 'APROBADA';
  });
  readonly registroUsuariosAtenea = signal<RegistroUsuariosAteneaDto | null>(null);
  readonly cargandoUsuariosAtenea = signal(false);
  readonly guardandoUsuariosAtenea = signal(false);
  readonly finalizandoUsuariosAtenea = signal(false);
  readonly bannerExitoUsuariosAtenea = signal<string | null>(null);
  readonly bannerErrorUsuariosAtenea = signal<string | null>(null);
  readonly modalNuevoUsuarioAteneaVisible = signal(false);
  readonly modalConfirmarFinalizarRg003Visible = signal(false);
  readonly editandoPlazoAtenea = signal(false);
  readonly diasPlazoAteneaInput = signal<number>(2);
  readonly fechaPlazoAteneaInput = signal<string>('');
  readonly editandoPlazoCa = signal(false);
  readonly diasPlazoCaInput = signal<number>(15);
  readonly fechaPlazoCaInput = signal<string>('');
  readonly subiendoResolucionCa = signal(false);
  readonly actividadEditandoPlazoId = signal<string | null>(null);
  readonly fechaPlazoActividadInput = signal<string>('');
  readonly guardandoPlazoActividad = signal<boolean>(false);

  // Matriz de permisos ATENEA (RG-003)
  readonly subTabAtenea = signal<'USUARIOS' | 'MATRIZ'>('USUARIOS');
  readonly filtroProcesoMatriz = signal<string>('TODOS');
  readonly busquedaItemMatriz = signal<string>('');
  readonly mostrarMatrizAtenea = signal<boolean>(false);

  readonly procesosUnicosMatriz = computed(() => {
    const items = this.registroUsuariosAtenea()?.matrizPermisos ?? [];
    const set = new Set<string>();
    items.forEach(i => set.add(i.procesoActividad));
    return Array.from(set);
  });

  readonly itemsMatrizFiltrados = computed(() => {
    const items = this.registroUsuariosAtenea()?.matrizPermisos ?? [];
    const filtroProc = this.filtroProcesoMatriz();
    const query = this.busquedaItemMatriz().toLowerCase().trim();

    return items.filter(i => {
      const matchProc = filtroProc === 'TODOS' || i.procesoActividad === filtroProc;
      const matchQuery = !query || i.item.toLowerCase().includes(query) || i.procesoActividad.toLowerCase().includes(query) || (!!i.seccion && i.seccion.toLowerCase().includes(query));
      return matchProc && matchQuery;
    });
  });

  readonly matrizAgrupadaPorProceso = computed(() => {
    const items = this.itemsMatrizFiltrados();
    const grupos: { proceso: string; items: ItemMatrizPermisoAteneaDto[] }[] = [];
    const mapa = new Map<string, ItemMatrizPermisoAteneaDto[]>();

    items.forEach(item => {
      if (!mapa.has(item.procesoActividad)) {
        mapa.set(item.procesoActividad, []);
      }
      mapa.get(item.procesoActividad)!.push(item);
    });

    mapa.forEach((itemsGrupo, proceso) => {
      grupos.push({ proceso, items: itemsGrupo });
    });

    return grupos;
  });

  // Formulario nuevo usuario ATENEA
  readonly nuevoUsuarioGrupo = signal<GrupoUsuarioAtenea>('OTRAS_AREAS');
  readonly nuevoUsuarioNombre = signal('');
  readonly nuevoUsuarioApellido = signal('');
  readonly nuevoUsuarioDni = signal('');
  readonly nuevoUsuarioCorreo = signal('');
  readonly nuevoUsuarioArea = signal('');
  readonly nuevoUsuarioCargo = signal('');
  readonly nuevoUsuarioAcceso = signal<TipoAccesoAtenea>('CARGA');
  readonly nuevoUsuarioAlcance = signal('');
  readonly nuevoUsuarioReqCuenta = signal(true);
  readonly nuevoUsuarioCuentaCreada = signal(false);
  readonly nuevoUsuarioHabilitado = signal(false);

  // Grupos filtrados para visualización por pestaña y tablas
  readonly filtroGrupoAtenea = signal<'TODOS' | 'SGI' | 'AEA' | 'OTRAS_AREAS' | 'COMISION_AUTOEVALUACION'>('TODOS');
  readonly usuariosSgi = computed(() => (this.registroUsuariosAtenea()?.usuarios ?? []).filter(u => u.grupoUsuario === 'SGI'));
  readonly usuariosAea = computed(() => (this.registroUsuariosAtenea()?.usuarios ?? []).filter(u => u.grupoUsuario === 'AEA'));
  readonly usuariosOtrasAreas = computed(() => (this.registroUsuariosAtenea()?.usuarios ?? []).filter(u => u.grupoUsuario === 'OTRAS_AREAS'));
  readonly usuariosCa = computed(() => (this.registroUsuariosAtenea()?.usuarios ?? []).filter(u => u.grupoUsuario === 'COMISION_AUTOEVALUACION'));

  readonly usuariosAteneaFiltrados = computed(() => {
    const list = this.registroUsuariosAtenea()?.usuarios ?? [];
    const filtro = this.filtroGrupoAtenea();
    if (filtro === 'TODOS') return list;
    return list.filter((u) => u.grupoUsuario === filtro);
  });

  readonly conteoAteneaTodos = computed(() => (this.registroUsuariosAtenea()?.usuarios ?? []).length);
  readonly conteoAteneaSgi = computed(() => this.usuariosSgi().length);
  readonly conteoAteneaAea = computed(() => this.usuariosAea().length);
  readonly conteoAteneaOtras = computed(() => this.usuariosOtrasAreas().length);
  readonly conteoAteneaCa = computed(() => this.usuariosCa().length);

  formatearGrupoAtenea(grupo: string): string {
    switch (grupo) {
      case 'SGI': return 'Secretaría de Gestión Institucional';
      case 'AEA': return 'Área de Evaluación y Acreditación';
      case 'OTRAS_AREAS': return 'Otras Áreas Institucionales';
      case 'COMISION_AUTOEVALUACION': return 'Comisión de Autoevaluación · Director de Carrera';
      default: return grupo;
    }
  }

  formatearTipoAcceso(tipo: string): string {
    switch (tipo) {
      case 'VISUALIZACION': return 'Visualización';
      case 'CARGA': return 'Carga';
      case 'CARGA_Y_VISUALIZACION': return 'Carga y visualización';
      case 'ADMINISTRACION': return 'Administración';
      default: return tipo;
    }
  }

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
  readonly modalCatalogoCaAbierto = signal<boolean>(false);
  readonly busquedaCatalogo = signal<string>('');
  readonly resultadosCatalogo = signal<CatalogoMiembroComisionDto[]>([]);
  readonly buscandoCatalogo = signal<boolean>(false);
  readonly modalNuevoMiembroCaAbierto = signal<boolean>(false);
  readonly guardandoAltaEnSistema = signal<string | null>(null);
  readonly nuevoMiembroForm = signal<{
    apellido: string;
    nombreCompleto: string;
    documento: string;
    cuitPasaporte: string;
    correo: string;
    correoGmail: string;
    cargoFuncion: string;
    claustroRol: RolClaustroCa;
    esDocente: boolean;
    cuentaConeauGlobalCreada: boolean;
    cuentaCvarVinculada: boolean;
  }>({
    apellido: '',
    nombreCompleto: '',
    documento: '',
    cuitPasaporte: '',
    correo: '',
    correoGmail: '',
    cargoFuncion: '',
    claustroRol: 'DOCENTE',
    esDocente: true,
    cuentaConeauGlobalCreada: false,
    cuentaCvarVinculada: false
  });

  readonly conformacionCaDesbloqueada = computed(() => {
    return this.actividadPlanFinalizada() || (this.esRg001Enviado() && this.esPlanEnviado());
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

  // Modo Test: Reiniciar Convocatoria
  readonly modalReiniciarTestAbierto = signal<boolean>(false);
  readonly reiniciandoTest = signal<boolean>(false);
  readonly bannerExitoTest = signal<string | null>(null);
  readonly bannerErrorTest = signal<string | null>(null);

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
  readonly seccionFt012Abierta = signal<string | null>('1');
  readonly opcionalesNormativaVisible = signal<boolean>(true);
  readonly seccionesConfirmadasFt012 = signal<Set<string>>(new Set());
  readonly subiendoTipoNormativa = signal<TipoDocumentoNormativa | null>(null);
  readonly enviandoNormativa = signal<boolean>(false);
  readonly aprobandoNormativa = signal<boolean>(false);

  private readonly TIPOS_NORMATIVA_OBLIGATORIOS: TipoDocumentoNormativa[] = [
    'CREACION_CARRERA',
    'ORDENANZA_PLAN_ESTUDIOS',
    'RESOLUCION_DIRECTOR_CARRERA',
    'RESOLUCION_COMISION_CURRICULAR',
    'PLAN_DESARROLLO_CARRERA',
  ];

  private readonly TIPOS_NORMATIVA_OPCIONALES: TipoDocumentoNormativa[] = [
    'RM_VALIDEZ_NACIONAL',
    'APROBACION_PROGRAMAS_ANALITICOS',
    'REGLAMENTO_CARRERA',
    'ACTA_COMISION_CURRICULAR',
    'RM_ESTANDARES_CARRERA',
    'RM_INCLUSION_ART_43',
    'RESOLUCION_CONEAU_ANTERIOR',
  ];

  toggleSeccionFt012(seccionId: string): void {
    this.seccionFt012Abierta.update(curr => curr === seccionId ? null : seccionId);
  }

  toggleOpcionalesNormativa(): void {
    this.opcionalesNormativaVisible.update(v => !v);
  }

  confirmarSeccionFt012(seccionId: string, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.seccionesConfirmadasFt012.update((prev) => {
      const next = new Set(prev);
      next.add(seccionId);
      return next;
    });
  }

  esSeccionFt012Completa(sec: string): boolean {
    const ft = this.fichaFt012();
    const fichaRemitida =
      ft.estado === 'REMITIDA_POR_DC' || ft.estado === 'FORMALIZADA_EN_CONEAU';

    switch (sec) {
      case '1': {
        const camposOk = !!(
          ft.institucionUniversitaria?.trim() &&
          ft.autoridadApellido?.trim() &&
          ft.autoridadPrimerNombre?.trim() &&
          ft.autoridadCorreo?.trim()
        );
        return (
          camposOk &&
          (this.seccionesConfirmadasFt012().has('1') || fichaRemitida)
        );
      }
      case '1.2': {
        const camposOk = !!(
          ft.unidadAcademica?.trim() &&
          ft.sedeCalle?.trim() &&
          ft.sedeNumero?.trim() &&
          ft.sedeCodigoPostal?.trim() &&
          ft.sedeProvincia?.trim() &&
          ft.sedeLocalidad?.trim()
        );
        return (
          camposOk &&
          (this.seccionesConfirmadasFt012().has('1.2') || fichaRemitida)
        );
      }
      case '1.3':
        return !!(
          ft.nexoApellido?.trim() &&
          ft.nexoPrimerNombre?.trim() &&
          ft.nexoCargo?.trim() &&
          ft.nexoCorreo?.trim()
        );
      case '2':
        return !!(
          ft.carreraNombre?.trim() &&
          ft.tituloAsimilado?.trim() &&
          ft.anioInicioDictado
        );
      case '3':
        return !!(
          ft.responsableApellido?.trim() &&
          ft.responsablePrimerNombre?.trim() &&
          ft.responsableCargo?.trim() &&
          ft.responsableCorreo?.trim()
        );
      case '4':
        return !!(
          ft.coordinadorCaApellido?.trim() &&
          ft.coordinadorCaPrimerNombre?.trim() &&
          ft.coordinadorCaCorreo?.trim()
        );
      default:
        return false;
    }
  }

  etiquetaEstadoSeccionFt012(sec: string): string {
    if (this.esSeccionFt012Completa(sec)) {
      return 'Completa';
    }
    if (sec === '1' || sec === '1.2') {
      return 'Pendiente de verificar';
    }
    const ft = this.fichaFt012();
    if (sec === '1.3') {
      const faltan = [
        ft.nexoApellido?.trim(),
        ft.nexoPrimerNombre?.trim(),
        ft.nexoCargo?.trim(),
        ft.nexoCorreo?.trim(),
      ].filter((v) => !v).length;
      return `Faltan ${faltan} campo(s) obligatorio(s)`;
    }
    if (sec === '2') {
      const faltan = [
        ft.carreraNombre?.trim(),
        ft.tituloAsimilado?.trim(),
        ft.anioInicioDictado,
      ].filter((v) => !v).length;
      return `Faltan ${faltan} campo(s) obligatorio(s)`;
    }
    return 'Incompleta';
  }

  calcularEtapaTexto(carrera: ConvocatoriaCarreraItem): string {
    const estado = carrera.estadoProceso || 'INICIADO';
    if (estado === 'FINALIZADO' || estado === 'SEGUIMIENTO') return 'Etapa 5 de 5 · Seguimiento del Plan de Trabajo';
    if (estado === 'USUARIOS_ATENEA') return 'Etapa 4 de 5 · Usuarios ATENEA';
    if (estado === 'EN_AUTOEVALUACION') return 'Etapa 3 de 5 · Comisión de Autoevaluación';
    if (estado === 'PLAN_TRABAJO') return 'Etapa 2 de 5 · Plan de Trabajo y RG-001';
    if (estado === 'EN_FORMALIZACION') return 'Etapa 1 de 5 · Formalización y Normativa (en curso)';
    return 'Etapa 1 de 5 · Formalización y Normativa (sin iniciar)';
  }

  calcularEtapaProgreso(carrera: ConvocatoriaCarreraItem): number {
    const estado = carrera.estadoProceso || 'INICIADO';
    if (estado === 'FINALIZADO' || estado === 'SEGUIMIENTO') return 100;
    if (estado === 'USUARIOS_ATENEA') return 80;
    if (estado === 'EN_AUTOEVALUACION') return 60;
    if (estado === 'PLAN_TRABAJO') return 40;
    if (estado === 'EN_FORMALIZACION') return 20;
    return 0;
  }

  irAGestionarActividad(act: any): void {
    if (act) {
      this.abrirDrawer(act);
    }
  }

  readonly normativaDetalle = this.normativaService.detalle;

  readonly ranurasNormativaObligatorias = computed(() => {
    const docs = this.normativaDetalle()?.documentos ?? [];
    return this.TIPOS_NORMATIVA_OBLIGATORIOS.map((tipo) => {
      const meta = METADATOS_DOCUMENTOS_NORMATIVA[tipo];
      const doc = docs.find((d) => d.tipoDocumento === tipo) ?? null;
      return {
        tipo,
        titulo: meta?.titulo ?? tipo,
        descripcion: meta?.descripcion ?? '',
        esObligatorio: true,
        documento: doc,
      };
    });
  });

  readonly ranurasNormativaOpcionales = computed(() => {
    const docs = this.normativaDetalle()?.documentos ?? [];
    return this.TIPOS_NORMATIVA_OPCIONALES.map((tipo) => {
      const meta = METADATOS_DOCUMENTOS_NORMATIVA[tipo];
      const doc = docs.find((d) => d.tipoDocumento === tipo) ?? null;
      return {
        tipo,
        titulo: meta?.titulo ?? tipo,
        descripcion: meta?.descripcion ?? '',
        esObligatorio: false,
        documento: doc,
      };
    });
  });

  readonly normativaObligatoriosCargadosCount = computed(() => {
    return this.ranurasNormativaObligatorias().filter((r) => !!r.documento).length;
  });

  readonly normativaOpcionalesCargadosCount = computed(() => {
    return this.ranurasNormativaOpcionales().filter((r) => !!r.documento).length;
  });

  readonly seccionesFt012CompletasCount = computed(() => {
    const ids = ['1', '1.2', '1.3', '2', '3', '4'];
    return ids.filter((id) => this.esSeccionFt012Completa(id)).length;
  });

  readonly tareasFormalizacionTotal = 11;

  readonly tareasFormalizacionCompletadas = computed(() => {
    return this.seccionesFt012CompletasCount() + this.normativaObligatoriosCargadosCount();
  });

  readonly progresoFormalizacionPorcentaje = computed(() => {
    return Math.round((this.tareasFormalizacionCompletadas() / this.tareasFormalizacionTotal) * 100);
  });

  onSubirDocNormativaInline(event: Event, tipo: TipoDocumentoNormativa): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    input.value = '';

    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      alert('Solo se permiten archivos en formato PDF.');
      return;
    }

    const c = this.convocatoria();
    if (!c) return;

    this.subiendoTipoNormativa.set(tipo);
    this.normativaService.cargarBorrador(c.id, tipo, file).subscribe({
      next: () => {
        this.subiendoTipoNormativa.set(null);
      },
      error: (err) => {
        this.subiendoTipoNormativa.set(null);
        alert(err?.error?.message || err?.error?.error || 'Error al subir el documento de normativa.');
      },
    });
  }

  descargarDocNormativaInline(doc: DocumentoNormativaDto): void {
    const c = this.convocatoria();
    if (!c) return;
    this.normativaService.descargarDocumento(c.id, doc.id, doc.nombreOriginal);
  }

  eliminarDocNormativaInline(docId: string): void {
    const c = this.convocatoria();
    if (!c) return;
    this.normativaService.eliminarBorrador(c.id, docId).subscribe({
      next: () => {},
      error: (err) => {
        alert(err?.error?.message || err?.error?.error || 'No fue posible eliminar el documento.');
      },
    });
  }

  formatearBytesNormativa(bytes?: number | null): string {
    if (!bytes || bytes <= 0) return '0 KB';
    const kb = bytes / 1024;
    if (kb < 1024) {
      return `${kb.toFixed(1)} KB`;
    }
    return `${(kb / 1024).toFixed(2).replace('.', ',')} MB`;
  }

  enviarNormativaDefinitivaInline(): void {
    const c = this.convocatoria();
    if (!c) return;
    if (this.normativaObligatoriosCargadosCount() < 5) {
      alert('Debe cargar los 5 documentos obligatorios de normativa antes de enviarlos.');
      return;
    }

    this.enviandoNormativa.set(true);
    this.normativaService.enviarDefinitiva(c.id).subscribe({
      next: () => {
        this.enviandoNormativa.set(false);
        this.bannerExitoPlan.set(
          'Los documentos de Normativa de la carrera fueron enviados formalmente a revisión por el Área de Evaluación y Acreditación (AEA).'
        );
        this.refrescarPlanTrabajo();
      },
      error: (err) => {
        this.enviandoNormativa.set(false);
        alert(
          err?.error?.message ||
            err?.error?.error ||
            'No fue posible enviar la documentación de normativa.'
        );
      },
    });
  }

  aprobarNormativaInline(): void {
    const c = this.convocatoria();
    if (!c) return;
    if (this.normativaObligatoriosCargadosCount() < 5) {
      alert('Debe cargar los 5 documentos obligatorios antes de aprobar la normativa.');
      return;
    }

    this.aprobandoNormativa.set(true);
    const estadoActual = this.normativaDetalle()?.estado;

    const ejecutarAprobacion = () => {
      this.normativaService
        .revisarNormativa(c.id, {
          decision: 'APROBADO',
          observaciones: 'Normativa verificada y aprobada por AEA.',
        })
        .subscribe({
          next: () => {
            this.aprobandoNormativa.set(false);
            this.bannerExitoPlan.set(
              '¡Normativa de la carrera aprobada exitosamente por la AEA! La actividad ha quedado completada.'
            );
            this.onNormativaActualizada();
          },
          error: (err) => {
            this.aprobandoNormativa.set(false);
            alert(
              err?.error?.message ||
                err?.error?.error ||
                'No fue posible aprobar la normativa.'
            );
          },
        });
    };

    if (estadoActual !== 'ENVIADA_A_REVISION') {
      this.normativaService.enviarDefinitiva(c.id).subscribe({
        next: () => ejecutarAprobacion(),
        error: (err) => {
          this.aprobandoNormativa.set(false);
          alert(
            err?.error?.message ||
              err?.error?.error ||
              'No fue posible enviar la normativa previa a la aprobación.'
          );
        },
      });
    } else {
      ejecutarAprobacion();
    }
  }

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

  readonly esFt012Completa = computed(() => {
    return this.seccionesFt012CompletasCount() === 6;
  });

  readonly esFt012Enviada = computed(() => {
    const ft = this.fichaFt012();
    return (
      (ft.estado === 'REMITIDA_POR_DC' || ft.estado === 'FORMALIZADA_EN_CONEAU') &&
      this.esFt012Completa()
    );
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
    const normOk =
      norm?.estado === 'COMPLETADA' ||
      norm?.estado === 'ENVIADA_A_REVISION' ||
      actNorm?.estado === 'COMPLETADA' ||
      this.actividad2Completa();
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

  readonly esAdminOAEA = this.canManage;

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

  readonly conteoCompletadas = computed(() => {
    return this.planTrabajo()?.actividades?.filter((a) => a.estado === 'COMPLETADA').length ?? 0;
  });

  readonly conteoPendientes = computed(() => {
    return this.planTrabajo()?.actividades?.filter((a) => a.estado !== 'COMPLETADA').length ?? 0;
  });

  readonly conteoConPlazoExtendido = computed(() => {
    return this.planTrabajo()?.actividades?.filter((a) => a.plazosExtendidos && a.plazosExtendidos.length > 0).length ?? 0;
  });

  readonly maxPlazosExtendidos = computed(() => {
    const acts = this.planTrabajo()?.actividades ?? [];
    return acts.reduce((max, a) => Math.max(max, a.plazosExtendidos?.length ?? 0), 0);
  });

  readonly columnasPlazosExtendidos = computed(() => {
    const max = this.maxPlazosExtendidos();
    const count = Math.max(max, 1);
    return Array.from({ length: count }, (_, i) => i);
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

  cargarDatos(id: string, autoAvanzar: boolean = false, forzarRecarga: boolean = false): void {
    if (this.idCargadoActualmente === id && this.convocatoria() && !autoAvanzar && !forzarRecarga) {
      return;
    }
    this.idCargadoActualmente = id;
    this.loading.set(true);
    this.error.set(null);
    this.convocatoria.set(null);
    this.planTrabajo.set(null);
    this.seccionesConfirmadasFt012.set(new Set());

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

  abrirModalReiniciarTest(): void {
    this.bannerErrorTest.set(null);
    this.modalReiniciarTestAbierto.set(true);
  }

  cerrarModalReiniciarTest(): void {
    if (!this.reiniciandoTest()) {
      this.modalReiniciarTestAbierto.set(false);
    }
  }

  confirmarReinicioTest(): void {
    const c = this.convocatoria();
    if (!c) return;

    this.reiniciandoTest.set(true);
    this.bannerErrorTest.set(null);

    this.convocatoriaService.reiniciarConvocatoriaTest(c.id).subscribe({
      next: (actualizada) => {
        this.reiniciandoTest.set(false);
        this.modalReiniciarTestAbierto.set(false);
        this.bannerExitoTest.set(
          "El proceso de acreditación de la escuela ha sido reiniciado con éxito para pruebas. Se encuentra nuevamente en estado 'Pendiente de Director de Escuela'."
        );
        this.convocatoria.set(actualizada);
        this.initSeleccionDirectorEscuela(actualizada);
        this.cargarDatos(c.id, true);
      },
      error: (err) => {
        this.reiniciandoTest.set(false);
        this.bannerErrorTest.set(err?.error?.message || err?.error?.error || 'Error al reiniciar la convocatoria para pruebas.');
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
        const tieneAvanceReal =
          ficha.estado === 'REMITIDA_POR_DC' ||
          ficha.estado === 'FORMALIZADA_EN_CONEAU' ||
          !!(
            ficha.nexoApellido?.trim() ||
            ficha.responsableApellido?.trim() ||
            ficha.coordinadorCaApellido?.trim() ||
            ficha.anioInicioDictado
          );
        if (tieneAvanceReal) {
          this.seccionesConfirmadasFt012.set(new Set(['1', '1.2']));
        }
        this.actualizarFocoPorDefecto();

        // Si la etapa de formalización y normativa está completada, avanzar a la carga del RG-001 y Plan de Trabajo, o a Comisión de Autoevaluación
        if (autoAvanzar || (!this.focoSeleccionadoManualmente && (this.tabActiva() === 'ACTIVIDADES' || this.tabActiva() === 'PLAN_TRABAJO'))) {
          if (this.conformacionCaDesbloqueada() && this.registroCa()?.estado !== 'APROBADA') {
            this.tabActiva.set('COMISION_AUTOEVALUACION');
          } else if (this.planTrabajoDesbloqueado() && !this.actividadPlanFinalizada()) {
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
      this.refrescarPlanTrabajo();
      this.cargarDatos(c.id, true);
    }
  }

  refrescarPlanTrabajo(): void {
    const c = this.convocatoria();
    if (!c) return;
    this.convocatoriaService.obtenerPlanTrabajo(c.id).subscribe({
      next: (plan) => {
        this.planTrabajo.set(plan);
        const drawerAct = this.actividadSeleccionadaDrawer();
        if (drawerAct && plan.actividades) {
          const actualizada = plan.actividades.find((a) => a.id === drawerAct.id);
          if (actualizada) {
            this.actividadSeleccionadaDrawer.set(actualizada);
          }
        }
      },
      error: () => {},
    });
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

  esActividadTallerConeau(act: ActividadPlanTrabajoDto): boolean {
    return (
      act.codigoActividad === '1.6' ||
      act.nombre.toLowerCase().includes('taller de la coneau') ||
      act.nombre.toLowerCase().includes('taller coneau')
    );
  }

  marcarComoCompleta(act: ActividadPlanTrabajoDto): void {
    if (!this.canManage()) return;
    this.convocatoriaService.completarActividadSimple(act.id).subscribe({
      next: (actActualizada) => {
        this.refrescarPlanTrabajo();
        const c = this.convocatoria();
        if (c?.id) {
          this.cargarDatos(c.id, false, true);
        }
      },
      error: (err) => {
        console.error('Error al completar actividad:', err);
        alert(err.error?.message || 'Error al marcar la actividad como completa.');
      }
    });
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
    const camposSec1: (keyof FichaFormalizacionFt012Dto)[] = [
      'institucionUniversitaria',
      'autoridadApellido',
      'autoridadPrimerNombre',
      'autoridadSegundoNombre',
      'autoridadCorreo',
      'autoridadTelefono',
    ];
    const camposSec12: (keyof FichaFormalizacionFt012Dto)[] = [
      'unidadAcademica',
      'sedeCalle',
      'sedeNumero',
      'sedeCodigoPostal',
      'sedeProvincia',
      'sedeLocalidad',
      'sedeDepartamentoPartido',
      'sedePiso',
    ];
    if (camposSec1.includes(campo)) {
      this.confirmarSeccionFt012('1');
    } else if (camposSec12.includes(campo)) {
      this.confirmarSeccionFt012('1.2');
    }
  }

  guardarORemitirFt012(enviar: boolean): void {
    const c = this.convocatoria();
    if (!c) return;

    if (enviar) {
      const nombresSecciones: Record<string, string> = {
        '1': '1. Datos de la institución',
        '1.2': '1.2 Unidad académica y sede',
        '1.3': '1.3 Persona nexo con la CONEAU',
        '2': '2. Datos de la carrera',
        '3': '3. Responsable de la carrera',
        '4': '4. Coordinador del Comité de Autoevaluación',
      };
      const ids = ['1', '1.2', '1.3', '2', '3', '4'];
      const faltantes = ids.filter((id) => !this.esSeccionFt012Completa(id));
      if (faltantes.length > 0) {
        this.seccionFt012Abierta.set(faltantes[0]);
        const lista = faltantes.map((id) => `• ${nombresSecciones[id]}`).join('\n');
        alert(
          `No es posible remitir la Ficha FT-012 porque aún faltan completar o verificar las siguientes secciones:\n\n${lista}`
        );
        return;
      }
    }

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
          alert(err?.error?.message || err?.error?.error || 'No fue posible guardar la Ficha FT-012.');
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

  seleccionarTab(tab: 'ACTIVIDADES' | 'PLAN_TRABAJO' | 'COMISION_AUTOEVALUACION' | 'USUARIOS_ATENEA' | 'SEGUIMIENTO'): void {
    if (tab === 'USUARIOS_ATENEA' && !this.usuariosAteneaDesbloqueado()) {
      return;
    }
    this.tabActiva.set(tab);
    const c = this.convocatoria();
    if (!c) return;

    if (tab === 'SEGUIMIENTO') {
      this.refrescarPlanTrabajo();
    } else if (tab === 'USUARIOS_ATENEA') {
      this.cargarUsuariosAtenea(c.id);
    } else if (tab === 'COMISION_AUTOEVALUACION') {
      this.cargarRegistroCa(c.id);
    } else if (tab === 'PLAN_TRABAJO') {
      this.refrescarPlanTrabajo();
    }
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
        this.seleccionarTab('COMISION_AUTOEVALUACION');
        this.refrescarPlanTrabajo();
        this.cargarDatos(c.id, false, true);
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
        if (ca.estado === 'APROBADA') {
          this.cargarUsuariosAtenea(procesoId);
        }
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

  abrirModalCatalogoCa(): void {
    this.busquedaCatalogo.set('');
    this.modalCatalogoCaAbierto.set(true);
    this.buscarEnCatalogo('');
  }

  cerrarModalCatalogoCa(): void {
    this.modalCatalogoCaAbierto.set(false);
    this.busquedaCatalogo.set('');
  }

  buscarEnCatalogo(termino?: string): void {
    const query = termino !== undefined ? termino : this.busquedaCatalogo();
    this.buscandoCatalogo.set(true);
    this.convocatoriaService.consultarCatalogoMiembrosCa(query ? query.trim() : undefined).subscribe({
      next: (res) => {
        this.resultadosCatalogo.set(res);
        this.buscandoCatalogo.set(false);
      },
      error: () => {
        this.buscandoCatalogo.set(false);
      }
    });
  }

  seleccionarMiembroDelCatalogo(item: CatalogoMiembroComisionDto): void {
    const actuales = this.miembrosCa();
    const doc = item.documento ? item.documento.trim().toLowerCase() : '';
    const cuit = item.cuitPasaporte ? item.cuitPasaporte.trim().toLowerCase() : '';

    const yaExiste = actuales.some(m => {
      const mDoc = (m.documento || m.dniCuil || '').trim().toLowerCase();
      const mCuit = (m.cuitPasaporte || '').trim().toLowerCase();
      return (doc && mDoc === doc) || (cuit && mCuit === cuit);
    });

    if (yaExiste) {
      alert('Esta persona ya se encuentra incorporada en la nómina de la comisión.');
      return;
    }

    const nuevo: MiembroCaDto = {
      apellido: item.apellido,
      primerNombre: item.nombreCompleto,
      nombreCompleto: item.nombreCompleto,
      dniCuil: item.documento,
      documento: item.documento,
      cuitPasaporte: item.cuitPasaporte,
      correo: item.correoElectronico,
      correoGmail: item.correoGmail,
      cargoFuncion: item.cargoFuncion,
      claustroRol: actuales.length === 0 ? 'COORDINADOR' : 'DOCENTE',
      esDocente: item.esDocente,
      cuentaConeauGlobalCreada: false,
      cuentaCvarVinculada: false,
      pendienteAltaSistema: false,
      numeroOrden: actuales.length + 1
    };

    this.miembrosCa.set([...actuales, nuevo]);
    this.cerrarModalCatalogoCa();
    this.guardarBorradorCa();
  }

  abrirModalNuevoMiembroCa(): void {
    this.cerrarModalCatalogoCa();
    const actuales = this.miembrosCa();
    this.nuevoMiembroForm.set({
      apellido: '',
      nombreCompleto: '',
      documento: '',
      cuitPasaporte: '',
      correo: '',
      correoGmail: '',
      cargoFuncion: '',
      claustroRol: actuales.length === 0 ? 'COORDINADOR' : 'DOCENTE',
      esDocente: true,
      cuentaConeauGlobalCreada: false,
      cuentaCvarVinculada: false
    });
    this.modalNuevoMiembroCaAbierto.set(true);
  }

  cerrarModalNuevoMiembroCa(): void {
    this.modalNuevoMiembroCaAbierto.set(false);
  }

  actualizarCampoNuevoMiembro(campo: string, valor: any): void {
    const actual = { ...this.nuevoMiembroForm() };
    (actual as any)[campo] = valor;
    if (campo === 'esDocente' && !valor) {
      actual.cuentaCvarVinculada = false;
    }
    this.nuevoMiembroForm.set(actual);
  }

  guardarNuevoMiembroModal(): void {
    const f = this.nuevoMiembroForm();
    if (!f.apellido.trim() || !f.nombreCompleto.trim()) {
      alert('Debe ingresar Apellido/s y Nombre completo.');
      return;
    }
    if (!f.documento.trim()) {
      alert('Debe ingresar el Documento.');
      return;
    }
    if (!f.cuitPasaporte.trim()) {
      alert('Debe ingresar el CUIT/Pasaporte.');
      return;
    }
    if (!f.correo.trim()) {
      alert('Debe ingresar el correo electrónico asociado a CVar y CONEAU Global.');
      return;
    }
    if (!f.correoGmail.trim()) {
      alert('Debe ingresar el correo electrónico Gmail para el espacio de trabajo compartido.');
      return;
    }
    if (!f.cargoFuncion.trim()) {
      alert('Debe indicar el cargo o función en la carrera (ej: Director de Carrera, Docente).');
      return;
    }

    const actuales = this.miembrosCa();
    const doc = f.documento.trim().toLowerCase();
    const cuit = f.cuitPasaporte.trim().toLowerCase();

    const yaExiste = actuales.some(m => {
      const mDoc = (m.documento || m.dniCuil || '').trim().toLowerCase();
      const mCuit = (m.cuitPasaporte || '').trim().toLowerCase();
      return (doc && mDoc === doc) || (cuit && mCuit === cuit);
    });

    if (yaExiste) {
      alert('Esta persona ya se encuentra incorporada en la nómina de la comisión.');
      return;
    }

    const nuevo: MiembroCaDto = {
      apellido: f.apellido.trim(),
      primerNombre: f.nombreCompleto.trim(),
      nombreCompleto: f.nombreCompleto.trim(),
      dniCuil: f.documento.trim(),
      documento: f.documento.trim(),
      cuitPasaporte: f.cuitPasaporte.trim(),
      correo: f.correo.trim().toLowerCase(),
      correoGmail: f.correoGmail.trim().toLowerCase(),
      cargoFuncion: f.cargoFuncion.trim(),
      claustroRol: f.claustroRol,
      esDocente: f.esDocente,
      cuentaConeauGlobalCreada: f.cuentaConeauGlobalCreada,
      cuentaCvarVinculada: f.cuentaCvarVinculada,
      pendienteAltaSistema: true,
      numeroOrden: actuales.length + 1
    };

    this.miembrosCa.set([...actuales, nuevo]);
    this.cerrarModalNuevoMiembroCa();
    this.guardarBorradorCa();
  }

  confirmarRegistroMiembroEnSistema(m: MiembroCaDto): void {
    const c = this.convocatoria();
    if (!c || !m.id) {
      alert('El miembro aún no fue persistido en el servidor. Guarde el borrador primero.');
      return;
    }
    this.guardandoAltaEnSistema.set(m.id);
    this.convocatoriaService.confirmarRegistroMiembroCa(c.id, m.id).subscribe({
      next: (ca) => {
        this.guardandoAltaEnSistema.set(null);
        this.registroCa.set(ca);
        if (ca.miembros) {
          this.miembrosCa.set(ca.miembros.map(mi => ({ ...mi })));
        }
        this.bannerExitoCa.set(`Se ha confirmado y registrado permanentemente a ${m.apellido} en el catálogo de miembros y en Usuarios.`);
        this.notificationService.refrescar('AEA');
      },
      error: (err) => {
        this.guardandoAltaEnSistema.set(null);
        alert(err?.error?.error || 'No fue posible registrar al miembro en el catálogo.');
      }
    });
  }

  cancelarRegistroMiembroEnSistema(m: MiembroCaDto): void {
    const c = this.convocatoria();
    if (!c || !m.id) {
      alert('El miembro aún no fue persistido en el servidor. Guarde el borrador primero.');
      return;
    }
    this.guardandoAltaEnSistema.set(m.id);
    this.convocatoriaService.cancelarRegistroMiembroCa(c.id, m.id).subscribe({
      next: (ca) => {
        this.guardandoAltaEnSistema.set(null);
        this.registroCa.set(ca);
        if (ca.miembros) {
          this.miembrosCa.set(ca.miembros.map(mi => ({ ...mi })));
        }
        this.bannerExitoCa.set(`Se marcó a ${m.apellido} como revisado para esta convocatoria.`);
        this.notificationService.refrescar('AEA');
      },
      error: (err) => {
        this.guardandoAltaEnSistema.set(null);
        alert(err?.error?.error || 'No fue posible actualizar el estado del miembro.');
      }
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
    if (campo === 'nombreCompleto') {
      actuales[index].primerNombre = valor;
    }
    if (campo === 'documento') {
      actuales[index].dniCuil = valor;
    }
    if (campo === 'dniCuil') {
      actuales[index].documento = valor;
    }
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
        if (ca.miembros) {
          this.miembrosCa.set(ca.miembros.map(mi => ({ ...mi })));
        }
        const tieneNuevos = (ca.miembros || []).some(m => m.pendienteAltaSistema);
        if (tieneNuevos) {
          this.bannerExitoCa.set('¡Borrador guardado! Se ha enviado la solicitud de alta en catálogo para los integrantes nuevos a la campana de AEA y a Administración de Usuarios.');
          if (this.canManage()) {
            this.notificationService.refrescar('AEA');
          }
        } else {
          this.bannerExitoCa.set('Borrador de la Comisión de Autoevaluación guardado correctamente.');
        }
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
      const nombre = (m.nombreCompleto || m.primerNombre || '').trim();
      const doc = (m.documento || m.dniCuil || '').trim();
      if (!m.apellido?.trim() || !nombre || !doc || !m.correo?.trim()) {
        alert('Todos los miembros deben tener completos Apellido, Nombre, Documento y Correo.');
        return;
      }
    }
    const archivo = this.archivoResolucionCa();

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
        if (ca.miembros) {
          this.miembrosCa.set(ca.miembros.map(mi => ({ ...mi })));
        }
        this.archivoResolucionCa.set(null);
        const msgRes = ca.resolucionStoragePath
          ? 'La conformación de la CA y su Resolución han sido remitidas a AEA. La etapa de Usuarios ATENEA (RG-003) está desbloqueada.'
          : 'La conformación de la CA ha sido remitida a AEA y la etapa de Usuarios ATENEA (RG-003) está desbloqueada. La actividad quedará en progreso hasta que envíe la Resolución de Designación.';
        this.bannerExitoCa.set(msgRes);
        this.refrescarPlanTrabajo();
        this.cargarDatos(c.id, false, true);
      },
      error: (err) => {
        this.remitirCaCargando.set(false);
        alert(err?.error?.error || 'No fue posible remitir la conformación de la CA.');
      }
    });
  }

  enviarResolucionPendienteCa(): void {
    const c = this.convocatoria();
    const archivo = this.archivoResolucionCa();
    if (!c) return;
    if (!archivo) {
      alert('Seleccione primero el archivo PDF de la Resolución de Designación de la Comisión.');
      return;
    }
    this.subiendoResolucionCa.set(true);
    this.convocatoriaService.adjuntarResolucionCa(c.id, archivo).subscribe({
      next: (ca) => {
        this.subiendoResolucionCa.set(false);
        this.registroCa.set(ca);
        this.archivoResolucionCa.set(null);
        this.bannerExitoCa.set('¡Resolución de Designación de la Comisión enviada correctamente! La actividad ha quedado completada.');
        this.refrescarPlanTrabajo();
        this.cargarDatos(c.id, false, true);
      },
      error: (err) => {
        this.subiendoResolucionCa.set(false);
        alert(err?.error?.error || 'No fue posible adjuntar la Resolución de Designación.');
      }
    });
  }

  private calcularDiaSiguienteISO(fechasCandidatas: (string | undefined | null)[]): string {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    let base = hoy;
    for (const fStr of fechasCandidatas) {
      if (!fStr) continue;
      const f = new Date(fStr + 'T00:00:00');
      if (!isNaN(f.getTime()) && f > base) {
        base = f;
      }
    }
    const min = new Date(base);
    min.setDate(min.getDate() + 1);
    return min.toISOString().split('T')[0];
  }

  obtenerFechaMinimaPlazoActividad(act: ActividadPlanTrabajoDto): string {
    const ultimoExt = act.plazosExtendidos && act.plazosExtendidos.length > 0
      ? act.plazosExtendidos[act.plazosExtendidos.length - 1]
      : null;
    return this.calcularDiaSiguienteISO([act.fechaFinEstimada, ultimoExt]);
  }

  obtenerFechaVencimientoEfectiva(act: ActividadPlanTrabajoDto): string | null {
    if (act.plazosExtendidos && act.plazosExtendidos.length > 0) {
      return act.plazosExtendidos[act.plazosExtendidos.length - 1];
    }
    return act.fechaFinEstimada || null;
  }

  obtenerUltimoPlazoExtendido(act: ActividadPlanTrabajoDto): string | null {
    if (!act.plazosExtendidos || act.plazosExtendidos.length === 0) return null;
    return act.plazosExtendidos[act.plazosExtendidos.length - 1];
  }

  calcularDiasRestantes(fechaFin?: string | null): number | null {
    if (!fechaFin) return null;
    const partes = fechaFin.split('-');
    if (partes.length < 3) return null;
    const fin = new Date(parseInt(partes[0], 10), parseInt(partes[1], 10) - 1, parseInt(partes[2], 10));
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    return Math.ceil((fin.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
  }

  esFilaAlerta(act: ActividadPlanTrabajoDto): boolean {
    if (act.estado === 'COMPLETADA') return false;
    const fechaVence = this.obtenerFechaVencimientoEfectiva(act);
    const dias = this.calcularDiasRestantes(fechaVence);
    const tieneExtensiones = !!(act.plazosExtendidos && act.plazosExtendidos.length > 0);
    return (dias !== null && dias <= 10) || tieneExtensiones;
  }

  abrirEdicionPlazoActividad(act: ActividadPlanTrabajoDto): void {
    if (!this.canManage()) return;
    this.actividadEditandoPlazoId.set(act.id);
    this.fechaPlazoActividadInput.set(this.obtenerFechaMinimaPlazoActividad(act));
  }

  cancelarEdicionPlazoActividad(): void {
    this.actividadEditandoPlazoId.set(null);
    this.fechaPlazoActividadInput.set('');
  }

  guardarPlazoExtendidoActividad(act: ActividadPlanTrabajoDto): void {
    if (!this.canManage()) return;
    const nuevaFecha = this.fechaPlazoActividadInput();
    const minFecha = this.obtenerFechaMinimaPlazoActividad(act);
    if (!nuevaFecha || nuevaFecha < minFecha) {
      alert(`El nuevo plazo debe ser posterior al último plazo vigente (mínimo ${minFecha}). No se puede volver el tiempo hacia atrás.`);
      return;
    }
    this.guardandoPlazoActividad.set(true);
    this.convocatoriaService.extenderPlazoActividad(act.id, nuevaFecha).subscribe({
      next: (actActualizada) => {
        this.guardandoPlazoActividad.set(false);
        this.actividadEditandoPlazoId.set(null);
        this.planTrabajo.update((plan) => {
          if (!plan) return plan;
          return {
            ...plan,
            actividades: (plan.actividades || []).map((a) => (a.id === actActualizada.id ? actActualizada : a)),
          };
        });
        if (this.actividadSeleccionadaDrawer()?.id === actActualizada.id) {
          this.actividadSeleccionadaDrawer.set(actActualizada);
        }
        const c = this.convocatoria();
        if (c) {
          this.normativaService.obtenerDetalle(c.id).subscribe({ error: () => {} });
          this.cargarRegistroCa(c.id);
          if (this.registroUsuariosAtenea()) {
            this.cargarUsuariosAtenea(c.id);
          }
        }
      },
      error: (err) => {
        this.guardandoPlazoActividad.set(false);
        alert(err?.error?.error || err?.error?.message || 'No se pudo agregar el plazo extendido.');
      },
    });
  }

  obtenerFechaMinimaPlazoCa(): string {
    const actCa = (this.planTrabajo()?.actividades ?? []).find(
      (a) => a.codigoActividad === '3.1' || a.codigoActividad === '3.2' || a.nombre.toLowerCase().includes('comisión de autoevaluación')
    );
    const ultimoExt = actCa?.plazosExtendidos?.length ? actCa.plazosExtendidos[actCa.plazosExtendidos.length - 1] : null;
    return this.calcularDiaSiguienteISO([this.registroCa()?.fechaLimiteDc, actCa?.fechaFinEstimada, ultimoExt]);
  }

  abrirEdicionPlazoCa(): void {
    if (!this.canManage()) return;
    this.fechaPlazoCaInput.set(this.obtenerFechaMinimaPlazoCa());
    this.editandoPlazoCa.set(true);
  }

  guardarPlazoCa(): void {
    const c = this.convocatoria();
    if (!c || !this.canManage()) return;
    const nuevaFecha = this.fechaPlazoCaInput();
    const minFecha = this.obtenerFechaMinimaPlazoCa();
    if (!nuevaFecha || nuevaFecha < minFecha) {
      alert(`El nuevo plazo debe ser posterior al último plazo vigente (mínimo ${minFecha}). No se puede volver hacia atrás.`);
      return;
    }
    this.cargandoRegistroCa.set(true);
    this.convocatoriaService.solicitarConformacionCa(c.id, undefined, nuevaFecha).subscribe({
      next: (ca) => {
        this.cargandoRegistroCa.set(false);
        this.editandoPlazoCa.set(false);
        this.registroCa.set(ca);
        this.bannerExitoCa.set(`Nuevo plazo extendido hasta ${nuevaFecha} y añadido al seguimiento de actividades.`);
        this.refrescarPlanTrabajo();
      },
      error: (err) => {
        this.cargandoRegistroCa.set(false);
        alert(err?.error?.error || err?.error?.message || 'No se pudo actualizar el plazo de la CA.');
      }
    });
  }

  obtenerFechaMinimaPlazoAtenea(): string {
    const actAtenea = (this.planTrabajo()?.actividades ?? []).find(
      (a) => a.codigoActividad === '3.3' || a.nombre.toLowerCase().includes('atenea')
    );
    const ultimoExt = actAtenea?.plazosExtendidos?.length ? actAtenea.plazosExtendidos[actAtenea.plazosExtendidos.length - 1] : null;
    return this.calcularDiaSiguienteISO([this.registroUsuariosAtenea()?.fechaLimite, actAtenea?.fechaFinEstimada, ultimoExt]);
  }

  abrirEdicionPlazoAtenea(): void {
    if (!this.canManage()) return;
    this.fechaPlazoAteneaInput.set(this.obtenerFechaMinimaPlazoAtenea());
    this.editandoPlazoAtenea.set(true);
  }

  guardarPlazoAtenea(): void {
    const c = this.convocatoria();
    if (!c || !this.canManage()) return;
    const nuevaFecha = this.fechaPlazoAteneaInput();
    const minFecha = this.obtenerFechaMinimaPlazoAtenea();
    if (!nuevaFecha || nuevaFecha < minFecha) {
      this.bannerErrorUsuariosAtenea.set(`El nuevo plazo debe ser posterior al último plazo vigente (mínimo ${minFecha}). No se puede volver hacia atrás.`);
      return;
    }
    this.guardandoUsuariosAtenea.set(true);
    this.usuariosAteneaService.actualizarPlazo(c.id, undefined, nuevaFecha).subscribe({
      next: (res) => {
        this.guardandoUsuariosAtenea.set(false);
        this.editandoPlazoAtenea.set(false);
        this.registroUsuariosAtenea.set(res);
        this.bannerExitoUsuariosAtenea.set(`Nuevo plazo extendido hasta ${nuevaFecha} y añadido al seguimiento de actividades.`);
        this.refrescarPlanTrabajo();
        setTimeout(() => this.bannerExitoUsuariosAtenea.set(null), 4000);
      },
      error: (err) => {
        this.guardandoUsuariosAtenea.set(false);
        this.bannerErrorUsuariosAtenea.set(err?.error?.error || err?.error?.message || 'No se pudo actualizar el plazo de Usuarios ATENEA.');
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
        this.refrescarPlanTrabajo();
        this.cargarDatos(c.id, false, true);
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


  // =========================================================================
  // Métodos RG-003: Registro de usuarios de ATENEA
  // =========================================================================

  cargarUsuariosAtenea(procesoId: string): void {
    this.cargandoUsuariosAtenea.set(true);
    this.usuariosAteneaService.consultar(procesoId).subscribe({
      next: (res) => {
        this.cargandoUsuariosAtenea.set(false);
        this.registroUsuariosAtenea.set(res);
      },
      error: (err) => {
        this.cargandoUsuariosAtenea.set(false);
        console.error('Error al cargar registro de usuarios ATENEA:', err);
      }
    });
  }

  guardarUsuariosAtenea(): void {
    const c = this.convocatoria();
    const reg = this.registroUsuariosAtenea();
    if (!c || !reg) return;

    this.guardandoUsuariosAtenea.set(true);
    this.bannerExitoUsuariosAtenea.set(null);
    this.bannerErrorUsuariosAtenea.set(null);

    this.usuariosAteneaService.guardar(c.id, {
      usuarios: reg.usuarios,
      matrizPermisos: reg.matrizPermisos
    }).subscribe({
      next: (actualizado) => {
        this.guardandoUsuariosAtenea.set(false);
        this.registroUsuariosAtenea.set(actualizado);
        this.bannerExitoUsuariosAtenea.set('Los permisos y usuarios de ATENEA han sido guardados correctamente.');
        setTimeout(() => this.bannerExitoUsuariosAtenea.set(null), 4000);
      },
      error: (err) => {
        this.guardandoUsuariosAtenea.set(false);
        this.bannerErrorUsuariosAtenea.set(err?.error?.message || 'Error al guardar cambios de usuarios ATENEA.');
      }
    });
  }

  togglePermisoSgiAea(item: ItemMatrizPermisoAteneaDto): void {
    const reg = this.registroUsuariosAtenea();
    if (!reg || reg.finalizado) return;
    const nuevosItems = reg.matrizPermisos.map(m => {
      if (m === item || (m.orden === item.orden && m.tipoPermiso === item.tipoPermiso)) {
        return { ...m, permisoSgiAea: !m.permisoSgiAea };
      }
      return m;
    });
    this.registroUsuariosAtenea.set({ ...reg, matrizPermisos: nuevosItems });
  }

  togglePermisoCa(item: ItemMatrizPermisoAteneaDto): void {
    const reg = this.registroUsuariosAtenea();
    if (!reg || reg.finalizado) return;
    const nuevosItems = reg.matrizPermisos.map(m => {
      if (m === item || (m.orden === item.orden && m.tipoPermiso === item.tipoPermiso)) {
        return { ...m, permisoCa: !m.permisoCa };
      }
      return m;
    });
    this.registroUsuariosAtenea.set({ ...reg, matrizPermisos: nuevosItems });
  }

  marcarTodosGrupoMatriz(grupo: 'SGI_AEA' | 'CA', valor: boolean): void {
    const reg = this.registroUsuariosAtenea();
    if (!reg || reg.finalizado) return;
    const nuevosItems = reg.matrizPermisos.map(m => {
      if (grupo === 'SGI_AEA') {
        return { ...m, permisoSgiAea: valor };
      } else {
        return { ...m, permisoCa: valor };
      }
    });
    this.registroUsuariosAtenea.set({ ...reg, matrizPermisos: nuevosItems });
  }

  restablecerMatrizPorDefecto(): void {
    const reg = this.registroUsuariosAtenea();
    if (!reg || reg.finalizado) return;
    if (!confirm('¿Desea restablecer todos los permisos de la matriz a los valores predeterminados de UNdeC AEA?')) {
      return;
    }
    const nuevosItems = reg.matrizPermisos.map(m => {
      const esCarrera = 'carrera' === (m.seccion || '').toLowerCase();
      if (m.tipoPermiso === 'LECTURA') {
        return { ...m, permisoSgiAea: true, permisoCa: true };
      } else {
        return { ...m, permisoSgiAea: true, permisoCa: esCarrera };
      }
    });
    this.registroUsuariosAtenea.set({ ...reg, matrizPermisos: nuevosItems });
  }

  abrirModalNuevoUsuario(grupo: GrupoUsuarioAtenea = 'OTRAS_AREAS'): void {
    this.nuevoUsuarioGrupo.set(grupo);
    this.nuevoUsuarioNombre.set('');
    this.nuevoUsuarioApellido.set('');
    this.nuevoUsuarioDni.set('');
    this.nuevoUsuarioCorreo.set('');
    this.nuevoUsuarioArea.set('');
    this.nuevoUsuarioCargo.set('');
    this.nuevoUsuarioAcceso.set('CARGA');
    this.nuevoUsuarioAlcance.set('');
    this.nuevoUsuarioReqCuenta.set(true);
    this.nuevoUsuarioCuentaCreada.set(false);
    this.nuevoUsuarioHabilitado.set(false);
    this.modalNuevoUsuarioAteneaVisible.set(true);
  }

  cerrarModalNuevoUsuario(): void {
    this.modalNuevoUsuarioAteneaVisible.set(false);
  }

  agregarNuevoUsuario(): void {
    const reg = this.registroUsuariosAtenea();
    if (!reg) return;

    if (!this.nuevoUsuarioNombre().trim() || !this.nuevoUsuarioApellido().trim() || !this.nuevoUsuarioDni().trim() || !this.nuevoUsuarioCorreo().trim()) {
      alert('Por favor complete Nombre, Apellido, DNI/CUIL y Correo electrónico.');
      return;
    }

    const nuevo: UsuarioPermisoAteneaDto = {
      grupoUsuario: this.nuevoUsuarioGrupo(),
      nombre: this.nuevoUsuarioNombre().trim(),
      apellido: this.nuevoUsuarioApellido().trim(),
      nombreCompleto: `${this.nuevoUsuarioApellido().trim()}, ${this.nuevoUsuarioNombre().trim()}`,
      dniCuil: this.nuevoUsuarioDni().trim(),
      correo: this.nuevoUsuarioCorreo().trim().toLowerCase(),
      areaDependencia: this.nuevoUsuarioArea().trim() || undefined,
      cargoFuncion: this.nuevoUsuarioCargo().trim() || undefined,
      tipoAcceso: this.nuevoUsuarioAcceso(),
      seccionAlcance: this.nuevoUsuarioAlcance().trim() || undefined,
      requiereCuentaNuevaConeauGlobal: this.nuevoUsuarioReqCuenta(),
      cuentaConeauGlobalCreada: this.nuevoUsuarioCuentaCreada(),
      habilitadoEnConeau: this.nuevoUsuarioHabilitado()
    };

    const lista = [...reg.usuarios, nuevo];
    this.registroUsuariosAtenea.set({ ...reg, usuarios: lista });
    this.cerrarModalNuevoUsuario();
  }

  eliminarUsuarioAtenea(usuario: UsuarioPermisoAteneaDto): void {
    const reg = this.registroUsuariosAtenea();
    if (!reg || reg.finalizado) return;

    if (!confirm(`¿Está seguro de quitar a "${usuario.nombreCompleto || usuario.apellido}" del registro de ATENEA?`)) {
      return;
    }

    const lista = reg.usuarios.filter(u => u !== usuario);
    this.registroUsuariosAtenea.set({ ...reg, usuarios: lista });
  }

  abrirModalFinalizarRg003(): void {
    this.modalConfirmarFinalizarRg003Visible.set(true);
  }

  cerrarModalFinalizarRg003(): void {
    this.modalConfirmarFinalizarRg003Visible.set(false);
  }

  confirmarFinalizarRg003(): void {
    const c = this.convocatoria();
    if (!c) return;

    this.finalizandoUsuariosAtenea.set(true);
    this.bannerExitoUsuariosAtenea.set(null);
    this.bannerErrorUsuariosAtenea.set(null);

    this.usuariosAteneaService.finalizar(c.id).subscribe({
      next: (actualizado) => {
        this.finalizandoUsuariosAtenea.set(false);
        this.registroUsuariosAtenea.set(actualizado);
        this.cerrarModalFinalizarRg003();
        this.bannerExitoUsuariosAtenea.set('El Registro RG-003 ha sido formalmente emitido, almacenado en MinIO y la Actividad 3.3 en el Plan de Trabajo ha quedado completada.');
        // Recargar convocatoria para refrescar progreso del plan
        this.refrescarPlanTrabajo();
        this.cargarDatos(c.id, false, true);
      },
      error: (err) => {
        this.finalizandoUsuariosAtenea.set(false);
        this.bannerErrorUsuariosAtenea.set(err?.error?.message || 'Error al finalizar el registro RG-003.');
      }
    });
  }

  descargarPdfRg003(): void {
    const c = this.convocatoria();
    if (!c) return;

    this.usuariosAteneaService.descargarPdf(c.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const nombreCarrera = c.carreraNombre?.replace(/\s+/g, '_') || 'Carrera';
        const anio = new Date().getFullYear();
        a.download = `RG-003_Registro_de_usuarios_de_ATENEA_${nombreCarrera}_${anio}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert('No fue posible descargar el archivo PDF oficial del RG-003.');
      }
    });
  }
}
