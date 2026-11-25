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
    path: 'admin/instituciones/:id/editar',
    title: 'Editar Institución — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/instituciones/institucion-register/institucion-register.component'
      ).then((m) => m.InstitucionRegisterComponent),
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
    path: 'admin/unidades-academicas-coneau',
    title: 'Unidades Académicas — CONEAU Global UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/unidades-academicas-coneau/unidades-academicas-coneau.component'
      ).then((m) => m.UnidadesAcademicasConeauComponent),
  },
  {
    path: 'admin/unidades-academicas',
    title: 'Escuelas — Sistema de Acreditación UNdeC',
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
    redirectTo: 'academica/carreras',
    pathMatch: 'full',
  },
  {
    path: 'admin/infraestructura',
    title: 'Infraestructura Maestra — Gestión Institucional UNdeC',
    canActivate: [authGuard],
    data: { modoMaestro: true },
    loadComponent: () =>
      import(
        './features/autoevaluacion/infraestructura/infraestructura.component'
      ).then((m) => m.InfraestructuraComponent),
  },
  {
    path: 'admin/ambitos-practica',
    title: 'Ámbitos de Práctica y Convenios — Gestión Institucional UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/admin-ambitos-practica/admin-ambitos-practica.component'
      ).then((m) => m.AdminAmbitosPracticaComponent),
  },
  {
    path: 'academica',
    title: 'Gestión Académica — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/academica/panel-academica/panel-academica.component'
      ).then((m) => m.PanelAcademicaComponent),
  },
  {
    path: 'academica/carreras',
    title: 'Carreras de Grado — Gestión Académica UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/carreras/carreras.component'
      ).then((m) => m.CarrerasComponent),
  },
  {
    path: 'academica/estandares',
    title: 'Estándares por Carrera — Gestión Académica UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/academica/academica-estandares/academica-estandares.component'
      ).then((m) => m.AcademicaEstandaresComponent),
  },
  {
    path: 'academica/planes',
    title: 'Planes de Estudio y Actividades Curriculares — Gestión Académica UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/academica/academica-planes/academica-planes.component'
      ).then((m) => m.AcademicaPlanesComponent),
  },
  {
    path: 'academica/cuerpo-academico',
    title: 'Cuerpo Académico Troncal — Gestión Académica UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/academica/academica-cuerpo-academico/academica-cuerpo-academico.component'
      ).then((m) => m.AcademicaCuerpoAcademicoComponent),
  },
  {
    path: 'admin/convenios',
    title: 'Convenios Institucionales (RG-007) — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/estructura/convenios/convenios.component'
      ).then((m) => m.ConveniosComponent),
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
  {
    path: 'acreditacion/convocatorias/:id/normativa',
    title: 'Gestión de Normativa de Carrera — Sistema de Acreditación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/normativa/normativa-detalle/normativa-detalle.component'
      ).then((m) => m.NormativaDetalleComponent),
  },
  {
    path: 'autoevaluacion',
    pathMatch: 'full',
    title: 'Autoevaluación Institucional por Convocatorias — UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/autoevaluacion/convocatorias-hub/autoevaluacion-convocatorias-hub.component'
      ).then((m) => m.AutoevaluacionConvocatoriasHubComponent),
  },
  {
    path: 'autoevaluacion/infraestructura',
    title: 'Infraestructura, Equipamiento y Biblioteca — Autoevaluación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/autoevaluacion/infraestructura/infraestructura.component'
      ).then((m) => m.InfraestructuraComponent),
  },
  {
    path: 'autoevaluacion/ambitos-formacion',
    title: 'Ámbitos de Formación Práctica (RG-006) — Autoevaluación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/autoevaluacion/ambitos-practica/ambitos-practica.component'
      ).then((m) => m.AmbitosPracticaComponent),
  },
  {
    path: 'autoevaluacion/estructura-curricular',
    title: 'Estructura Curricular y Programas Analíticos (FT-001) — UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/autoevaluacion/estructura-curricular/estructura-curricular.component'
      ).then((m) => m.EstructuraCurricularComponent),
  },
  {
    path: 'autoevaluacion/cuerpo-academico',
    title: 'Cuerpo Académico (RG-010) — Autoevaluación UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/autoevaluacion/cuerpo-academico/autoevaluacion-cuerpo-academico.component'
      ).then((m) => m.AutoevaluacionCuerpoAcademicoComponent),
  },
  {
    path: 'autoevaluacion/:dimension',
    title: 'Autoevaluación y Gestión de Información — UNdeC',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/autoevaluacion/dimension-detalle/dimension-detalle.component'
      ).then((m) => m.DimensionDetalleComponent),
  },
  {
    path: 'datos-centrales',
    redirectTo: 'academica/planes',
    pathMatch: 'full',
  },
  { path: '**', redirectTo: 'dashboard' },
];
