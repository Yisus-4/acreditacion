import { Routes } from '@angular/router';

import { authGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    title: 'Acceso — Sistema de Acreditación UNdeC',
    loadComponent: () =>
      import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'dashboard',
    title: 'Dashboard — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'admin/users',
    title: 'Administración de Usuarios — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/admin/users/users').then((m) => m.UserManagement),
  },
  {
    path: 'admin/instituciones',
    title: 'Administrar Instituciones — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/instituciones/institucion-list/institucion-list.component'
      ).then((m) => m.InstitucionListComponent),
  },
  {
    path: 'admin/instituciones/nueva',
    title: 'Registrar Institución — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/instituciones/institucion-register/institucion-register.component'
      ).then((m) => m.InstitucionRegisterComponent),
  },
  {
    path: 'admin/estructura',
    title: 'Administración Institucional y Académica — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/panel-administracion/panel-administracion.component'
      ).then((m) => m.PanelAdministracionComponent),
  },
  {
    path: 'admin/unidades-academicas',
    title: 'Unidades Académicas — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/unidades-academicas/unidades-academicas.component'
      ).then((m) => m.UnidadesAcademicasComponent),
  },
  {
    path: 'admin/sedes',
    title: 'Sedes y Localizaciones — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/sedes/sedes.component'
      ).then((m) => m.SedesComponent),
  },
  {
    path: 'admin/carreras',
    title: 'Carreras de Grado — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/carreras/carreras.component'
      ).then((m) => m.CarrerasComponent),
  },
  {
    path: 'acreditacion/convocatorias',
    title: 'Convocatorias de Acreditación — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/convocatorias/convocatoria-list/convocatoria-list.component'
      ).then((m) => m.ConvocatoriaListComponent),
  },
  {
    path: 'acreditacion/convocatorias/nueva',
    title: 'Iniciar Convocatoria — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/convocatorias/convocatoria-form/convocatoria-form.component'
      ).then((m) => m.ConvocatoriaFormComponent),
  },
  {
    path: 'acreditacion/convocatorias/:id',
    title: 'Detalle de Convocatoria y Plan de Trabajo — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/convocatorias/convocatoria-detalle/convocatoria-detalle.component'
      ).then((m) => m.ConvocatoriaDetalleComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
