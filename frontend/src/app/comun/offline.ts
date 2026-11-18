import { Injectable, signal } from '@angular/core';

/** MutacionPendiente: define una operación de campo guardada localmente para sincronizarse después. */
export interface MutacionPendiente {
  id: string;
  metodo: string;
  ruta: string;
  datos?: object;
  creadaEn: string;
  sesion: string;
  intentos: number;
  error?: string;
}

/** CacheApi: estructura persistida para recuperar consultas cuando el dispositivo está desconectado. */
interface CacheApi { clave: string; valor: unknown; guardadoEn: string }

/**
 * Almacén local de AgroCiclo.
 * Guarda respuestas GET recientes y una cola IndexedDB de registros de campo
 * que esperan conexión para sincronizarse con el Backend.
 */
@Injectable({ providedIn: 'root' })
export class AlmacenOffline {
  readonly conectado = signal(typeof navigator === 'undefined' ? true : navigator.onLine);
  readonly pendientes = signal(0);
  private base?: Promise<IDBDatabase>;

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => this.conectado.set(true));
      window.addEventListener('offline', () => this.conectado.set(false));
      void this.actualizarContador();
    }
  }

  async huellaSesion(token: string | null): Promise<string> {
    const texto = token || 'sesion-publica';
    if (globalThis.crypto?.subtle) {
      const bytes = new TextEncoder().encode(texto);
      const hash = await crypto.subtle.digest('SHA-256', bytes);
      return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2,'0')).join('');
    }
    return texto.length + '-' + texto.slice(0,8);
  }

  async obtenerCache(clave: string): Promise<unknown | null> {
    const db = await this.abrir();
    return new Promise(resolve => {
      const req = db.transaction('cache','readonly').objectStore('cache').get(clave);
      req.onsuccess = () => resolve((req.result as CacheApi | undefined)?.valor ?? null);
      req.onerror = () => resolve(null);
    });
  }

  async guardarCache(clave: string, valor: unknown) {
    const db = await this.abrir();
    await new Promise<void>((resolve,reject) => {
      const tx = db.transaction('cache','readwrite');
      tx.objectStore('cache').put({ clave, valor, guardadoEn: new Date().toISOString() } satisfies CacheApi);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async agregar(mutacion: MutacionPendiente) {
    const db = await this.abrir();
    await new Promise<void>((resolve,reject) => {
      const tx = db.transaction('cola','readwrite');
      tx.objectStore('cola').put(mutacion);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
    await this.actualizarContador();
  }

  async listar(): Promise<MutacionPendiente[]> {
    const db = await this.abrir();
    return new Promise(resolve => {
      const req = db.transaction('cola','readonly').objectStore('cola').getAll();
      req.onsuccess = () => resolve((req.result as MutacionPendiente[]).sort((a,b)=>a.creadaEn.localeCompare(b.creadaEn)));
      req.onerror = () => resolve([]);
    });
  }

  async eliminar(id: string) {
    const db = await this.abrir();
    await new Promise<void>((resolve,reject) => {
      const tx = db.transaction('cola','readwrite');
      tx.objectStore('cola').delete(id);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
    await this.actualizarContador();
  }

  async actualizarError(id: string, mensaje: string) {
    const db = await this.abrir();
    const lista = await this.listar();
    const registro = lista.find(x => x.id === id);
    if (!registro) return;
    registro.intentos++;
    registro.error = mensaje;
    await new Promise<void>((resolve,reject) => {
      const tx = db.transaction('cola','readwrite');
      tx.objectStore('cola').put(registro);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async actualizarContador() {
    try { this.pendientes.set((await this.listar()).length); } catch { this.pendientes.set(0); }
  }

  private abrir(): Promise<IDBDatabase> {
    if (this.base) return this.base;
    this.base = new Promise((resolve,reject) => {
      const solicitud = indexedDB.open('agrociclo-offline',1);
      solicitud.onupgradeneeded = () => {
        const db = solicitud.result;
        if (!db.objectStoreNames.contains('cache')) db.createObjectStore('cache',{keyPath:'clave'});
        if (!db.objectStoreNames.contains('cola')) db.createObjectStore('cola',{keyPath:'id'});
      };
      solicitud.onsuccess = () => resolve(solicitud.result);
      solicitud.onerror = () => reject(solicitud.error);
    });
    return this.base;
  }
}
