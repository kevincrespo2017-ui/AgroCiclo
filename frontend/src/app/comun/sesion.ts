import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Api, ApiError } from './api';
import { Sesion, Usuario } from '../modelos';

@Injectable({ providedIn: 'root' })
/**
 * Acceso: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Acceso {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  readonly usuario = signal<Usuario | null>(null);
  private restauracion?: Promise<Usuario | null>;

  constructor() {
    this.api.sesionExpirada.subscribe(() => {
      const origen = this.router.url;
      this.limpiar();
      if (!origen.startsWith('/acceso')) {
        void this.router.navigate(['/acceso'], { queryParams: { desde: origen }, replaceUrl: true });
      }
    });
  }

  restaurar(): Promise<Usuario | null> {
    const usuario = this.usuario();
    if (usuario || !this.api.token) return Promise.resolve(usuario);
    return this.restauracion ??= this.api.pedir<Usuario>('/auth/me')
      .then((perfil) => { this.usuario.set(perfil); this.api.fijarUsuarioId(perfil.id); void this.api.sincronizarPendientes(); return perfil; })
      .finally(() => { this.restauracion = undefined; });
  }

  async entrar(email: string, password: string) {
    const sesion = await this.api.pedir<Sesion>('/auth/login', 'POST', { email: email.trim(), password });
    this.api.fijarToken(sesion.token);
    this.usuario.set(sesion.usuario);
    this.api.fijarUsuarioId(sesion.usuario.id);
    void this.api.sincronizarPendientes();
  }

  async salir() {
    try {
      await this.api.pedir('/auth/logout', 'POST');
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) throw error;
    }
    this.limpiar();
  }

  limpiar() {
    this.api.fijarToken(null);
    this.api.fijarUsuarioId(null);
    this.usuario.set(null);
  }
}

export const requiereSesion: CanActivateFn = async (_ruta, estado) => {
  const acceso = inject(Acceso);
  const router = inject(Router);
  try { if (await acceso.restaurar()) return true; } catch { /* El acceso puede volver a intentar la sesión. */ }
  return router.createUrlTree(['/acceso'], { queryParams: { desde: estado.url } });
};

export const requiereAdmin: CanActivateFn = () => {
  return inject(Acceso).usuario()?.rol === 'ADMIN' || inject(Router).parseUrl('/');
};
