import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { RoleItem, UserItem, UsersService } from '../../../core/users/users.service';
import { EstructuraService, UnidadAcademicaResponse } from '../../../core/estructura/estructura.service';

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
  private readonly fb = inject(FormBuilder);

  readonly users = signal<UserItem[]>([]);
  readonly roles = signal<RoleItem[]>([]);
  readonly unidadesAcademicas = signal<UnidadAcademicaResponse[]>([]);
  readonly loading = signal<boolean>(true);
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly searchQuery = signal<string>('');

  // Modals state
  readonly showCreateModal = signal<boolean>(false);
  readonly showEditModal = signal<boolean>(false);
  readonly showConfirmModal = signal<boolean>(false);
  readonly selectedUser = signal<UserItem | null>(null);
  readonly selectedRoles = signal<Set<string>>(new Set());

  // Forms
  readonly createForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    unidadAcademicaId: [''],
  });

  readonly editForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    newPassword: [''],
    unidadAcademicaId: [''],
  });

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

    this.usersService.getRoles().subscribe({
      next: (roles) => this.roles.set(roles),
      error: (err) => console.error('Error al cargar roles:', err),
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

  filteredUsers(): UserItem[] {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) {
      return this.users();
    }
    return this.users().filter(
      (u) => u.username.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    );
  }

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

  // --- Create Modal ---
  openCreateModal(): void {
    this.createForm.reset({
      username: '',
      email: '',
      password: '',
      unidadAcademicaId: '',
    });
    this.selectedRoles.set(new Set());
    this.errorMessage.set(null);
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { username, email, password, unidadAcademicaId } = this.createForm.getRawValue();
    const roleCodes = Array.from(this.selectedRoles());

    this.usersService.createUser({
      username,
      email,
      password,
      roleCodes,
      unidadAcademicaId: unidadAcademicaId ? unidadAcademicaId : null,
    })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (created) => {
          this.successMessage.set(`Usuario '${created.username}' creado con éxito.`);
          this.closeCreateModal();
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

    this.submitting.set(true);
    const { username, email, newPassword, unidadAcademicaId } = this.editForm.getRawValue();
    const roleCodes = Array.from(this.selectedRoles());

    this.usersService.updateUser(user.id, {
      username,
      email,
      roleCodes,
      newPassword: newPassword ? newPassword : undefined,
      unidadAcademicaId: unidadAcademicaId ? unidadAcademicaId : null,
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

  // --- Status Toggle Modal ---
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

    this.usersService.toggleStatus(user.id, newStatus)
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
