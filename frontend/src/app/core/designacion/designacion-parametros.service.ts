import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CargoDocente,
  CatalogosDocentes,
  DedicacionDocente,
  DepartamentoAcademico,
  GuardarCargoDocenteRequest,
  GuardarDedicacionDocenteRequest,
  GuardarDepartamentoAcademicoRequest,
  GuardarPeriodoAcademicoRequest,
  GuardarTipoDesignacionRequest,
  PeriodoAcademico,
  TipoDesignacionDocente,
} from './designacion-parametros.models';

@Injectable({ providedIn: 'root' })
export class DesignacionParametrosService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/academica';

  obtenerCatalogos(incluirInactivos = false): Observable<CatalogosDocentes> {
    const params = new HttpParams().set('incluirInactivos', String(incluirInactivos));
    return this.http.get<CatalogosDocentes>(`${this.baseUrl}/parametros-docentes`, { params });
  }

  listarDepartamentos(incluirInactivos = false): Observable<DepartamentoAcademico[]> {
    const params = new HttpParams().set('incluirInactivos', String(incluirInactivos));
    return this.http.get<DepartamentoAcademico[]>(`${this.baseUrl}/departamentos`, { params });
  }

  crearDepartamento(req: GuardarDepartamentoAcademicoRequest): Observable<DepartamentoAcademico> {
    return this.http.post<DepartamentoAcademico>(`${this.baseUrl}/departamentos`, req);
  }

  actualizarDepartamento(id: string, req: GuardarDepartamentoAcademicoRequest): Observable<DepartamentoAcademico> {
    return this.http.put<DepartamentoAcademico>(`${this.baseUrl}/departamentos/${id}`, req);
  }

  crearCargo(req: GuardarCargoDocenteRequest): Observable<CargoDocente> {
    return this.http.post<CargoDocente>(`${this.baseUrl}/parametros-docentes/cargos`, req);
  }

  actualizarCargo(id: string, req: GuardarCargoDocenteRequest): Observable<CargoDocente> {
    return this.http.put<CargoDocente>(`${this.baseUrl}/parametros-docentes/cargos/${id}`, req);
  }

  crearDedicacion(req: GuardarDedicacionDocenteRequest): Observable<DedicacionDocente> {
    return this.http.post<DedicacionDocente>(`${this.baseUrl}/parametros-docentes/dedicaciones`, req);
  }

  actualizarDedicacion(id: string, req: GuardarDedicacionDocenteRequest): Observable<DedicacionDocente> {
    return this.http.put<DedicacionDocente>(`${this.baseUrl}/parametros-docentes/dedicaciones/${id}`, req);
  }

  crearTipoDesignacion(req: GuardarTipoDesignacionRequest): Observable<TipoDesignacionDocente> {
    return this.http.post<TipoDesignacionDocente>(`${this.baseUrl}/parametros-docentes/tipos-designacion`, req);
  }

  actualizarTipoDesignacion(id: string, req: GuardarTipoDesignacionRequest): Observable<TipoDesignacionDocente> {
    return this.http.put<TipoDesignacionDocente>(`${this.baseUrl}/parametros-docentes/tipos-designacion/${id}`, req);
  }

  crearPeriodo(req: GuardarPeriodoAcademicoRequest): Observable<PeriodoAcademico> {
    return this.http.post<PeriodoAcademico>(`${this.baseUrl}/parametros-docentes/periodos`, req);
  }

  actualizarPeriodo(id: string, req: GuardarPeriodoAcademicoRequest): Observable<PeriodoAcademico> {
    return this.http.put<PeriodoAcademico>(`${this.baseUrl}/parametros-docentes/periodos/${id}`, req);
  }
}
