import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { RoleItem, UserItem, UsersService } from '../../../core/users/users.service';
import { EstructuraService, UnidadAcademicaResponse } from '../../../core/estructura/estructura.service';
import { DepartamentoAcademico } from '../../../core/designacion/designacion-parametros.models';
import { DesignacionParametrosService } from '../../../core/designacion/designacion-parametros.service';
import {
  DatosPayloadAltaMiembro,
  NotificationService,
  SolicitudNotificacion,
} from '../../../core/notifications/notification.service';

export type FiltroEstado = 'TODOS' | 'PENDIENTES' | 'ACTIVOS' | 'INACTIVOS';

export type TableRowItem =
  | {
      kind: 'solicitud';
      id: string;
      notif: SolicitudNotificacion;
      payload: DatosPayloadAltaMiembro | null;
    }
  | {
      kind: 'user';
      id: string;
      user: UserItem;
    };

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './users.html',
  styleUrl: './users.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserManagement implements OnInit {
  private readonly usersService = inject(UsersService);
  private readonly estructuraService = inject(EstructuraService);
  private readonly parametrosDocentesService = inject(DesignacionParametrosService);
  private readonly notificationService = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  // Data signals
  readonly users = signal<UserItem[]>([]);
  readonly roles = signal<RoleItem[]>([]);
  readonly unidadesAcademicas = signal<UnidadAcademicaResponse[]>([]);
  readonly departamentosAcademicos = signal<DepartamentoAcademico[]>([]);
  readonly solicitudesPendientes = signal<SolicitudNotificacion[]>([]);
  readonly procesandoSolicitudId = signal<string | null>(null);
  readonly loading = signal<boolean>(true);
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Filter signals
  readonly searchQuery = signal<string>('');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');
  readonly filtroEscuela = signal<string>('');

  // Pagination signals
  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);

  // Modal & Drawer signals
  readonly showCreateDrawer = signal<boolean>(false);
  readonly createStep = signal<number>(1);
  readonly showEditModal = signal<boolean>(false);
  readonly showConfirmModal = signal<boolean>(false);
  readonly showConfirmAltaModal = signal<boolean>(false);
  readonly showRejectConfirmModal = signal<boolean>(false);

  readonly selectedUser = signal<UserItem | null>(null);
  readonly selectedSolicitud = signal<SolicitudNotificacion | null>(null);
  readonly selectedRoles = signal<Set<string>>(new Set());

  // Forms
  readonly createForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    unidadAcademicaId: [''],
    departamentoAcademicoId: [''],
  });

  readonly editForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    newPassword: [''],
    unidadAcademicaId: [''],
    departamentoAcademicoId: [''],
  });

  // Computed data
  readonly allFilteredRows = computed<TableRowItem[]>(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const estado = this.filtroEstado();
    const escuelaId = this.filtroEscuela();
    const rows: TableRowItem[] = [];

    // 1. Solicitudes pendientes
    if (estado === 'TODOS' || estado === 'PENDIENTES') {
      const uaSeleccionada = escuelaId
        ? this.unidadesAcademicas().find((u) => u.id === escuelaId)
        : null;

      for (const notif of this.solicitudesPendientes()) {
        const payload = this.parsePayload(notif.datosPayload);

        // Filtro por escuela
        if (uaSeleccionada) {
          const carrera = (notif.carreraNombre || '').toLowerCase();
          const uaNom = uaSeleccionada.nombre.toLowerCase();
          const uaSig = (uaSeleccionada.sigla || '').toLowerCase();
          const coincideEscuela =
            carrera.includes(uaNom) || (uaSig ? carrera.includes(uaSig) : false);
          if (!coincideEscuela) {
            continue;
          }
        }

        // Filtro por búsqueda
        if (q) {
          const nombreComp = payload
            ? `${payload.apellido} ${payload.nombreCompleto}`.toLowerCase()
            : '';
          const email = (payload?.correoElectronico || '').toLowerCase();
          const dni = (payload?.documento || '').toLowerCase();
          const cuit = (payload?.cuitPasaporte || '').toLowerCase();
          const titulo = notif.titulo.toLowerCase();
          const carrera = (notif.carreraNombre || '').toLowerCase();

          const match =
            nombreComp.includes(q) ||
            email.includes(q) ||
            dni.includes(q) ||
            cuit.includes(q) ||
            titulo.includes(q) ||
            carrera.includes(q);

          if (!match) {
            continue;
          }
        }

        rows.push({
          kind: 'solicitud',
          id: notif.id,
          notif,
          payload,
        });
      }
    }

    // 2. Usuarios
    if (estado === 'TODOS' || estado === 'ACTIVOS' || estado === 'INACTIVOS') {
      for (const user of this.users()) {
        if (estado === 'ACTIVOS' && !user.active) continue;
        if (estado === 'INACTIVOS' && user.active) continue;

        if (escuelaId && user.unidadAcademicaId !== escuelaId) {
          continue;
        }

        if (q) {
          const username = user.username.toLowerCase();
          const email = user.email.toLowerCase();
          if (!username.includes(q) && !email.includes(q)) {
            continue;
          }
        }

        rows.push({
          kind: 'user',
          id: user.id,
          user,
        });
      }
    }

    return rows;
  });

  readonly totalItems = computed(() => this.allFilteredRows().length);

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalItems() / this.pageSize()))
  );

  readonly pagedRows = computed(() => {
    const page = Math.min(this.currentPage(), this.totalPages());
    const size = this.pageSize();
    const start = (page - 1) * size;
    return this.allFilteredRows().slice(start, start + size);
  });

  readonly paginationRange = computed(() => {
    const total = this.totalItems();
    if (total === 0) return { start: 0, end: 0, total: 0 };
    const page = Math.min(this.currentPage(), this.totalPages());
    const size = this.pageSize();
    const start = (page - 1) * size + 1;
    const end = Math.min(page * size, total);
    return { start, end, total };
  });

  readonly solicitudesPendientesCount = computed(
    () => this.solicitudesPendientes().length
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.estructuraService.listarUnidadesAcademicas().subscribe({
      next: (unidades) => this.unidadesAcademicas.set(unidades),
      error: (err) => console.error('Error al cargar unidades académicas:', err),
    });

    this.parametrosDocentesService.listarDepartamentos(false).subscribe({
      next: (deptos) => this.departamentosAcademicos.set(deptos.filter((d) => d.codigo !== 'SIN_ASIGNAR')),
      error: (err) => console.error('Error al cargar departamentos académicos:', err),
    });

    this.usersService.getRoles().subscribe({
      next: (roles) => this.roles.set(roles),
      error: (err) => console.error('Error al cargar roles:', err),
    });

    this.notificationService.listar('AEA', 'PENDIENTE').subscribe({
      next: (list) => this.solicitudesPendientes.set(list.filter((n) => n.estado === 'PENDIENTE')),
      error: () => {},
    });

    this.notificationService.conteoPendientes('AEA').subscribe({
      error: () => {},
    });

    this.usersService.getUsers()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (users) => this.users.set(users),
        error: (err) => {
          this.errorMessage.set('No se pudo cargar el listado de usuarios.');
          console.error(err);
        },
      });
  }

  parsePayload(json: string): DatosPayloadAltaMiembro | null {
    if (!json) return null;
    try {
      return JSON.parse(json) as DatosPayloadAltaMiembro;
    } catch {
      return null;
    }
  }

  // --- Filtering and Pagination methods ---
  setFiltroEstado(estado: FiltroEstado): void {
    this.filtroEstado.set(estado);
    this.currentPage.set(1);
  }

  filtrarPorPendientes(): void {
    this.setFiltroEstado('PENDIENTES');
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.currentPage.set(1);
  }

  onEscuelaChange(value: string): void {
    this.filtroEscuela.set(value);
    this.currentPage.set(1);
  }

  limpiarFiltros(): void {
    this.searchQuery.set('');
    this.filtroEstado.set('TODOS');
    this.filtroEscuela.set('');
    this.currentPage.set(1);
  }

  setPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  // --- Display helpers ---
  getNombreUnidadAcademica(id?: string | null): string {
    if (!id) return '—';
    const ua = this.unidadesAcademicas().find((u) => u.id === id);
    return ua ? (ua.sigla || ua.nombre) : id;
  }

  getDetalleUnidadAcademica(id?: string | null): string {
    if (!id) return '';
    const ua = this.unidadesAcademicas().find((u) => u.id === id);
    return ua ? `${ua.nombre} (${ua.sigla})` : id;
  }

  getNombreDepartamentoAcademico(id?: string | null): string {
    if (!id) return '';
    const d = this.departamentosAcademicos().find((dep) => dep.id === id);
    return d ? `${d.denominacion} (${d.codigo})` : id;
  }

  getNombreRol(code: string): string {
    const found = this.roles().find((r) => r.code === code);
    if (found?.name) {
      return found.name;
    }
    const fallbackMap: Record<string, string> = {
      AEA: 'Área de Evaluación y Acreditación (Administrador)',
      SECRETARIA_ACADEMICA: 'Secretaría de Gestión Académica',
      CONSULTA_INSTITUCIONAL: 'Consulta Institucional',
      DIRECTOR_CARRERA: 'Director de Carrera',
      DIRECTOR_ESCUELA: 'Director de Escuela',
      DIRECTOR_DEPARTAMENTO: 'Director de Departamento',
      COMISION_AUTOEVALUACION: 'Miembro de Comisión CA',
      DOCENTE: 'Docente',
      INFRAESTRUCTURA: 'Responsable de Infraestructura',
      BEDELIA: 'Responsable de Bedelía',
      HIGIENE_SEGURIDAD: 'Higiene y Seguridad',
      BIBLIOTECA: 'Responsable de Biblioteca',
      PROVEEDOR_INFORMACION: 'Proveedor de Información',
    };
    return fallbackMap[code] || code;
  }

  getDescripcionRol(code: string): string {
    const found = this.roles().find((r) => r.code === code);
    if (found?.description) return found.description;
    const descMap: Record<string, string> = {
      AEA: 'Administrador general del sistema de acreditación · AEA',
      SECRETARIA_ACADEMICA: 'Recibe, controla y aprueba solicitudes de designación docente · SECRETARIA_ACADEMICA',
      CONSULTA_INSTITUCIONAL: 'Consulta transversal de reportes docentes y curriculares · CONSULTA_INSTITUCIONAL',
      DIRECTOR_CARRERA: 'Gestiona la autoevaluación de su carrera · DIRECTOR_CARRERA',
      DIRECTOR_ESCUELA: 'Supervisa las carreras de su escuela · DIRECTOR_ESCUELA',
      DIRECTOR_DEPARTAMENTO: 'Responsable de solicitudes y docentes de su departamento · DIRECTOR_DEPARTAMENTO',
      COMISION_AUTOEVALUACION: 'Participa en la autoevaluación · COMISION_AUTOEVALUACION',
      DOCENTE: 'Responsable de actividades curriculares y programas · DOCENTE',
      INFRAESTRUCTURA: 'Gestiona datos de sedes y espacios físicos · INFRAESTRUCTURA',
      BEDELIA: 'Gestiona asignaciones de aulas y bedelía · BEDELIA',
      HIGIENE_SEGURIDAD: 'Supervisa condiciones de higiene y seguridad · HIGIENE_SEGURIDAD',
      BIBLIOTECA: 'Gestiona equipamiento y acervo bibliográfico · BIBLIOTECA',
      PROVEEDOR_INFORMACION: 'Carga y provee información solicitada · PROVEEDOR_INFORMACION',
    };
    return descMap[code] || code;
  }

  getInitials(first: string, second?: string | null): string {
    if (second) {
      const f = second.trim().charAt(0);
      const s = first.trim().charAt(0);
      return `${f}${s}`.toUpperCase();
    }
    const clean = first.trim().replace(/[^a-zA-Z0-9]/g, '');
    if (clean.length >= 2) {
      return clean.slice(0, 2).toUpperCase();
    }
    return clean.toUpperCase() || 'U';
  }

  getSelectedRoleNames(): string[] {
    return Array.from(this.selectedRoles()).map((code) => this.getNombreRol(code));
  }

  getSelectedEscuelaName(): string {
    const id = this.createForm.controls.unidadAcademicaId.value;
    if (!id) return 'Sin Escuela / Ámbito Central';
    return this.getDetalleUnidadAcademica(id);
  }

  getSelectedDepartamentoName(): string {
    const id = this.createForm.controls.departamentoAcademicoId.value;
    if (!id) return 'Sin Departamento Académico';
    return this.getNombreDepartamentoAcademico(id);
  }

  // --- Role checkboxes helper ---
  toggleRole(code: string): void {
    const next = new Set(this.selectedRoles());
    if (next.has(code)) {
      next.delete(code);
    } else {
      next.add(code);
    }
    this.selectedRoles.set(next);
  }

  isRoleSelected(code: string): boolean {
    return this.selectedRoles().has(code);
  }

  // --- Solicitudes Pendientes (Confirmación / Rechazo) ---
  abrirModalConfirmarAlta(notif: SolicitudNotificacion): void {
    this.selectedSolicitud.set(notif);
    this.errorMessage.set(null);
    this.showConfirmAltaModal.set(true);
  }

  cerrarModalConfirmarAlta(): void {
    this.showConfirmAltaModal.set(false);
    this.selectedSolicitud.set(null);
  }

  confirmarAltaDesdeModal(): void {
    const notif = this.selectedSolicitud();
    if (!notif) return;
    this.confirmarAltaMiembro(notif, () => {
      this.cerrarModalConfirmarAlta();
    });
  }

  abrirModalRechazarAlta(notif: SolicitudNotificacion): void {
    this.selectedSolicitud.set(notif);
    this.errorMessage.set(null);
    this.showRejectConfirmModal.set(true);
  }

  cerrarModalRechazarAlta(): void {
    this.showRejectConfirmModal.set(false);
    this.selectedSolicitud.set(null);
  }

  rechazarAltaDesdeModal(): void {
    const notif = this.selectedSolicitud();
    if (!notif) return;
    this.rechazarAltaMiembro(notif, () => {
      this.cerrarModalRechazarAlta();
      this.cerrarModalConfirmarAlta();
    });
  }

  confirmarAltaMiembro(notif: SolicitudNotificacion, onComplete?: () => void): void {
    if (this.procesandoSolicitudId()) return;
    this.procesandoSolicitudId.set(notif.id);
    this.errorMessage.set(null);

    this.notificationService
      .aceptar(notif.id)
      .pipe(
        finalize(() => {
          this.procesandoSolicitudId.set(null);
          onComplete?.();
        })
      )
      .subscribe({
        next: () => {
          this.successMessage.set(
            'Se confirmó el alta del usuario en el sistema y en el catálogo permanente de la Comisión de Autoevaluación.'
          );
          this.loadData();
        },
        error: (err) => {
          const msg = err?.error?.error || 'No fue posible confirmar el alta del miembro.';
          this.errorMessage.set(msg);
        },
      });
  }

  rechazarAltaMiembro(notif: SolicitudNotificacion, onComplete?: () => void): void {
    if (this.procesandoSolicitudId()) return;
    this.procesandoSolicitudId.set(notif.id);
    this.errorMessage.set(null);

    this.notificationService
      .rechazar(notif.id)
      .pipe(
        finalize(() => {
          this.procesandoSolicitudId.set(null);
          onComplete?.();
        })
      )
      .subscribe({
        next: () => {
          this.successMessage.set('Solicitud de alta rechazada.');
          this.loadData();
        },
        error: (err) => {
          const msg = err?.error?.error || 'No fue posible rechazar la solicitud.';
          this.errorMessage.set(msg);
        },
      });
  }

  // --- Create Drawer (Pantalla 2 con Stepper) ---
  openCreateModal(): void {
    this.createForm.reset({
      username: '',
      email: '',
      password: '',
      unidadAcademicaId: '',
      departamentoAcademicoId: '',
    });
    this.selectedRoles.set(new Set());
    this.createStep.set(1);
    this.errorMessage.set(null);
    this.showCreateDrawer.set(true);
  }

  closeCreateDrawer(): void {
    this.showCreateDrawer.set(false);
  }

  goToCreateStep(step: number): void {
    if (step === 2) {
      if (this.isStep1Invalid()) {
        this.markStep1Touched();
        return;
      }
    } else if (step === 3) {
      if (this.isStep1Invalid()) {
        this.markStep1Touched();
        this.createStep.set(1);
        return;
      }
      if (this.selectedRoles().size === 0) {
        this.errorMessage.set('Debe seleccionar al menos un rol.');
        this.createStep.set(2);
        return;
      }
    }
    this.errorMessage.set(null);
    this.createStep.set(step);
  }

  nextCreateStep(): void {
    const cur = this.createStep();
    if (cur === 1) {
      if (this.isStep1Invalid()) {
        this.markStep1Touched();
        return;
      }
      this.errorMessage.set(null);
      this.createStep.set(2);
    } else if (cur === 2) {
      if (this.selectedRoles().size === 0) {
        this.errorMessage.set('Debe seleccionar al menos un rol para el nuevo usuario.');
        return;
      }
      this.errorMessage.set(null);
      this.createStep.set(3);
    }
  }

  prevCreateStep(): void {
    this.createStep.update((s) => Math.max(1, s - 1));
  }

  private isStep1Invalid(): boolean {
    const { username, email, password } = this.createForm.controls;
    return username.invalid || email.invalid || password.invalid;
  }

  private markStep1Touched(): void {
    this.createForm.controls.username.markAsTouched();
    this.createForm.controls.email.markAsTouched();
    this.createForm.controls.password.markAsTouched();
  }

  submitCreate(): void {
    if (this.isStep1Invalid()) {
      this.markStep1Touched();
      this.createStep.set(1);
      return;
    }

    if (this.selectedRoles().size === 0) {
      this.errorMessage.set('Debe seleccionar al menos un rol para el usuario.');
      this.createStep.set(2);
      return;
    }

    this.submitting.set(true);
    const { username, email, password, unidadAcademicaId, departamentoAcademicoId } = this.createForm.getRawValue();
    const roleCodes = Array.from(this.selectedRoles());

    this.usersService
      .createUser({
        username,
        email,
        password,
        roleCodes,
        unidadAcademicaId: unidadAcademicaId ? unidadAcademicaId : null,
        departamentoAcademicoId: departamentoAcademicoId ? departamentoAcademicoId : null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (created) => {
          this.successMessage.set(`Usuario '${created.username}' creado con éxito.`);
          this.closeCreateDrawer();
          this.loadData();
        },
        error: (err) => {
          const msg = err.error?.error || 'Error al crear el usuario. Verifique los datos ingresados.';
          this.errorMessage.set(msg);
        },
      });
  }

  // --- Edit Modal ---
  openEditModal(user: UserItem): void {
    this.selectedUser.set(user);
    this.editForm.reset({
      username: user.username,
      email: user.email,
      newPassword: '',
      unidadAcademicaId: user.unidadAcademicaId ?? '',
      departamentoAcademicoId: user.departamentoAcademicoId ?? '',
    });
    this.selectedRoles.set(new Set(user.roleCodes));
    this.errorMessage.set(null);
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.selectedUser.set(null);
  }

  submitEdit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const user = this.selectedUser();
    if (!user) return;

    if (this.selectedRoles().size === 0) {
      this.errorMessage.set('Debe seleccionar al menos un rol.');
      return;
    }

    const { username, email, newPassword, unidadAcademicaId, departamentoAcademicoId } = this.editForm.getRawValue();
    if (newPassword && newPassword.trim().length > 0 && newPassword.trim().length < 6) {
      this.errorMessage.set('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }

    this.submitting.set(true);
    const roleCodes = Array.from(this.selectedRoles());

    this.usersService
      .updateUser(user.id, {
        username,
        email,
        roleCodes,
        newPassword: newPassword ? newPassword : undefined,
        unidadAcademicaId: unidadAcademicaId ? unidadAcademicaId : null,
        departamentoAcademicoId: departamentoAcademicoId ? departamentoAcademicoId : null,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (updated) => {
          this.successMessage.set(`Usuario '${updated.username}' actualizado con éxito.`);
          this.closeEditModal();
          this.loadData();
        },
        error: (err) => {
          const msg = err.error?.error || 'Error al actualizar el usuario.';
          this.errorMessage.set(msg);
        },
      });
  }

  // --- Status Toggle Modal (Pantalla 4) ---
  openStatusConfirm(user: UserItem): void {
    this.selectedUser.set(user);
    this.errorMessage.set(null);
    this.showConfirmModal.set(true);
  }

  closeStatusConfirm(): void {
    this.showConfirmModal.set(false);
    this.selectedUser.set(null);
  }

  confirmStatusChange(): void {
    const user = this.selectedUser();
    if (!user) return;

    const newStatus = !user.active;
    this.submitting.set(true);

    this.usersService
      .toggleStatus(user.id, newStatus)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          const actionText = newStatus ? 'activado' : 'desactivado';
          this.successMessage.set(`Usuario '${user.username}' ${actionText} con éxito.`);
          this.closeStatusConfirm();
          this.loadData();
        },
        error: (err) => {
          const msg = err.error?.error || 'Error al modificar el estado del usuario.';
          this.errorMessage.set(msg);
          this.closeStatusConfirm();
        },
      });
  }

  dismissAlerts(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }
}
