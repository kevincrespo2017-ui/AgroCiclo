import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, filter, from, fromEvent, switchMap, timer } from 'rxjs';
import { Acceso } from './sesion';
import { Api, mensajeError } from './api';
import { Aviso, Icono, Marca } from './ui';
import { AccesibilidadPublica } from './accesibilidad-publica';
import { Traduccion } from './traduccion';
import { VozLectura } from './voz';
import { AlmacenOffline } from './offline';

/**
 * Notificacion: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
interface Notificacion {
  id: number; titulo: string; mensaje: string; tipo: string; ruta: string | null;
  creada_en: string; leida_en: string | null;
}
/**
 * RespuestaNotificaciones: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
interface RespuestaNotificaciones { datos: Notificacion[]; no_leidas: number }
/**
 * MensajeIA: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
interface MensajeIA { rol: 'user' | 'assistant'; contenido: string }

@Component({
  selector: 'agro-shell', imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono, Marca, Aviso, AccesibilidadPublica],
  templateUrl: './shell.html'
})
/**
 * Shell: estructura principal autenticada; controla navegación por rol, notificaciones, AgroCiclo AI,
 * estado de conexión y lectura accesible al cambiar de pantalla.
 */
export class Shell {
  readonly acceso = inject(Acceso);
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly traduccion = inject(Traduccion);
  private readonly voz = inject(VozLectura);
  readonly offline = inject(AlmacenOffline);
  readonly menu = signal(false);
  readonly titulo = signal(this.ruta.firstChild?.snapshot?.title ?? 'Resumen');
  readonly saliendo = signal(false);
  readonly error = signal('');
  readonly fecha = computed(() => {
    const idioma = this.traduccion.idioma();
    const locale = idioma === 'en' ? 'en-US' : idioma === 'pt' ? 'pt-BR' : 'es-EC';
    return new Intl.DateTimeFormat(locale, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date());
  });

  readonly notificacionesAbiertas = signal(false);
  readonly notificaciones = signal<Notificacion[]>([]);
  readonly noLeidas = signal(0);
  readonly cargandoNotificaciones = signal(false);


  readonly chatAbierto = signal(false);
  readonly chatMensaje = signal('');
  readonly chatEnviando = signal(false);
  readonly chatError = signal('');
  readonly chatMensajes = signal<MensajeIA[]>([
    { rol: 'assistant', contenido: '¡Hola! Soy AgroCiclo AI. Puedo ayudarte con dudas agrícolas y con el uso de la plataforma. ¿Qué necesitas saber?' }
  ]);

  readonly secciones = [
    { nombre: 'Operación', rutas: [
      { ruta: '/', texto: 'Resumen', icono: 'layout-dashboard' },
      { ruta: '/ciclos', texto: 'Ciclos agrícolas', icono: 'calendar-days' },
      { ruta: '/catalogos', texto: 'Terrenos y cultivos', icono: 'map' },
      { ruta: '/insumos', texto: 'Insumos', icono: 'package' }
    ] },
    { nombre: 'Análisis', rutas: [
       { ruta: '/productividad', texto: 'Productividad', icono: 'bar-chart-3' },
      { ruta: '/reportes', texto: 'Centro de reportes PDF', icono: 'file-down' },
      { ruta: '/inteligencia-agricola', texto: 'Inteligencia agrícola', icono: 'cloud-sun' },
      { ruta: '/auditoria', texto: 'Auditoría', icono: 'clipboard-list', admin: true },
      { ruta: '/usuarios', texto: 'Usuarios', icono: 'users', admin: true }
    ] }
  ];

