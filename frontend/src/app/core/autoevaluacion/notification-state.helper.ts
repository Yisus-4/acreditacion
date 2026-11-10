import { DestroyRef, WritableSignal } from '@angular/core';

/**
 * Reusable notification auto-dismiss engine with per-signal timer isolation
 * and lifecycle teardown protection to avoid race conditions and memory leaks.
 */
export class NotificationState {
  private readonly timers = new Map<WritableSignal<string | null>, ReturnType<typeof setTimeout>>();

  /**
   * Displays a notification message in the provided signal for `duracionMs` (default: 6000ms).
   * Automatically clears any previous active timer for this signal before scheduling.
   */
  mostrar(
    signalRef: WritableSignal<string | null>,
    mensaje: string,
    duracionMs = 6000
  ): void {
    this.limpiar(signalRef);
    signalRef.set(mensaje);

    const timerId = setTimeout(() => {
      signalRef.set(null);
      this.timers.delete(signalRef);
    }, duracionMs);

    this.timers.set(signalRef, timerId);
  }

  /**
   * Clears the active notification and its timer for the provided signal.
   */
  limpiar(signalRef: WritableSignal<string | null>): void {
    const existingTimer = this.timers.get(signalRef);
    if (existingTimer !== undefined) {
      clearTimeout(existingTimer);
      this.timers.delete(signalRef);
    }
    signalRef.set(null);
  }

  /**
   * Clears all managed notification signals and cancels all active timers.
   */
  limpiarTodos(): void {
    for (const [signalRef, timerId] of this.timers.entries()) {
      clearTimeout(timerId);
      signalRef.set(null);
    }
    this.timers.clear();
  }

  /**
   * Registers automatic cleanup when the hosting component/service is destroyed.
   * If a specific `signalRef` is given, cleans only that signal; otherwise cleans all active timers.
   */
  registrarTeardown(destroyRef: DestroyRef, signalRef?: WritableSignal<string | null>): void {
    destroyRef.onDestroy(() => {
      if (signalRef) {
        this.limpiar(signalRef);
      } else {
        this.limpiarTodos();
      }
    });
  }
}
