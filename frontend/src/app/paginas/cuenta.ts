import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api, mensajeError } from '../comun/api';
import { Aviso, Marca, Icono } from '../comun/ui';
import { AccesibilidadPublica } from '../comun/accesibilidad-publica';

@Component({
  selector: 'agro-cuenta', imports: [RouterLink, Aviso, Marca, Icono, AccesibilidadPublica],
  template: `<agro-accesibilidad-publica/><main class="acceso"><section class="acceso-forma"><agro-marca/><div class="acceso-centro">
    <span class="sobre-titulo">TU CAMPO, CONECTADO</span><h1>{{ titulos[modo] || 'Tu cuenta AgroCiclo' }}</h1><p>{{ textos[modo] || 'Gestiona tu acceso a AgroCiclo.' }}</p>
    @if(error()){<agro-aviso [mensaje]="error()"/>}
    @if(mensaje()){<div class="cuenta-exito" role="status">{{mensaje()}}</div>}
    @if(!completo()) {
      <form (submit)="enviar($event)">
        @if(modo==='registro') {
          <div class="campo"><label for="nombre">Nombre completo</label><input id="nombre" name="nombre" autocomplete="name" required maxlength="100"></div>
          <div class="campo"><label for="email">Correo electrónico</label><input id="email" name="email" type="email" autocomplete="email" required maxlength="254" placeholder="tu@correo.com"></div>
          <div class="campo"><label for="password">Contraseña</label><div class="campo-clave"><input id="password" name="password" [type]="mostrarClave() ? 'text' : 'password'" autocomplete="new-password" required minlength="8" maxlength="72"><button type="button" class="icono-boton" (click)="mostrarClave.update(v=>!v)" [attr.aria-label]="mostrarClave() ? 'Ocultar contraseña' : 'Mostrar contraseña'"><agro-icon [nombre]="mostrarClave() ? 'eye-off' : 'eye'"/></button></div><small>Mínimo 8 caracteres.</small></div>
          <div class="campo"><label for="confirmar">Confirma la contraseña</label><div class="campo-clave"><input id="confirmar" name="confirmar" [type]="mostrarConfirmacion() ? 'text' : 'password'" autocomplete="new-password" required minlength="8" maxlength="72"><button type="button" class="icono-boton" (click)="mostrarConfirmacion.update(v=>!v)" [attr.aria-label]="mostrarConfirmacion() ? 'Ocultar contraseña' : 'Mostrar contraseña'"><agro-icon [nombre]="mostrarConfirmacion() ? 'eye-off' : 'eye'"/></button></div></div>
        }
        @if(modo==='verificar') {
          <div class="campo"><label for="email">Correo electrónico</label><input id="email" name="email" type="email" autocomplete="email" required maxlength="254" [value]="correoActual()" (input)="correoActual.set(valor($event))"></div>
          <div class="campo"><label for="codigo">Código de verificación</label><input id="codigo" name="codigo" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{4}" minlength="4" maxlength="4" required placeholder="0000"><small>Ingresa los cuatro dígitos que enviamos a tu correo.</small></div>
        }
        @if(modo==='recuperar'||modo==='reenviar') {
          <div class="campo"><label for="email">Correo electrónico</label><input id="email" name="email" type="email" autocomplete="email" required maxlength="254" placeholder="tu@correo.com"></div>
        }
        @if(modo==='restablecer') {
          <div class="campo"><label for="password">Nueva contraseña</label><div class="campo-clave"><input id="password" name="password" [type]="mostrarClave() ? 'text' : 'password'" autocomplete="new-password" required minlength="8" maxlength="72"><button type="button" class="icono-boton" (click)="mostrarClave.update(v=>!v)" aria-label="Mostrar u ocultar contraseña"><agro-icon [nombre]="mostrarClave() ? 'eye-off' : 'eye'"/></button></div><small>Mínimo 8 caracteres.</small></div>
          <div class="campo"><label for="confirmar">Confirma la contraseña</label><div class="campo-clave"><input id="confirmar" name="confirmar" [type]="mostrarConfirmacion() ? 'text' : 'password'" autocomplete="new-password" required minlength="8" maxlength="72"><button type="button" class="icono-boton" (click)="mostrarConfirmacion.update(v=>!v)" aria-label="Mostrar u ocultar contraseña"><agro-icon [nombre]="mostrarConfirmacion() ? 'eye-off' : 'eye'"/></button></div></div>
        }
        <button class="boton primario" [disabled]="ocupado()">{{ocupado()?'Procesando…':etiquetaAccion()}}</button>
      </form>
      @if(modo==='verificar') { <button type="button" class="boton secundario cuenta-reenvio" [disabled]="ocupado()" (click)="reenviarCodigo()">Reenviar código al correo</button> }
    } @else {
      <a class="boton primario cuenta-login" routerLink="/acceso">Volver a iniciar sesión</a>
    }
    <nav class="cuenta-enlaces"><a routerLink="/acceso">Volver a iniciar sesión</a>@if(modo!=='registro'){<a routerLink="/cuenta/reenviar">Reenviar código</a>}</nav>
    @if(modo==='registro'){<p class="registro-invitacion">¿Ya tienes una cuenta? <a routerLink="/acceso">Inicia sesión</a></p>}
  </div></section><aside class="acceso-foto"><div class="relato-campo"><span>AGRICULTURA CON PROPÓSITO</span><h2>Cada ciclo cuenta.<br>Cada decisión transforma.</h2><p>Planifica, registra y descubre el potencial de tu producción.</p></div></aside></main>`
})
/**
 * CuentaPagina: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
export class CuentaPagina {
  private api = inject(Api);
  private ruta = inject(ActivatedRoute);
  modo = this.ruta.snapshot.paramMap.get('modo') || 'registro';
  titulos: Record<string,string> = { registro: 'Cultiva nuevas posibilidades', recuperar: '¿Olvidaste tu contraseña?', reenviar: 'Reenvía tu código', verificar: 'Confirma tu correo', restablecer: 'Crea una nueva contraseña' };
  textos: Record<string,string> = {
    registro: 'Crea tu cuenta operativa. Te enviaremos un código de cuatro dígitos para verificar tu correo.',
    recuperar: 'Te enviaremos un enlace seguro para recuperar el acceso.',
    reenviar: 'Escribe el correo que utilizaste al registrarte y solicita un nuevo código.',
    verificar: 'Verifica tu correo. Después, el administrador revisará y aprobará tu solicitud de acceso.',
    restablecer: 'Protege tu cuenta con una contraseña que no utilices en otros sitios.'
  };
  ocupado = signal(false); error = signal(''); mensaje = signal(''); completo = signal(false); correoActual = signal('');
  mostrarClave = signal(false); mostrarConfirmacion = signal(false);
  private get token() { return new URLSearchParams(this.ruta.snapshot.fragment || '').get('token') || ''; }
  constructor() {
    this.ruta.paramMap.subscribe(params => {
      this.modo = params.get('modo') || 'registro'; this.error.set(''); this.mensaje.set(''); this.completo.set(false);
    });
  }
  valor(evento: Event) { return (evento.target as HTMLInputElement).value; }
  etiquetaAccion() {
    return this.modo === 'registro' ? 'Crear cuenta' : this.modo === 'verificar' ? 'Verificar correo' :
      this.modo === 'reenviar' ? 'Enviar nuevo código' : this.modo === 'recuperar' ? 'Enviar enlace' : 'Actualizar contraseña';
  }
  async reenviarCodigo() {
    const email = this.correoActual().trim();
    if (!email) { this.error.set('Escribe primero tu correo electrónico.'); return; }
    this.ocupado.set(true); this.error.set(''); this.mensaje.set('');
    try {
      const r = await this.api.pedir<{mensaje:string}>('/auth/reenviar', 'POST', { email });
      this.mensaje.set(r.mensaje || 'Si la cuenta está pendiente de verificación, enviaremos un nuevo código.');
    } catch (err) { this.error.set(mensajeError(err)); }
    finally { this.ocupado.set(false); }
  }
  async enviar(e: Event) {
    e.preventDefault(); if (this.ocupado()) return;
    this.error.set(''); this.mensaje.set('');
    const f = new FormData(e.target as HTMLFormElement); const d: Record<string,string> = {};
    if (this.modo === 'registro') {
      d['nombre'] = String(f.get('nombre')).trim(); d['email'] = String(f.get('email')).trim();
      d['password'] = String(f.get('password'));
      if (d['password'] !== f.get('confirmar')) { this.error.set('Las contraseñas no coinciden.'); return; }
    }
    if (this.modo === 'verificar') {
      d['email'] = String(f.get('email')).trim(); d['codigo'] = String(f.get('codigo')).trim();
      if (!/^[0-9]{4}$/.test(d['codigo'])) { this.error.set('El código debe contener exactamente cuatro dígitos.'); return; }
    }
    if (['recuperar','reenviar'].includes(this.modo)) d['email'] = String(f.get('email')).trim();
    if (this.modo === 'restablecer') {
      d['password'] = String(f.get('password'));
      if (d['password'] !== f.get('confirmar')) { this.error.set('Las contraseñas no coinciden.'); return; }
      if (!/^[0-9a-f]{64}$/.test(this.token)) { this.error.set('El enlace no es válido. Solicita uno nuevo.'); return; }
      d['token'] = this.token;
    }
    if (!this.titulos[this.modo]) { this.error.set('Esta página no existe.'); return; }
    this.ocupado.set(true);
    try {
      const ruta = this.modo === 'registro' ? '/auth/registro' : this.modo === 'verificar' ? '/auth/verificar' :
        this.modo === 'reenviar' ? '/auth/reenviar' : this.modo === 'recuperar' ? '/auth/recuperar' : '/auth/restablecer';
      const r = await this.api.pedir<{mensaje:string}>(ruta, 'POST', d);
      if (this.modo === 'registro') {
        this.correoActual.set(d['email']); this.modo = 'verificar'; this.mensaje.set(r.mensaje);
      } else if (this.modo === 'verificar') {
        this.mensaje.set(r.mensaje); this.completo.set(true);
      } else if (this.modo === 'reenviar') {
        this.correoActual.set(d['email']); this.modo = 'verificar';
        this.mensaje.set('Si la cuenta está pendiente, se enviará un nuevo código. Ingresa los cuatro dígitos para verificar el correo.');
      } else {
        this.mensaje.set(r.mensaje); this.completo.set(true);
      }
    } catch (err) { this.error.set(mensajeError(err)); }
    finally { this.ocupado.set(false); }
  }
}