  constructor() {
    this.traduccion.iniciar();
    // Mantiene un solo panel flotante abierto a la vez.
    fromEvent(window, 'agro:close-notifications')
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.notificacionesAbiertas.set(false);
        window.dispatchEvent(new CustomEvent('agro:notifications-closed'));
      });
    this.router.events.pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed()).subscribe(() => {
      this.titulo.set(this.ruta.firstChild?.snapshot?.title ?? 'AgroCiclo');
      this.menu.set(false);
      this.cerrarNotificaciones();
      if (localStorage.getItem('agro-leer-navegacion') === 'true') {
        setTimeout(() => this.voz.leerPagina(), 350);
      }
    });
    timer(0, 60000).pipe(
      switchMap(() => from(this.api.pedir<RespuestaNotificaciones>('/notificaciones')).pipe(catchError(() => EMPTY))),
      takeUntilDestroyed()
    ).subscribe(respuesta => this.actualizarEstadoNotificaciones(respuesta));
  }

  texto(texto: string) { return this.traduccion.t(texto); }

  private actualizarEstadoNotificaciones(r: RespuestaNotificaciones) {
    this.notificaciones.set(r.datos || []); this.noLeidas.set(r.no_leidas || 0);
  }

  async abrirNotificaciones() {
    this.notificacionesAbiertas.set(!this.notificacionesAbiertas());
    if (this.notificacionesAbiertas()) {
      // Oculta el disparador de accesibilidad mientras el popover ocupa esa zona.
      window.dispatchEvent(new CustomEvent('agro:notifications-opened'));
      this.cargandoNotificaciones.set(true);
      try { this.actualizarEstadoNotificaciones(await this.api.pedir<RespuestaNotificaciones>('/notificaciones')); }
      catch (e) { this.error.set(mensajeError(e)); }
      finally { this.cargandoNotificaciones.set(false); }
    } else {
      this.cerrarNotificaciones();
    }
  }

  cerrarNotificaciones() {
    this.notificacionesAbiertas.set(false);
    window.dispatchEvent(new CustomEvent('agro:notifications-closed'));
  }

  async marcarNotificacion(n: Notificacion) {
    if (!n.leida_en) {
      try { await this.api.pedir('/notificaciones/' + n.id + '/leer', 'PATCH', {}); }
      catch (e) { this.error.set(mensajeError(e)); return; }
      this.notificaciones.update(lista => lista.map(item => item.id === n.id ? { ...item, leida_en: new Date().toISOString() } : item));
      this.noLeidas.update(total => Math.max(0, total - 1));
    }
    this.cerrarNotificaciones();
    if (n.ruta && n.ruta.startsWith('/')) await this.router.navigateByUrl(n.ruta);
  }

  async marcarTodasLeidas() {
    try {
      await this.api.pedir('/notificaciones/leer-todas', 'PATCH', {});
      this.notificaciones.update(lista => lista.map(n => ({ ...n, leida_en: n.leida_en || new Date().toISOString() })));
      this.noLeidas.set(0);
    } catch (e) { this.error.set(mensajeError(e)); }
  }

  fechaNotificacion(valor: string) {
    return new Intl.DateTimeFormat(this.traduccion.idioma() === 'en' ? 'en-US' : this.traduccion.idioma() === 'pt' ? 'pt-BR' : 'es-EC', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(valor));
  }

  leerMensaje(evento: Event) { this.chatMensaje.set((evento.target as HTMLInputElement).value); }
  async enviarAsistente(evento: Event) {
    evento.preventDefault(); const pregunta = this.chatMensaje().trim();
    if (!pregunta || this.chatEnviando()) return;
    this.chatMensajes.update(lista => [...lista, { rol: 'user', contenido: pregunta }]);
    this.chatMensaje.set(''); this.chatError.set(''); this.chatEnviando.set(true);
    try {
      const historial = this.chatMensajes().slice(0, -1).slice(-8).map(m => ({ rol: m.rol, contenido: m.contenido }));
      const r = await this.api.pedir<{respuesta:string}>('/asistente/chat', 'POST', { mensaje: pregunta, historial });
      this.chatMensajes.update(lista => [...lista, { rol: 'assistant', contenido: r.respuesta }]);
    } catch (e) { this.chatError.set(mensajeError(e)); }
    finally { this.chatEnviando.set(false); }
  }

  async cerrarSesion() {
    this.saliendo.set(true);
    try { await this.acceso.salir(); await this.router.navigateByUrl('/acceso', { replaceUrl: true }); }
    catch (error) { this.error.set(mensajeError(error)); }
    finally { this.saliendo.set(false); }
  }
}
