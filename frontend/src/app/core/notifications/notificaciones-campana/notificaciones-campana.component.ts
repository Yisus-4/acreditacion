import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { NotificationService } from '../notification.service';
import { DatosPayloadAltaMiembro, SolicitudNotificacion } from '../notification.model';
import { Auth } from '../../auth/auth';

@Component({
  selector: 'app-notificaciones-campana',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './notificaciones-campana.component.html',
  styleUrl: './notificaciones-campana.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificacionesCampanaComponent implements OnInit, OnDestroy {
  private readonly notificationService = inject(NotificationService);
  private readonly auth = inject(Auth);
  private readonly elementRef = inject(ElementRef);

  readonly session = toSignal(this.auth.currentSession$, {
    initialValue: this.auth.currentSession,
  });

  readonly dropdownAbierto = signal<boolean>(false);
  readonly procesandoId = signal<string | null>(null);

  readonly conteo = this.notificationService.conteo;
  readonly notificaciones = this.notificationService.notificaciones;
  readonly cargando = this.notificationService.cargando;

  private pollInterval: any = null;

  readonly userRoles = computed(() => {
    return this.session()?.user?.roleCodes ?? [];
  });

  readonly puedeGestionar = computed(() => {
    const r = this.userRoles();
    return r.includes('AEA');
  });

  constructor() {
    effect(() => {
      if (this.puedeGestionar()) {
        this.notificationService.refrescar('AEA');
      }
    });
  }

  ngOnInit(): void {
    this.pollInterval = setInterval(() => {
      if (this.puedeGestionar()) {
        this.notificationService.refrescar('AEA');
      }
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  toggleDropdown(): void {
    const nuevoEstado = !this.dropdownAbierto();
    this.dropdownAbierto.set(nuevoEstado);
    if (nuevoEstado && this.puedeGestionar()) {
      this.notificationService.listar('AEA').subscribe({ error: () => {} });
      this.notificationService.conteoPendientes('AEA').subscribe({ error: () => {} });
    }
  }

  cerrarDropdown(): void {
    this.dropdownAbierto.set(false);
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.cerrarDropdown();
    }
  }

  parsePayload(json: string): DatosPayloadAltaMiembro | null {
    if (!json) return null;
    try {
      return JSON.parse(json) as DatosPayloadAltaMiembro;
    } catch {
      return null;
    }
  }

  aceptar(n: SolicitudNotificacion): void {
    if (this.procesandoId()) return;
    this.procesandoId.set(n.id);
    this.notificationService.aceptar(n.id).subscribe({
      next: () => this.procesandoId.set(null),
      error: (err) => {
        this.procesandoId.set(null);
        alert(err?.error?.error || 'No fue posible aceptar la solicitud.');
      },
    });
  }

  rechazar(n: SolicitudNotificacion): void {
    if (this.procesandoId()) return;
    if (!confirm(`¿Está seguro de que desea rechazar la solicitud de alta para ${n.titulo}?`)) {
      return;
    }
    this.procesandoId.set(n.id);
    this.notificationService.rechazar(n.id).subscribe({
      next: () => this.procesandoId.set(null),
      error: (err) => {
        this.procesandoId.set(null);
        alert(err?.error?.error || 'No fue posible rechazar la solicitud.');
      },
    });
  }

  marcarLeida(n: SolicitudNotificacion): void {
    if (n.leida) return;
    this.notificationService.marcarLeida(n.id).subscribe({ error: () => {} });
  }

  recargar(): void {
    if (this.puedeGestionar()) {
      this.notificationService.listar('AEA').subscribe({ error: () => {} });
      this.notificationService.conteoPendientes('AEA').subscribe({ error: () => {} });
    }
  }
}
