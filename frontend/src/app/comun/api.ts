import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Subject, firstValueFrom } from 'rxjs';
import { AlmacenOffline, MutacionPendiente } from './offline';

const sessionKey = 'agrociclo-sesion';
const configuracion = globalThis as typeof globalThis & { AGROCICLO_API_URL?: string };
const base = (configuracion.AGROCICLO_API_URL || 'http://localhost:4000/api').replace(/\/$/, '');

/**
 * ApiError: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class ApiError extends Error {
  constructor(public status: number, mensaje: string, public codigo?: string,
              public campos?: Record<string, string>) {
    super(mensaje);
  }
}

export function conConsulta<T extends object>(ruta: string, filtros: T) {
  const parametros = new URLSearchParams();
  for (const [clave, valor] of Object.entries(filtros)) {
    if (valor !== '' && valor !== undefined && valor !== null) parametros.set(clave, String(valor));
  }
  return parametros.size ? ruta + '?' + parametros.toString() : ruta;
}

@Injectable({ providedIn: 'root' })
/**
 * Api: centraliza las llamadas REST, autenticación Bearer, caché local y cola IndexedDB
 * para sincronizar operaciones de campo cuando se recupera la conexión.
 */
export class Api {
  private readonly http = inject(HttpClient);
  private readonly offline = inject(AlmacenOffline);
  readonly sesionExpirada = new Subject<void>();
  readonly sincronizacionCompletada = new Subject<number>();
  private usuarioId: number | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => { void this.sincronizarPendientes().catch(() => undefined); });
      setTimeout(() => { void this.sincronizarPendientes().catch(() => undefined); }, 500);
    }
  }

  get token() { return sessionStorage.getItem(sessionKey); }

  urlImagen(url: string | null | undefined) {
    if (!url) return '/campo.webp';
    if (/^https?:\/\//i.test(url)) return url;
    if (url.startsWith('/uploads/')) return base.replace(/\/api$/, '') + url;
    return url;
  }

  fijarUsuarioId(id: number | null) { this.usuarioId = id; }

  fijarToken(token: string | null) {
    if (token) sessionStorage.setItem(sessionKey, token);
    else sessionStorage.removeItem(sessionKey);
  }

  async pedir<T>(ruta: string, metodo = 'GET', datos?: object): Promise<T> {
    const verbo = metodo.toUpperCase();
    const token = ruta.startsWith('/auth/') && !['/auth/me','/auth/logout'].includes(ruta) ? null : this.token;
    const claveCache = (this.usuarioId == null ? await this.offline.huellaSesion(token) : 'usuario-' + this.usuarioId) + ':' + base + ruta;

    if (verbo === 'GET' && typeof navigator !== 'undefined' && !navigator.onLine) {
      const guardado = await this.offline.obtenerCache(claveCache);
      if (guardado !== null) return guardado as T;
      throw new ApiError(0, 'No hay conexión y todavía no existe una copia local de esta información.');
    }

    try {
      const respuesta = await firstValueFrom(this.http.request<T>(verbo, base + ruta, {
        headers: this.cabeceras(token), body: datos
      }));
      if (verbo === 'GET') {
        try { await this.offline.guardarCache(claveCache, respuesta); } catch { /* La caché no debe bloquear una consulta válida. */ }
      }
      return respuesta;
    } catch (error) {
      const fallo = await this.interpretar(error, token);
      if (verbo === 'GET' && fallo.status === 0) {
        const guardado = await this.offline.obtenerCache(claveCache);
        if (guardado !== null) return guardado as T;
      }
      if (this.permiteColaOffline(verbo, ruta) && (fallo.status === 0 || (typeof navigator !== 'undefined' && !navigator.onLine))) {
        try {
          const pendiente: MutacionPendiente = {
            id: globalThis.crypto?.randomUUID?.() ?? String(Date.now()) + '-' + Math.random().toString(16).slice(2),
            metodo: verbo, ruta, datos, creadaEn: new Date().toISOString(),
            sesion: await this.offline.huellaSesion(token), intentos: 0
          };
          await this.offline.agregar(pendiente);
          return { offlineQueued: true, mensaje: 'Guardado en este dispositivo. Se sincronizará cuando vuelva la conexión.' } as T;
        } catch {
          throw new ApiError(0, 'No se pudo guardar la operación sin conexión. No cierres esta página e intenta nuevamente.');
        }
      }
      throw fallo;
    }
  }

  private permiteColaOffline(metodo: string, ruta: string) {
    if (!['POST','PUT','PATCH'].includes(metodo)) return false;
    return /^\/ciclos\/\d+\/(actividades(?:\/\d+(?:\/completar)?)?|consumos|cosechas|sembrar)$/.test(ruta) ||
      /^\/insumos\/\d+\/entradas$/.test(ruta);
  }

  async sincronizarPendientes() {
    if (typeof navigator === 'undefined' || !navigator.onLine || !this.token) return;
    const sesion = await this.offline.huellaSesion(this.token);
    const pendientes = await this.offline.listar();
    let sincronizados = 0;
    for (const pendiente of pendientes) {
      if (pendiente.sesion !== sesion) continue;
      try {
        await firstValueFrom(this.http.request(pendiente.metodo, base + pendiente.ruta, {
          headers: this.cabeceras(this.token), body: pendiente.datos
        }));
        await this.offline.eliminar(pendiente.id);
        sincronizados++;
      } catch (error) {
        const fallo = await this.interpretar(error, this.token);
        await this.offline.actualizarError(pendiente.id, fallo.message);
        if (fallo.status === 0 || fallo.status === 401) break;
      }
    }
    if (sincronizados) this.sincronizacionCompletada.next(sincronizados);
    await this.offline.actualizarContador();
  }

  async descargarFicha(id: number, codigo: string) {
    const token = this.token;
    try {
      const archivo = await firstValueFrom(this.http.get(base + '/reportes/ciclos/' + id + '/pdf', {
        responseType: 'blob', headers: this.cabeceras(token)
      }));
      const url = URL.createObjectURL(archivo);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = 'AgroCiclo-' + codigo + '.pdf';
      document.body.appendChild(enlace);
      enlace.click();
      enlace.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      throw await this.interpretar(error, token);
    }
  }

  async subirImagenTerreno(id: number, archivo: File) {
    const formulario = new FormData();
    formulario.append('archivo', archivo);
    const token = this.token;
    const headers = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
    try {
      return await firstValueFrom(this.http.post<any>(base + `/terrenos/${id}/imagen`, formulario, { headers }));
    } catch (error) {
      throw await this.interpretar(error, token);
    }
  }

  async descargarReporte(tipo: string) {
    const token = this.token;
    try {
      const archivo = await firstValueFrom(this.http.get(base + '/reportes/general.pdf?tipo=' + encodeURIComponent(tipo), {
        responseType: 'blob', headers: this.cabeceras(token)
      }));
      const url = URL.createObjectURL(archivo);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = 'AgroCiclo-reporte-' + tipo + '.pdf';
      document.body.appendChild(enlace);
      enlace.click();
      enlace.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1200);
    } catch (error) {
      throw await this.interpretar(error, token);
    }
  }

  private cabeceras(token: string | null) {
    return token ? new HttpHeaders({ Authorization: 'Bearer ' + token }) : new HttpHeaders();
  }

  private async interpretar(error: unknown, token: string | null): Promise<ApiError> {
    if (!(error instanceof HttpErrorResponse)) return new ApiError(0, 'No se pudo completar la solicitud.');
    if (error.status === 401 && token && token === this.token) this.sesionExpirada.next();
    if (!error.status) return new ApiError(0, 'No se pudo conectar con la API. Revisa la conexión y vuelve a intentar.');
    let cuerpo = error.error;
    if (cuerpo instanceof Blob) {
      try { cuerpo = JSON.parse(await cuerpo.text()); } catch { cuerpo = null; }
    }
    const fallo = cuerpo?.error;
    return new ApiError(error.status, fallo?.mensaje || 'No se pudo completar la solicitud.', fallo?.codigo, fallo?.campos);
  }
}

/**
 * Consulta: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Consulta<T> {
  readonly datos = signal<T | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  private version = 0;
  private ruta = '';

  constructor(private api: Api) {}

  async cargar(ruta: string = this.ruta) {
    this.ruta = ruta;
    const version = ++this.version;
    this.cargando.set(true);
    this.error.set('');
    try {
      const contenido = await this.api.pedir<T>(ruta);
      if (version === this.version) this.datos.set(contenido);
    } catch (error) {
      if (version === this.version) {
        this.error.set(mensajeError(error));
        this.datos.set(null);
      }
    } finally {
      if (version === this.version) this.cargando.set(false);
    }
  }
}

export const mensajeError = (error: unknown) => error instanceof Error ? error.message : 'No se pudo completar la solicitud.';
