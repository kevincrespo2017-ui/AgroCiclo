import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Acceso } from '../comun/sesion';
import { mensajeError, ApiError } from '../comun/api';
import { Aviso, Icono, Marca } from '../comun/ui';
import { AccesibilidadPublica } from '../comun/accesibilidad-publica';

@Component({
  selector: 'agro-acceso', imports: [Icono, Marca, Aviso, RouterLink, AccesibilidadPublica],
  template: `<agro-accesibilidad-publica/><main class="acceso">
    <section class="acceso-forma" aria-labelledby="inicio-sesion">
      <agro-marca />
      <div class="acceso-centro">
        <span class="sobre-titulo">GESTIÓN AGRÍCOLA INTELIGENTE</span><h1 id="inicio-sesion">Iniciar sesión</h1><p>Ingresa con tu cuenta de AgroCiclo.</p>
        @if (error()) { <agro-aviso [mensaje]="error()" /> }
        <form (submit)="enviar($event)">
          <div class="campo"><label for="email">Correo electrónico</label>
            <input type="email" id="email" name="email" autocomplete="username" required placeholder="nombre@ejemplo.com">
          </div>
          <div class="campo"><label for="password">Contraseña</label>
            <div class="campo-clave">
              <input [type]="visible() ? 'text' : 'password'" id="password" name="password" autocomplete="current-password" required>
              <button type="button" class="icono-boton" [attr.aria-label]="visible() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                (click)="visible.set(!visible())"><agro-icon [nombre]="visible() ? 'eye-off' : 'eye'" /></button>
            </div>
          </div>
          <button class="boton primario" [disabled]="enviando()">{{ enviando() ? 'Ingresando…' : 'Ingresar' }} <agro-icon nombre="arrow-right" /></button>
        </form><nav class="cuenta-enlaces"><a routerLink="/cuenta/recuperar">¿Olvidaste tu contraseña?</a><a routerLink="/cuenta/reenviar">Verificar mi correo</a></nav><p class="registro-invitacion">¿Primera vez aquí? <a routerLink="/cuenta/registro">Crear cuenta</a></p>
      </div>
    </section>
    <aside class="acceso-foto"><div class="relato-campo"><span>DE LA TIERRA A LOS DATOS</span><h2>Un campo lleno<br>de posibilidades.</h2><p>Conecta tus terrenos, tu equipo y cada cosecha en un solo lugar.</p><div class="campo-etiquetas"><span>Planifica</span><span>Cultiva</span><span>Crece</span></div></div></aside>
  </main>`
})
/**
 * AccesoPagina: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
export class AccesoPagina implements OnInit {
  private readonly acceso = inject(Acceso);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  readonly visible = signal(false);
  readonly enviando = signal(false);
  readonly error = signal('');

  async ngOnInit() {
    try { if (await this.acceso.restaurar()) await this.irAlDestino(); }
    catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) this.error.set(mensajeError(error));
    }
  }

  async enviar(evento: Event) {
    evento.preventDefault();
    if (this.enviando()) return;
    const datos = new FormData(evento.target as HTMLFormElement);
    this.enviando.set(true);
    this.error.set('');
    try {
      await this.acceso.entrar(String(datos.get('email')), String(datos.get('password')));
      await this.irAlDestino();
    } catch (error) {
      this.error.set(mensajeError(error));
    } finally {
      this.enviando.set(false);
    }
  }

  private irAlDestino() {
    const origen = this.ruta.snapshot.queryParamMap.get('desde') ?? '/';
    const seguro = origen.startsWith('/') && !origen.startsWith('//') && !origen.startsWith('/acceso');
    return this.router.navigateByUrl(seguro ? origen : '/', { replaceUrl: true });
  }
}
