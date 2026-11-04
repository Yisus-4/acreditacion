import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';

import {
  ActualizarGuiaHigieneRequest,
  AutoevaluacionService,
  DocumentoHigieneItemRequest,
  EspacioFisico,
  GuiaHigieneSeguridad,
  Inmueble,
  RegistrarInmuebleRequest,
  TipoEspacioFisico,
} from '../../../core/autoevaluacion/autoevaluacion.service';
import { CarreraResponse, EstructuraService } from '../../../core/estructura/estructura.service';
import {
  Departamento,
  GeografiaService,
  Localidad,
  Provincia,
} from '../../../core/geografia/geografia.service';
import { SubnavAutoevaluacionComponent } from '../subnav-autoevaluacion/subnav-autoevaluacion.component';

@Component({
  selector: 'app-infraestructura',
  standalone: true,
  imports: [ReactiveFormsModule, RouterModule, SubnavAutoevaluacionComponent],
  templateUrl: './infraestructura.component.html',
  styleUrl: './infraestructura.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfraestructuraComponent implements OnInit {
  private readonly autoevalService = inject(AutoevaluacionService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly geografiaService = inject(GeografiaService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly inmuebles = signal<Inmueble[]>([]);
  readonly tiposEspacio = signal<TipoEspacioFisico[]>([]);
  readonly carreras = signal<CarreraResponse[]>([]);
  readonly provincias = signal<Provincia[]>([]);
  readonly departamentos = signal<Departamento[]>([]);
  readonly localidades = signal<Localidad[]>([]);

  readonly loading = signal<boolean>(true);
  readonly submitting = signal<boolean>(false);
  readonly formAbierto = signal<boolean>(false);
  readonly modoEdicion = signal<boolean>(false);
  readonly inmuebleEditandoId = signal<string | null>(null);

  readonly actividadSeleccionada = signal<'INFRAESTRUCTURA' | 'HIGIENE_SEGURIDAD' | 'EQUIPAMIENTO_BIBLIOTECA' | null>(null);
  readonly filtroActividadesTexto = signal<string>('');
  readonly modoVista = signal<'MAESTRO' | 'PROCESO'>('MAESTRO');
  readonly procesoId = signal<string | null>(null);

  readonly searchQuery = signal<string>('');
  readonly selectedTenenciaFilter = signal<string>('');
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly espacioExpandidoId = signal<string | null>(null);

  // Modal Extensión de Plazo (PT-001)
  readonly mostrarModalExtension = signal<boolean>(false);
  readonly nuevaFechaLimite = signal<string>('');
  readonly motivoExtension = signal<string>('');

  // Espacios para el formulario de inmueble
  readonly espaciosAgregados = signal<EspacioFisico[]>([]);
  readonly mostrarFormEspacio = signal<boolean>(false);
  readonly espacioEditandoIndex = signal<number | null>(null);
  readonly nuevoEspacioNombre = signal<string>('');
  readonly nuevoEspacioDescInfra = signal<string>('');
  readonly nuevoEspacioTipoId = signal<string>('');
  readonly nuevoEspacioCantidad = signal<number>(1);
  readonly nuevoEspacioCapacidad = signal<number>(30);

  // Modal para Crear Tipo de Espacio Físico (Caso 2 del Diagrama de Secuencia)
  readonly mostrarModalNuevoTipo = signal<boolean>(false);
  readonly nuevoTipoDenominacion = signal<string>('');
  readonly nuevoTipoCodigo = signal<string>('');
  readonly guardandoTipoEspacio = signal<boolean>(false);

  // Carreras Vinculadas (Diagrama de Secuencia: Agregar carreras con las que se vincula)
  readonly carrerasSeleccionadas = signal<CarreraResponse[]>([]);
  readonly carreraParaVincularId = signal<string>('');

  // Documentación de Respaldo con Tipología (Diagrama de Secuencia: CU-ACR-03.02)
  readonly documentosAgregados = signal<{
    nombreArchivo: string;
    urlMinio: string;
    tipoAnexo: string;
    descripcion?: string;
  }[]>([]);
  readonly inputDocNombre = signal<string>('');
  readonly inputDocTipoAnexo = signal<string>('CONVENIO_COMODATO');
  readonly inputDocDescripcion = signal<string>('');

  // Doble Confirmación al Guardar (Diagrama de Secuencia: Solicita segunda confirmación)
  readonly mostrarConfirmacionGuardado = signal<boolean>(false);

  // ---------------------------------------------------------------------------
  // GT-003: Higiene y Seguridad
  // ---------------------------------------------------------------------------
  readonly guiaHigiene = signal<GuiaHigieneSeguridad | null>(null);
  readonly loadingHigiene = signal<boolean>(false);
  readonly guardandoHigiene = signal<boolean>(false);

  readonly mostrarModalNormativaHigiene = signal<boolean>(false);
  readonly nuevaNormativaTipo = signal<string>('Resolución Rectoral');
  readonly nuevaNormativaEmisor = signal<string>('Secretaría de Gestión Institucional');
  readonly nuevaNormativaNumero = signal<string>('');
  readonly nuevaNormativaDesc = signal<string>('');
  readonly nuevaNormativaArchivo = signal<string>('');

  readonly formHigiene: FormGroup = this.fb.group({
    instanciaResponsableUa: ['', [Validators.required, Validators.maxLength(2000)]],
    politicaUnidadesApoyo: ['', [Validators.maxLength(1600)]],
    aplicaUnidadesApoyo: [false],
    observaciones: [''],
  });

  readonly form: FormGroup = this.fb.group({
    numeroOrden: [null],
    nombre: ['', [Validators.required, Validators.maxLength(255)]],
    domicilio: ['', [Validators.required, Validators.maxLength(255)]],
    calle: [''],
    numero: [''],
    provinciaId: [null, [Validators.required]],
    departamentoId: [null, [Validators.required]],
    localidadId: [null, [Validators.required]],
    anioConstruccion: [null],
    anioInicioInstitucion: [null],
    condicionTenencia: ['PROPIO', [Validators.required]],
    situacion: ['Propiedad'],
    vinculaTodasCarreras: [false],
    superficieCubiertaM2: [null],
    superficieTotalM2: [null],
    observaciones: [''],
    estadoConeau: ['CARGADO'],
    carrerasIds: [[]],
  });

  readonly filteredInmuebles = computed(() => {
    let list = this.inmuebles();
    const query = this.searchQuery().trim().toLowerCase();
    const tenencia = this.selectedTenenciaFilter();

    if (query) {
      list = list.filter(
        (i) =>
          i.nombre.toLowerCase().includes(query) ||
          i.domicilio.toLowerCase().includes(query) ||
          (i.localidadNombre && i.localidadNombre.toLowerCase().includes(query)) ||
          (i.situacion && i.situacion.toLowerCase().includes(query))
      );
    }

    if (tenencia) {
      list = list.filter((i) => i.condicionTenencia === tenencia || i.situacion?.toUpperCase() === tenencia.toUpperCase());
    }

    return list;
  });

  readonly actividad41Completada = computed<boolean>(() => {
    return this.inmuebles().length > 0 && this.inmuebles().some((i) => i.verificado);
  });

  readonly listaActividadesDimension = computed(() => {
    const query = this.filtroActividadesTexto().toLowerCase().trim();
    const estadoInfra = this.actividad41Completada() ? 'Completada' : 'Pendiente';
    const estadoHigiene = this.guiaHigiene()?.verificado ? 'Completada' : 'Pendiente';

    const actividades = [
      {
        id: 'INFRAESTRUCTURA' as const,
        nombre: 'Gestionar información sobre infraestructura',
        responsable: 'Planificación e Infraestructura',
        instrumento: 'RG-005: Registro de Inmuebles',
        estado: estadoInfra,
        completada: this.actividad41Completada(),
      },
      {
        id: 'HIGIENE_SEGURIDAD' as const,
        nombre: 'Gestionar información sobre Higiene y Seguridad',
        responsable: 'Área de Higiene, Seguridad y Medio Ambiente Laboral',
        instrumento: 'GT-003: Guía de Higiene y Seguridad de la UA',
        estado: estadoHigiene,
        completada: !!this.guiaHigiene()?.verificado,
      },
      {
        id: 'EQUIPAMIENTO_BIBLIOTECA' as const,
        nombre: 'Gestionar información sobre equipamiento didáctico y centros de documentación',
        responsable: 'Biblioteca Central / Informática',
        instrumento: 'Formulario CONEAU / Inventario Bibliotecario',
        estado: 'Pendiente',
        completada: false,
      },
    ];

    if (!query) return actividades;
    return actividades.filter(
      (a) =>
        a.nombre.toLowerCase().includes(query) ||
        a.responsable.toLowerCase().includes(query) ||
        a.instrumento.toLowerCase().includes(query)
    );
  });

  volverAlListado(): void {
    this.actividadSeleccionada.set(null);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const pId = params.get('procesoId') || params.get('convocatoriaId');
        if (pId) {
          this.procesoId.set(pId);
          this.modoVista.set('PROCESO');
        }
        const act = params.get('actividad');
        if (act === 'INFRAESTRUCTURA' || act === 'HIGIENE_SEGURIDAD' || act === 'EQUIPAMIENTO_BIBLIOTECA') {
          this.actividadSeleccionada.set(act);
        }
        this.cargarDatosIniciales();
      });

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const id = params.get('id');
        if (id) {
          this.procesoId.set(id);
          this.modoVista.set('PROCESO');
          this.cargarDatosIniciales();
        }
      });
  }

  cargarDatosIniciales(): void {
    if (this.actividadSeleccionada() === 'HIGIENE_SEGURIDAD') {
      this.cargarGuiaHigiene();
    } else {
      this.cargarInmuebles();
    }

    this.autoevalService.listarTiposEspacio()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (tipos) => this.tiposEspacio.set(tipos),
      });

    this.estructuraService.listarCarreras()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (car) => this.carreras.set(car),
      });

    this.geografiaService.obtenerProvincias()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (provs) => {
          this.provincias.set(provs);
          const lr = provs.find((p) => p.codigo === 'LR' || p.nombre.toLowerCase().includes('rioja'));
          if (lr) {
            this.form.patchValue({ provinciaId: lr.id });
            this.cargarDepartamentos(lr.id);
          }
        },
      });
  }

  cargarInmuebles(): void {
    this.loading.set(true);

    const call$ = (this.modoVista() === 'PROCESO' && this.procesoId())
      ? this.autoevalService.obtenerInmueblesPorProceso(this.procesoId()!)
      : this.autoevalService.listarInmueblesMaestros();

    call$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (data) => this.inmuebles.set(data),
        error: () => this.errorMessage.set('Error al cargar registro de inmuebles'),
      });
  }

  cambiarModoVista(modo: 'MAESTRO' | 'PROCESO'): void {
    this.modoVista.set(modo);
    this.errorMessage.set(null);
    if (this.actividadSeleccionada() === 'HIGIENE_SEGURIDAD') {
      this.cargarGuiaHigiene();
    } else {
      this.cargarInmuebles();
    }
  }

  cargarDepartamentos(provinciaId: string | number): void {
    this.geografiaService.obtenerDepartamentos(provinciaId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (deptos) => {
          this.departamentos.set(deptos);
          const chilecito = deptos.find((d) => d.nombre.toLowerCase().includes('chilecito'));
          if (chilecito) {
            this.form.patchValue({ departamentoId: chilecito.id });
            this.cargarLocalidades(chilecito.id);
          }
        },
      });
  }

  cargarLocalidades(departamentoId: string | number): void {
    this.geografiaService.obtenerLocalidades(departamentoId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (locs) => {
          this.localidades.set(locs);
          if (locs.length > 0 && !this.form.get('localidadId')?.value) {
            this.form.patchValue({ localidadId: locs[0].id });
          }
        },
      });
  }

  onProvinciaChange(): void {
    const provId = this.form.get('provinciaId')?.value;
    if (provId) {
      this.cargarDepartamentos(provId);
    } else {
      this.departamentos.set([]);
      this.localidades.set([]);
    }
  }

  onDepartamentoChange(): void {
    const deptoId = this.form.get('departamentoId')?.value;
    if (deptoId) {
      this.cargarLocalidades(deptoId);
    } else {
      this.localidades.set([]);
    }
  }

  seleccionarActividad(actividad: 'INFRAESTRUCTURA' | 'HIGIENE_SEGURIDAD' | 'EQUIPAMIENTO_BIBLIOTECA' | null): void {
    this.actividadSeleccionada.set(actividad);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    if (actividad === 'HIGIENE_SEGURIDAD') {
      this.cargarGuiaHigiene();
    } else if (actividad === 'INFRAESTRUCTURA') {
      this.cargarInmuebles();
    }
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchQuery.set(target.value);
  }

  onFilterTenencia(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.selectedTenenciaFilter.set(target.value);
  }

  toggleEspacios(inmuebleId: string): void {
    if (this.espacioExpandidoId() === inmuebleId) {
      this.espacioExpandidoId.set(null);
    } else {
      this.espacioExpandidoId.set(inmuebleId);
    }
  }

  abrirFormularioNuevo(): void {
    this.modoEdicion.set(false);
    this.inmuebleEditandoId.set(null);
    this.form.reset({
      numeroOrden: (this.inmuebles().length + 1),
      condicionTenencia: 'PROPIO',
      situacion: 'Propiedad',
      vinculaTodasCarreras: false,
      estadoConeau: 'CARGADO',
      carrerasIds: [],
    });

    const lr = this.provincias().find((p) => p.codigo === 'LR');
    if (lr) {
      this.form.patchValue({ provinciaId: lr.id });
      this.cargarDepartamentos(lr.id);
    }

    this.carrerasSeleccionadas.set([]);
    this.carreraParaVincularId.set('');
    this.espaciosAgregados.set([]);
    this.documentosAgregados.set([]);
    this.espacioEditandoIndex.set(null);
    this.mostrarFormEspacio.set(false);
    this.mostrarConfirmacionGuardado.set(false);
    this.mostrarModalNuevoTipo.set(false);
    this.formAbierto.set(true);
  }

  abrirEditar(inm: Inmueble): void {
    this.modoEdicion.set(true);
    this.inmuebleEditandoId.set(inm.id);

    this.form.patchValue({
      numeroOrden: inm.numeroOrden,
      nombre: inm.nombre,
      domicilio: inm.domicilio,
      calle: inm.calle || '',
      numero: inm.numero || '',
      provinciaId: inm.provinciaId,
      departamentoId: inm.departamentoId,
      localidadId: inm.localidadId,
      anioConstruccion: inm.anioConstruccion,
      anioInicioInstitucion: inm.anioInicioInstitucion,
      condicionTenencia: inm.condicionTenencia,
      situacion: inm.situacion || 'Propiedad',
      vinculaTodasCarreras: inm.vinculaTodasCarreras || false,
      superficieCubiertaM2: inm.superficieCubiertaM2,
      superficieTotalM2: inm.superficieTotalM2,
      observaciones: inm.observaciones,
      estadoConeau: inm.estadoConeau || 'CARGADO',
      carrerasIds: inm.carreras ? inm.carreras.map((c) => c.id) : [],
    });

    if (inm.provinciaId) {
      this.cargarDepartamentos(inm.provinciaId);
    }
    if (inm.departamentoId) {
      this.cargarLocalidades(inm.departamentoId);
    }

    this.carrerasSeleccionadas.set(
      inm.carreras
        ? inm.carreras.map((c) => ({ id: c.id, nombre: c.nombre, sigla: c.sigla || '' } as CarreraResponse))
        : []
    );
    this.carreraParaVincularId.set('');
    this.espaciosAgregados.set(inm.espacios ? [...inm.espacios] : []);
    this.documentosAgregados.set(
      inm.documentos
        ? inm.documentos.map((d) => ({
            nombreArchivo: d.nombreArchivo,
            urlMinio: d.urlMinio,
            tipoAnexo: d.tipoAnexo,
            descripcion: d.descripcion,
          }))
        : []
    );
    this.espacioEditandoIndex.set(null);
    this.mostrarFormEspacio.set(false);
    this.mostrarConfirmacionGuardado.set(false);
    this.mostrarModalNuevoTipo.set(false);
    this.formAbierto.set(true);
  }

  cerrarFormulario(): void {
    this.formAbierto.set(false);
    this.mostrarFormEspacio.set(false);
    this.mostrarConfirmacionGuardado.set(false);
    this.mostrarModalNuevoTipo.set(false);
  }

  // --- Carreras Vinculadas ---
  vincularCarrera(): void {
    const cid = this.carreraParaVincularId();
    if (!cid) return;
    const car = this.carreras().find((c) => c.id === cid);
    if (car && !this.carrerasSeleccionadas().some((c) => c.id === cid)) {
      this.carrerasSeleccionadas.update((list) => [...list, car]);
      this.carreraParaVincularId.set('');
    }
  }

  desvincularCarrera(carreraId: string): void {
    this.carrerasSeleccionadas.update((list) => list.filter((c) => c.id !== carreraId));
  }

  // --- Espacios Físicos (Registrar, Modificar, Dar de baja) ---
  abrirNuevoEspacio(): void {
    this.espacioEditandoIndex.set(null);
    this.nuevoEspacioNombre.set('');
    this.nuevoEspacioDescInfra.set('');
    this.nuevoEspacioTipoId.set('');
    this.nuevoEspacioCantidad.set(1);
    this.nuevoEspacioCapacidad.set(30);
    this.mostrarFormEspacio.set(true);
  }

  editarEspacioFisico(index: number): void {
    const esp = this.espaciosAgregados()[index];
    if (!esp) return;
    this.espacioEditandoIndex.set(index);
    this.nuevoEspacioNombre.set(esp.denominacion);
    this.nuevoEspacioDescInfra.set(esp.descripcionInfraestructura || esp.denominacion);
    this.nuevoEspacioTipoId.set(esp.tipoEspacioId || '');
    this.nuevoEspacioCantidad.set(esp.cantidad || 1);
    this.nuevoEspacioCapacidad.set(esp.capacidadPersonasSentadas || esp.capacidadEstudiantes || 0);
    this.mostrarFormEspacio.set(true);
  }

  guardarEspacioFisico(): void {
    const nombre = this.nuevoEspacioNombre().trim();
    if (!nombre) return;

    const tipoId = this.nuevoEspacioTipoId();
    const tipoObj = this.tiposEspacio().find((t) => t.id === tipoId);

    const index = this.espacioEditandoIndex();
    const prev = index !== null ? this.espaciosAgregados()[index] : null;

    const espacioData: EspacioFisico = {
      id: prev?.id,
      denominacion: nombre,
      descripcionInfraestructura: this.nuevoEspacioDescInfra().trim() || nombre,
      tipoEspacioId: tipoId || undefined,
      tipoEspacioDenominacion: tipoObj ? tipoObj.denominacion : 'Aula Común',
      cantidad: this.nuevoEspacioCantidad() || 1,
      capacidadPersonasSentadas: this.nuevoEspacioCapacidad() || 0,
      capacidadEstudiantes: this.nuevoEspacioCapacidad() || 0,
      superficieM2: prev?.superficieM2,
      equipamientoDisponible: prev?.equipamientoDisponible,
      estadoConeau: prev?.estadoConeau || 'CARGADO',
      activo: true,
    };

    if (index !== null) {
      this.espaciosAgregados.update((list) => {
        const copy = [...list];
        copy[index] = espacioData;
        return copy;
      });
    } else {
      this.espaciosAgregados.update((list) => [...list, espacioData]);
    }

    this.espacioEditandoIndex.set(null);
    this.nuevoEspacioNombre.set('');
    this.nuevoEspacioDescInfra.set('');
    this.mostrarFormEspacio.set(false);
  }

  eliminarEspacioFisico(esp: EspacioFisico): void {
    this.espaciosAgregados.update((list) => list.filter((e) => e !== esp));
    if (this.espacioEditandoIndex() !== null) {
      this.espacioEditandoIndex.set(null);
      this.mostrarFormEspacio.set(false);
    }
  }

  // --- Tipo de Espacio (Caso 2 del Diagrama de Secuencia) ---
  abrirModalNuevoTipo(): void {
    this.nuevoTipoDenominacion.set('');
    this.nuevoTipoCodigo.set('');
    this.mostrarModalNuevoTipo.set(true);
  }

  cerrarModalNuevoTipo(): void {
    this.mostrarModalNuevoTipo.set(false);
  }

  guardarNuevoTipoEspacio(): void {
    const denom = this.nuevoTipoDenominacion().trim();
    if (!denom) return;
    this.guardandoTipoEspacio.set(true);

    this.autoevalService.crearTipoEspacio({
      denominacion: denom,
      codigo: this.nuevoTipoCodigo().trim() || undefined,
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.guardandoTipoEspacio.set(false))
      )
      .subscribe({
        next: (tipoCreado) => {
          this.tiposEspacio.update((list) => [...list, tipoCreado]);
          this.nuevoEspacioTipoId.set(tipoCreado.id);
          this.cerrarModalNuevoTipo();
          this.successMessage.set(`Nuevo tipo de espacio "${tipoCreado.denominacion}" registrado.`);
        },
        error: () => this.errorMessage.set('Error al registrar el nuevo tipo de espacio.'),
      });
  }

  // --- Documentación de Respaldo con Tipología ---
  agregarDocumentoRespaldo(): void {
    const nombre = this.inputDocNombre().trim();
    if (!nombre) return;

    this.documentosAgregados.update((list) => [
      ...list,
      {
        nombreArchivo: nombre,
        urlMinio: `/minio/acreditacion/03_01/${nombre}`,
        tipoAnexo: this.inputDocTipoAnexo(),
        descripcion: this.inputDocDescripcion().trim() || 'Documentación respaldatoria (Normativa MinIO)',
      },
    ]);

    this.inputDocNombre.set('');
    this.inputDocDescripcion.set('');
    this.inputDocTipoAnexo.set('CONVENIO_COMODATO');
  }

  eliminarDocumentoRespaldo(index: number): void {
    this.documentosAgregados.update((list) => list.filter((_, i) => i !== index));
  }

  // --- Doble Confirmación de Carga (Diagrama de Secuencia) ---
  solicitarConfirmacion(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.mostrarConfirmacionGuardado.set(true);
  }

  cancelarConfirmacion(): void {
    this.mostrarConfirmacionGuardado.set(false);
  }

  confirmarYEjectuarGuardado(): void {
    this.mostrarConfirmacionGuardado.set(false);
    this.onSubmit();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const val = this.form.value;

    const req: RegistrarInmuebleRequest = {
      numeroOrden: val.numeroOrden,
      nombre: val.nombre,
      domicilio: val.domicilio,
      calle: val.calle,
      numero: val.numero,
      provinciaId: val.provinciaId,
      departamentoId: val.departamentoId,
      localidadId: val.localidadId,
      anioConstruccion: val.anioConstruccion,
      anioInicioInstitucion: val.anioInicioInstitucion,
      propiedadInstitucion: val.condicionTenencia === 'PROPIO' || val.situacion === 'Propiedad',
      condicionTenencia: val.condicionTenencia,
      situacion: val.situacion,
      vinculaTodasCarreras: val.vinculaTodasCarreras,
      esMaestro: this.modoVista() === 'MAESTRO',
      procesoId: this.procesoId() || undefined,
      superficieCubiertaM2: val.superficieCubiertaM2,
      superficieTotalM2: val.superficieTotalM2,
      observaciones: val.observaciones,
      estadoConeau: val.estadoConeau || 'CARGADO',
      carrerasIds: val.vinculaTodasCarreras ? [] : this.carrerasSeleccionadas().map((c) => c.id),
      espacios: this.espaciosAgregados().map((e) => ({
        id: e.id,
        tipoEspacioId: e.tipoEspacioId,
        denominacion: e.denominacion,
        descripcionInfraestructura: e.descripcionInfraestructura || e.denominacion,
        cantidad: e.cantidad || 1,
        capacidadPersonasSentadas: e.capacidadPersonasSentadas || e.capacidadEstudiantes || 0,
        capacidadEstudiantes: e.capacidadEstudiantes || 0,
        superficieM2: e.superficieM2,
        equipamientoDisponible: e.equipamientoDisponible,
        estadoConeau: e.estadoConeau || 'CARGADO',
      })),
      documentos: this.documentosAgregados().map((d) => ({
        nombreArchivo: d.nombreArchivo,
        urlMinio: d.urlMinio,
        tipoAnexo: d.tipoAnexo,
        descripcion: d.descripcion,
      })),
    };

    const action$ = this.modoEdicion() && this.inmuebleEditandoId()
      ? this.autoevalService.actualizarInmueble(this.inmuebleEditandoId()!, req)
      : this.autoevalService.registrarInmueble(req);

    action$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: () => {
          this.successMessage.set(
            this.modoEdicion()
              ? 'Inmueble actualizado exitosamente.'
              : 'Inmueble registrado exitosamente.'
          );
          this.cerrarFormulario();
          this.cargarDatosIniciales();
        },
        error: () => {
          this.errorMessage.set('Error al guardar datos del inmueble.');
        },
      });
  }

  // ---------------------------------------------------------------------------
  // Extensión de Plazos (Sincronización con PT-001)
  // ---------------------------------------------------------------------------

  abrirModalExtensionPlazo(): void {
    const hoy = new Date();
    hoy.setDate(hoy.getDate() + 10);
    this.nuevaFechaLimite.set(hoy.toISOString().split('T')[0]);
    this.motivoExtension.set(
      this.actividadSeleccionada() === 'HIGIENE_SEGURIDAD'
        ? 'Prórroga para verificación de certificados municipales y bomberos de la UA'
        : 'Prórroga para relevamiento de documentación y espacios'
    );
    this.mostrarModalExtension.set(true);
  }

  cerrarModalExtension(): void {
    this.mostrarModalExtension.set(false);
  }

  confirmarExtensionPlazo(): void {
    const pId = this.procesoId() || 'ceea6f16-19b4-4dee-91ef-f38d47cdd3b5';
    const nuevaFecha = this.nuevaFechaLimite();
    if (!nuevaFecha) return;

    if (this.actividadSeleccionada() === 'HIGIENE_SEGURIDAD') {
      this.autoevalService.extenderPlazoHigiene(pId, {
        nuevaFechaLimite: nuevaFecha,
        motivo: this.motivoExtension(),
      })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.successMessage.set(`Plazo de Higiene y Seguridad extendido hasta el ${nuevaFecha}. Sincronizado en el Plan de Trabajo (Actividad 4.2).`);
            this.cerrarModalExtension();
            this.cargarGuiaHigiene();
          },
          error: () => this.errorMessage.set('No se pudo extender el plazo de Higiene y Seguridad.'),
        });
    } else {
      this.autoevalService.extenderPlazoInfraestructura(pId, {
        nuevaFechaLimite: nuevaFecha,
        motivo: this.motivoExtension(),
      })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.successMessage.set(`Plazo extendido hasta el ${nuevaFecha}. Sincronizado automáticamente con el Plan de Trabajo (hoja INFO - Actividad 4.1).`);
            this.cerrarModalExtension();
          },
          error: () => this.errorMessage.set('No se pudo extender el plazo.'),
        });
    }
  }

  // ---------------------------------------------------------------------------
  // Remisión a AEA y Actualización de Actividades en Plan de Trabajo
  // ---------------------------------------------------------------------------

  remitirADeaPorProceso(): void {
    const pId = this.procesoId() || 'ceea6f16-19b4-4dee-91ef-f38d47cdd3b5';
    this.autoevalService.verificarYRemitirPorProceso(pId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.successMessage.set(res.mensaje);
          this.cargarDatosIniciales();
        },
        error: () => this.errorMessage.set('Error al remitir verificación a la AEA.'),
      });
  }

  verificarInmueble(inm: Inmueble): void {
    this.autoevalService.verificarYRemitirADea(inm.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set(`Inmueble "${inm.nombre}" verificado.`);
          this.cargarDatosIniciales();
        },
        error: () => this.errorMessage.set('No se pudo verificar el inmueble.'),
      });
  }

  // ---------------------------------------------------------------------------
  // Métodos de Higiene y Seguridad (GT-003)
  // ---------------------------------------------------------------------------

  cargarGuiaHigiene(): void {
    this.loadingHigiene.set(true);

    const call$ = (this.modoVista() === 'PROCESO' && this.procesoId())
      ? this.autoevalService.obtenerGuiaHigienePorProceso(this.procesoId()!)
      : this.autoevalService.obtenerGuiaHigieneMaestra();

    call$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingHigiene.set(false))
      )
      .subscribe({
        next: (g) => {
          this.guiaHigiene.set(g);
          this.formHigiene.patchValue({
            instanciaResponsableUa: g.instanciaResponsableUa || '',
            politicaUnidadesApoyo: g.politicaUnidadesApoyo || '',
            aplicaUnidadesApoyo: g.aplicaUnidadesApoyo || false,
            observaciones: g.observaciones || '',
          });
        },
        error: () => {
          if (this.actividadSeleccionada() === 'HIGIENE_SEGURIDAD') {
            this.errorMessage.set('Error al cargar la Guía de Higiene y Seguridad (GT-003)');
          }
        },
      });
  }

  guardarGuiaHigiene(): void {
    const guia = this.guiaHigiene();
    if (!guia) return;

    if (this.formHigiene.invalid) {
      this.formHigiene.markAllAsTouched();
      return;
    }

    this.guardandoHigiene.set(true);
    const val = this.formHigiene.value;

    const req: ActualizarGuiaHigieneRequest = {
      instanciaResponsableUa: val.instanciaResponsableUa,
      politicaUnidadesApoyo: val.politicaUnidadesApoyo,
      aplicaUnidadesApoyo: val.aplicaUnidadesApoyo,
      observaciones: val.observaciones,
    };

    this.autoevalService.actualizarGuiaHigiene(guia.id, req)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.guardandoHigiene.set(false))
      )
      .subscribe({
        next: (g) => {
          this.guiaHigiene.set(g);
          this.successMessage.set('Guía de Higiene y Seguridad (GT-003) actualizada exitosamente.');
        },
        error: () => this.errorMessage.set('Error al guardar la guía de Higiene y Seguridad.'),
      });
  }

  abrirModalNormativaHigiene(): void {
    this.nuevaNormativaTipo.set('Resolución Rectoral');
    this.nuevaNormativaEmisor.set('Secretaría de Gestión Institucional');
    this.nuevaNormativaNumero.set('');
    this.nuevaNormativaDesc.set('');
    this.nuevaNormativaArchivo.set('');
    this.mostrarModalNormativaHigiene.set(true);
  }

  cerrarModalNormativaHigiene(): void {
    this.mostrarModalNormativaHigiene.set(false);
  }

  guardarNormativaHigiene(): void {
    const guia = this.guiaHigiene();
    if (!guia) return;

    const nombreArchivo = this.nuevaNormativaArchivo().trim();
    if (!nombreArchivo) {
      this.errorMessage.set('Debe indicar el nombre del archivo de respaldo a subir en Normativa.');
      return;
    }

    const req: DocumentoHigieneItemRequest = {
      tipoDocumentoClave: 'NORMATIVA_RESPALDO',
      tipoDocumentoNombre: this.nuevaNormativaTipo(),
      organismoEmisor: this.nuevaNormativaEmisor(),
      numeroNormativa: this.nuevaNormativaNumero(),
      descripcion: this.nuevaNormativaDesc(),
      nombreArchivo: nombreArchivo,
      obligatorio: false,
    };

    this.autoevalService.agregarDocumentoGuiaHigiene(guia.id, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set(`Documento normativo "${nombreArchivo}" archivado en subcarpeta Normativa.`);
          this.cerrarModalNormativaHigiene();
          this.cargarGuiaHigiene();
        },
        error: () => this.errorMessage.set('Error al guardar el documento normativo.'),
      });
  }

  remitirHigieneADea(): void {
    const pId = this.procesoId() || 'ceea6f16-19b4-4dee-91ef-f38d47cdd3b5';
    this.autoevalService.verificarYRemitirHigienePorProceso(pId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.successMessage.set(res.mensaje);
          this.cargarGuiaHigiene();
        },
        error: () => this.errorMessage.set('Error al remitir la Guía GT-003 a la AEA.'),
      });
  }
}
