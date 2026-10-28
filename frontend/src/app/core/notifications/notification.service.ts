import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ConteoNotificaciones, SolicitudNotificacion } from './notification.model';

export * from './notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly http = inject(HttpClient);

  readonly conteo = signal<number>(0);
  readonly notificaciones = signal<SolicitudNotificacion[]>([]);
  readonly cargando = signal<boolean>(false);

  listar(rol = 'AEA', estado?: string): Observable<SolicitudNotificacion[]> {
    let params = new HttpParams().set('rol', rol);
    if (estado) {
      params = params.set('estado', estado);
    }
    this.cargando.set(true);
    return this.http.get<SolicitudNotificacion[]>('/api/notificaciones', { params }).pipe(
      tap({
        next: (items) => {
          this.notificaciones.set(items);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      })
    );
  }

  conteoPendientes(rol = 'AEA'): Observable<ConteoNotificaciones> {
    const params = new HttpParams().set('rol', rol);
    return this.http.get<ConteoNotificaciones>('/api/notificaciones/conteo-pendientes', { params }).pipe(
      tap((res) => {
        this.conteo.set(res.conteo);
      })
    );
  }

  aceptar(id: string): Observable<SolicitudNotificacion> {
    return this.http.post<SolicitudNotificacion>(`/api/notificaciones/${id}/aceptar`, {}).pipe(
      tap((actualizada) => {
        this.actualizarItem(actualizada);
        this.decrementarConteo();
      })
    );
  }

  rechazar(id: string): Observable<SolicitudNotificacion> {
    return this.http.post<SolicitudNotificacion>(`/api/notificaciones/${id}/rechazar`, {}).pipe(
      tap((actualizada) => {
        this.actualizarItem(actualizada);
        this.decrementarConteo();
      })
    );
  }

  marcarLeida(id: string): Observable<SolicitudNotificacion> {
    return this.http.patch<SolicitudNotificacion>(`/api/notificaciones/${id}/marcar-leida`, {}).pipe(
      tap((actualizada) => {
        this.actualizarItem(actualizada);
      })
    );
  }

  refrescar(rol = 'AEA'): void {
    this.conteoPendientes(rol).subscribe({ error: () => {} });
  }

  private actualizarItem(actualizada: SolicitudNotificacion): void {
    const actuales = this.notificaciones();
    const idx = actuales.findIndex((n) => n.id === actualizada.id);
    if (idx !== -1) {
      const actualizados = [...actuales];
      actualizados[idx] = actualizada;
      this.notificaciones.set(actualizados);
    }
  }

  private decrementarConteo(): void {
    const actual = this.conteo();
    if (actual > 0) {
      this.conteo.set(actual - 1);
    }
  }
}
