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

  readonly actividadSeleccionada = signal<'INFRAESTRUCTURA' | 'HIGIENE_SEGURIDAD' | 'EQUIPAMIENTO_BIBLIOTECA'>('INFRAESTRUCTURA');
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
  readonly nuevoEspacioNombre = signal<string>('');
  readonly nuevoEspacioDescInfra = signal<string>('');
  readonly nuevoEspacioTipoId = signal<string>('');
  readonly nuevoEspacioCantidad = signal<number>(1);
  readonly nuevoEspacioCapacidad = signal<number>(30);
  readonly nuevoDocNombre = signal<string>('');

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

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const pId = params.get('procesoId') || params.get('convocatoriaId');
        if (pId) {
          this.procesoId.set(pId);
          this.modoVista.set('PROCESO');
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
    if (this.actividadSeleccionada() === 'INFRAESTRUCTURA') {
      this.cargarInmuebles();
    } else if (this.actividadSeleccionada() === 'HIGIENE_SEGURIDAD') {
      this.cargarGuiaHigiene();
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
    this.cargarDatosIniciales();
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

  seleccionarActividad(actividad: 'INFRAESTRUCTURA' | 'HIGIENE_SEGURIDAD' | 'EQUIPAMIENTO_BIBLIOTECA'): void {
    this.actividadSeleccionada.set(actividad);
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

    this.espaciosAgregados.set([]);
    this.nuevoDocNombre.set('');
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

    this.espaciosAgregados.set(inm.espacios ? [...inm.espacios] : []);
    this.nuevoDocNombre.set(inm.documentos && inm.documentos.length > 0 ? inm.documentos[0].nombreArchivo : '');
    this.formAbierto.set(true);
  }

  cerrarFormulario(): void {
    this.formAbierto.set(false);
    this.mostrarFormEspacio.set(false);
  }

  agregarEspacioFisico(): void {
    const nombre = this.nuevoEspacioNombre().trim();
    if (!nombre) return;

    const tipoId = this.nuevoEspacioTipoId();
    const tipoObj = this.tiposEspacio().find((t) => t.id === tipoId);

    const nuevo: EspacioFisico = {
      denominacion: nombre,
      descripcionInfraestructura: this.nuevoEspacioDescInfra().trim() || nombre,
      tipoEspacioId: tipoId || undefined,
      tipoEspacioDenominacion: tipoObj ? tipoObj.denominacion : 'Aula Común',
      cantidad: this.nuevoEspacioCantidad() || 1,
      capacidadPersonasSentadas: this.nuevoEspacioCapacidad() || 0,
      capacidadEstudiantes: this.nuevoEspacioCapacidad() || 0,
      estadoConeau: 'CARGADO',
    };

    this.espaciosAgregados.update((list) => [...list, nuevo]);
    this.nuevoEspacioNombre.set('');
    this.nuevoEspacioDescInfra.set('');
    this.mostrarFormEspacio.set(false);
  }

  eliminarEspacioFisico(esp: EspacioFisico): void {
    this.espaciosAgregados.update((list) => list.filter((e) => e !== esp));
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
      carrerasIds: val.carrerasIds || [],
      espacios: this.espaciosAgregados().map((e) => ({
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
      documentos: this.nuevoDocNombre().trim()
        ? [
            {
              nombreArchivo: this.nuevoDocNombre().trim(),
              urlMinio: `/minio/acreditacion/03_01/${this.nuevoDocNombre().trim()}`,
              tipoAnexo: val.situacion?.toUpperCase() || 'CONVENIO_COMODATO',
              descripcion: 'Documentación remitida a AEA (Subcarpeta Normativa)',
            },
          ]
        : [],
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
        error: () => this.errorMessage.set('Error al cargar la Guía de Higiene y Seguridad (GT-003)'),
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
