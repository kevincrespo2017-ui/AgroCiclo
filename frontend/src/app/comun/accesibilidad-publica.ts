import { AfterViewInit, Component, DestroyRef, signal, inject } from '@angular/core';
import { fromEvent } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Icono } from './ui';
import { Traduccion } from './traduccion';
import { VozLectura } from './voz';

/**
 * Panel global de accesibilidad.
 * Se utiliza en el inicio de sesión, registro y aplicación autenticada.
 */
@Component({
  selector: 'agro-accesibilidad-publica',
  standalone: true,
  imports: [Icono],
  template: `
    <div class="acceso-a11y" data-no-lectura>
      @if (!ocultarDisparador()) {
        <button type="button" class="a11y-disparador" aria-label="Abrir accesibilidad"
          [attr.aria-expanded]="abierto()" (click)="alternar()">
          <agro-icon nombre="accessibility" [tam]="21"/>
        </button>
      }
      @if (abierto()) {
        <section class="panel-a11y" role="dialog" aria-label="Panel de accesibilidad" (keydown.escape)="abierto.set(false)">
          <header class="a11y-cabecera">
            <div><span class="a11y-eyebrow">AGROCICLO · INCLUSIÓN</span>
              <h2><agro-icon nombre="accessibility" [tam]="20"/> {{t('Accesibilidad')}}</h2>
              <p>{{t('Personaliza la visualización sin alterar el contenido.')}}</p>
            </div>
            <button type="button" class="a11y-cerrar" aria-label="Cerrar accesibilidad" (click)="abierto.set(false)">
              <agro-icon nombre="x" [tam]="17"/>
            </button>
          </header>

          <div class="a11y-cuerpo">
            <section class="a11y-seccion">
              <h3><span>🌐</span> {{t('Idioma de la página')}}</h3>
              <p>{{t('Selecciona el idioma de los menús y textos de la interfaz.')}}</p>
              <select aria-label="Idioma de la página" [value]="traduccion.idioma()" (change)="cambiarIdioma($any($event.target).value)">
                <option value="es">🇪🇨 Español</option><option value="en">🇺🇸 English</option>
                <option value="qu">🌎 Kichwa</option><option value="pt">🇧🇷 Português</option>
              </select>
            </section>

            <section class="a11y-seccion">
              <h3><span>🎨</span> {{t('Apariencia y contraste')}}</h3>
              <p>{{t('Selecciona el modo visual más cómodo.')}}</p>
              <div class="a11y-temas">
                <button type="button" [class.seleccionado]="tema()==='claro' && !contraste()" (click)="setTema('claro')">
                  <span class="muestra normal">Aa</span><small>{{t('Normal')}}</small>
                </button>
                <button type="button" [class.seleccionado]="tema()==='suave'" (click)="setTema('suave')">
                  <span class="muestra suave">Aa</span><small>{{t('Suave')}}</small>
                </button>
                <button type="button" [class.seleccionado]="contraste()" (click)="setContraste(!contraste())">
                  <span class="muestra contraste">Aa</span><small>{{t('Alto contraste')}}</small>
                </button>
                <button type="button" [class.seleccionado]="tema()==='oscuro'" (click)="setTema('oscuro')">
                  <span class="muestra oscuro">Aa</span><small>{{t('Oscuro')}}</small>
                </button>
              </div>
              <label class="a11y-check"><input type="checkbox" [checked]="contraste()" (change)="setContraste($any($event.target).checked)">
                {{t('Activar alto contraste')}}</label>
            </section>

            <section class="a11y-seccion">
              <h3><span>🔠</span> {{t('Tamaño de visualización')}}</h3>
              <div class="a11y-escala">
                <button type="button" aria-label="Reducir tamaño" (click)="ajustarEscala(-10)">A−</button>
                <strong>{{escala()}}%</strong>
                <button type="button" aria-label="Aumentar tamaño" (click)="ajustarEscala(10)">A+</button>
              </div>
              <p>{{t('Amplía o reduce textos y controles.')}}</p>
              <label class="a11y-campo">{{t('Tipografía')}}
                <select [value]="fuente()" (change)="setFuente($any($event.target).value)">
                  <option value="sans">{{t('Sans serif')}}</option><option value="serif">{{t('Serif')}}</option>
                  <option value="dislexia">{{t('Alta legibilidad')}}</option>
                </select>
              </label>
              <label class="a11y-check"><input type="checkbox" [checked]="subrayar()" (change)="setSubrayar($any($event.target).checked)">
                {{t('Subrayar enlaces')}}</label>
            </section>

            <section class="a11y-seccion">
              <h3><span>🔊</span> {{t('Lectura y voz')}}</h3>
              <p>{{t('Selecciona una voz disponible en tu dispositivo.')}}</p>
              <label class="a11y-campo">{{t('Cambiar de voz')}}
                <select [value]="voz.vozSeleccionada()" (change)="voz.seleccionarVoz($any($event.target).value)" [disabled]="!voz.voces().length">
                  <option value="">{{t('Voz automática según idioma')}}</option>
                  @for (v of voz.voces(); track v.voiceURI) {
                    <option [value]="v.voiceURI">{{v.name}} · {{v.lang}}</option>
                  }
                  @if (!voz.voces().length) { <option value="">{{t('No hay voces disponibles')}}</option> }
                </select>
              </label>
              <div class="a11y-voz-info">
                <span>{{voz.voces().length}} {{t('voces disponibles en este dispositivo')}}</span>
                <button type="button" (click)="voz.refrescarVoces()">{{t('Actualizar voces')}}</button>
              </div>
              <p class="a11y-ayuda">{{t('Se muestran todas las voces instaladas que ofrece tu navegador. En Kichwa se usará una voz española de apoyo si el dispositivo no incluye una voz Kichwa.')}}</p>
              <button type="button" class="a11y-control a11y-probar" [disabled]="!voz.compatible()" (click)="voz.probarVoz()">
                <agro-icon nombre="volume-2" [tam]="15"/> {{t('Reproducir voz de prueba')}}
              </button>
              <label class="a11y-rango">{{t('Volumen de voz')}} <strong>{{voz.volumen()}}%</strong>
                <input type="range" min="0" max="100" step="5" [value]="voz.volumen()" (input)="voz.cambiarVolumen(+$any($event.target).value)">
              </label>
              <label class="a11y-rango">{{t('Velocidad de voz')}} <strong>{{voz.velocidad().toFixed(2)}}x</strong>
                <input type="range" min="0.6" max="1.8" step="0.1" [value]="voz.velocidad()" (input)="voz.cambiarVelocidad(+$any($event.target).value)">
              </label>
              <div class="a11y-reproductor">
                <button type="button" class="a11y-play" [attr.aria-label]="t('Leer página completa')" [disabled]="!voz.compatible()" (click)="reproducir()">
                  <agro-icon nombre="book-open" [tam]="15"/> {{t('Leer página completa')}}
                </button>
                <button type="button" class="a11y-control" [disabled]="!voz.reproduciendo() || voz.pausada()" (click)="voz.pausar()">
                  <agro-icon nombre="pause" [tam]="15"/> {{t('Pausar')}}
                </button>
                <button type="button" class="a11y-control" [disabled]="!voz.pausada()" (click)="voz.reanudar()">
                  <agro-icon nombre="play" [tam]="15"/> {{t('Continuar')}}
                </button>
                <button type="button" class="a11y-control detener" [disabled]="!voz.reproduciendo()" (click)="voz.detener()">
                  <agro-icon nombre="square" [tam]="14"/> {{t('Detener voz')}}
                </button>
              </div>
              <p class="a11y-estado-voz" aria-live="polite">{{voz.pausada() ? t('Lectura pausada') : voz.reproduciendo() ? t('Leyendo el contenido de la página') : t('La lectura está detenida')}}</p>
              <label class="a11y-check"><input type="checkbox" [checked]="leerNavegacion()" (change)="setLeerNavegacion($any($event.target).checked)">
                {{t('Leer al navegar')}}</label>
            </section>

            <section class="a11y-seccion a11y-final">
              <h3><span>⚙️</span> {{t('Lectura y movimiento')}}</h3>
              <label class="a11y-check"><input type="checkbox" [checked]="reducirMovimiento()" (change)="setReducirMovimiento($any($event.target).checked)">
                {{t('Reducir animaciones')}}</label>
              <label class="a11y-check"><input type="checkbox" [checked]="cursorGrande()" (change)="setCursorGrande($any($event.target).checked)">
                {{t('Cursor grande')}}</label>
              <button type="button" class="a11y-restablecer" (click)="restablecer()">{{t('Restablecer ajustes')}}</button>
            </section>
          </div>
        </section>
      }
    </div>
  `,
  styles: [`
    :host{position:fixed;z-index:1200;right:18px;top:12px;font-family:inherit}
    .acceso-a11y{position:relative}
    .a11y-disparador{width:46px;height:46px;display:grid;place-items:center;border:1px solid #ded8ff;border-radius:15px;background:#f5f2ff;color:#5b4ce2;box-shadow:0 5px 18px #392d8c14;cursor:pointer}
    .a11y-disparador:hover{background:#ebe7ff}
    .panel-a11y{position:absolute;right:0;top:56px;width:min(430px,calc(100vw - 24px));max-height:min(82vh,780px);overflow:auto;background:#fff;color:#202b43;border:1px solid #e3def8;border-radius:19px;box-shadow:0 24px 70px #18213c30;display:flex;flex-direction:column}
    .a11y-cabecera{display:flex;justify-content:space-between;align-items:flex-start;gap:14px;padding:19px 20px 16px;border-bottom:1px solid #eeeafa;background:linear-gradient(135deg,#fff 0%,#faf9ff 100%)}
    .a11y-eyebrow{font-size:9px;letter-spacing:.14em;font-weight:850;color:#7667dc}
    .a11y-cabecera h2{display:flex;align-items:center;gap:8px;font-size:20px;font-weight:800;margin:5px 0}
    .a11y-cabecera h2 agro-icon{color:#6555e8}
    .a11y-cabecera p,.a11y-seccion>p{font-size:12px;color:#69748a;line-height:1.5;margin:0}
    .a11y-cerrar{width:37px;height:37px;border:1px solid #e4e0f4;background:#fff;color:#5b4ce2;border-radius:11px;display:grid;place-items:center;cursor:pointer}
    .a11y-cuerpo{padding:0 20px 18px}
    .a11y-seccion{padding:18px 0;border-bottom:1px solid #eeeafa;display:grid;gap:11px}
    .a11y-seccion h3{font-size:14px;font-weight:800;color:#25324d;display:flex;align-items:center;gap:8px;margin:0}
    .a11y-seccion select{width:100%;height:43px;border:1px solid #dcd7f4;border-radius:11px;padding:0 12px;background:#fff;color:#27334c;font:inherit;font-size:13px}
    .a11y-temas{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px}
    .a11y-temas button{min-width:0;padding:7px 5px;border:1px solid #e3def8;border-radius:13px;background:#fff;display:grid;gap:7px;cursor:pointer}
    .a11y-temas button.seleccionado{border:2px solid #6756ef;padding:6px 4px;background:#faf9ff}
    .muestra{height:47px;border-radius:9px;display:grid;place-items:center;font-weight:850;font-size:18px}
    .muestra.normal{background:#f1efff;color:#5246b9}.muestra.suave{background:#f3eee5;color:#67533d}
    .muestra.contraste{background:#050505;color:#ffe900}.muestra.oscuro{background:#242136;color:#fff}
    .a11y-temas small{font-size:10px;color:#33405b;white-space:nowrap}
    .a11y-escala{display:flex;align-items:center;justify-content:space-between;padding:11px 13px;border:1px solid #e6e0ff;border-radius:13px;background:#faf9ff}
    .a11y-escala button{height:40px;min-width:50px;border:1px solid #dcd5ff;border-radius:10px;background:#fff;color:#5e4de5;font-weight:850;font-size:16px;cursor:pointer}
    .a11y-escala strong{font-size:18px}
    .a11y-campo{display:grid;gap:6px;font-size:12px;font-weight:750;color:#34415c}
    .a11y-check{display:flex;align-items:center;gap:9px;font-size:12px;color:#34415c;font-weight:650}
    .a11y-check input{width:17px;height:17px;accent-color:#6756ef}
    .a11y-ayuda{font-size:10px!important;color:#8a91a2!important}
    .a11y-voz-info{display:flex;justify-content:space-between;align-items:center;gap:8px;font-size:11px;color:#68758a}
    .a11y-voz-info button{border:0;background:transparent;color:#5d4bd9;font-size:11px;font-weight:800;cursor:pointer}
    .a11y-probar{width:100%;min-height:38px;border:1px solid #ded9f5;background:#faf9ff;color:#5144b5;border-radius:10px;display:flex;align-items:center;justify-content:center;gap:7px;font-weight:750;cursor:pointer}
    .a11y-estado-voz{font-size:10px!important;color:#68758a!important;text-align:center}
    .a11y-voz-info button:focus-visible,.a11y-probar:focus-visible{outline:3px solid #8173f3;outline-offset:2px}
    .a11y-rango{display:grid;grid-template-columns:1fr auto;align-items:center;gap:7px;font-size:12px;font-weight:700;color:#34415c}
    .a11y-rango strong{color:#6555e8}
    .a11y-rango input{grid-column:1/-1;width:100%;accent-color:#6756ef}
    .a11y-reproductor{display:grid;grid-template-columns:1fr 1fr;gap:8px}
    .a11y-reproductor button{min-height:39px;border-radius:10px;display:flex;justify-content:center;align-items:center;gap:6px;font-size:11px;font-weight:750;cursor:pointer}
    .a11y-play{grid-column:1/-1;background:#6252e8;border:1px solid #6252e8;color:white}
    .a11y-control{background:#fff;border:1px solid #e0dcf5;color:#5549a9}
    .a11y-control.detener{color:#a13c3c}
    .a11y-reproductor button:disabled{opacity:.4;cursor:not-allowed}
    .a11y-restablecer{border:0;background:transparent;color:#6252d9;font-size:12px;font-weight:750;text-align:left;cursor:pointer;padding:4px 0}
    .a11y-final{border:0}
    @media(max-width:600px){
      /* En móviles el acceso se ubica abajo a la izquierda, separado de Notificaciones. */
      :host{right:auto;left:12px;top:auto;bottom:12px}
      .panel-a11y{top:auto;bottom:56px;left:0;right:auto;max-height:calc(100dvh - 90px);width:min(430px,calc(100vw - 20px))}
      .a11y-cuerpo{padding-inline:15px}.a11y-cabecera{padding:16px}.a11y-temas{gap:5px}.a11y-temas small{font-size:9px}
    }
    @media(prefers-reduced-motion:reduce){.panel-a11y *{scroll-behavior:auto!important;transition:none!important}}
  `]
})
/**
 * AccesibilidadPublica: panel flotante global para idioma, tema, contraste, escala, tipografía,
 * lectura por voz, navegación hablada y preferencias de movimiento/cursor.
 */
export class AccesibilidadPublica implements AfterViewInit {
  readonly abierto = signal(false);
  readonly ocultarDisparador = signal(false);
  readonly tema = signal(localStorage.getItem('agro-tema') || 'claro');
  readonly contraste = signal(localStorage.getItem('agro-contraste') === 'true');
  readonly escala = signal(Number(localStorage.getItem('agro-escala') || 100));
  readonly fuente = signal(localStorage.getItem('agro-fuente') || 'sans');
  readonly subrayar = signal(localStorage.getItem('agro-subrayar') === 'true');
  readonly leerNavegacion = signal(localStorage.getItem('agro-leer-navegacion') === 'true');
  readonly reducirMovimiento = signal(localStorage.getItem('agro-reducir-movimiento') === 'true');
  readonly cursorGrande = signal(localStorage.getItem('agro-cursor-grande') === 'true');
  private readonly destroyRef = inject(DestroyRef);

  constructor(readonly traduccion: Traduccion, readonly voz: VozLectura) {
    this.traduccion.iniciar();
    this.aplicar();
    // Cierra accesibilidad cuando se abre otro popover para evitar superposiciones.
    fromEvent(window, 'agro:close-accessibility')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.abierto.set(false));
    fromEvent(window, 'agro:notifications-opened')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { this.abierto.set(false); this.ocultarDisparador.set(true); });
    fromEvent(window, 'agro:notifications-closed')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.ocultarDisparador.set(false));
  }

  alternar() {
    const abrir = !this.abierto();
    if (abrir) window.dispatchEvent(new CustomEvent('agro:close-notifications'));
    this.abierto.set(abrir);
  }

  ngAfterViewInit() {
    if (this.leerNavegacion()) setTimeout(() => this.voz.leerPagina(), 400);
  }

  t(texto: string) { return this.traduccion.t(texto); }
  cambiarIdioma(valor: string) { this.traduccion.cambiar(valor); }

  setTema(valor: string) {
    this.tema.set(valor);
    this.contraste.set(false);
    localStorage.setItem('agro-tema', valor);
    localStorage.setItem('agro-contraste', String(this.contraste()));
    this.aplicar();
  }
  setContraste(valor: boolean) {
    this.contraste.set(valor);
    if (valor) this.tema.set('claro');
    localStorage.setItem('agro-contraste', String(valor));
    localStorage.setItem('agro-tema', this.tema());
    this.aplicar();
  }
  ajustarEscala(delta: number) {
    const valor = Math.max(90, Math.min(150, this.escala() + delta));
    this.escala.set(valor); localStorage.setItem('agro-escala', String(valor)); this.aplicar();
  }
  setFuente(valor: string) { this.fuente.set(valor); localStorage.setItem('agro-fuente', valor); this.aplicar(); }
  setSubrayar(valor: boolean) { this.subrayar.set(valor); localStorage.setItem('agro-subrayar', String(valor)); this.aplicar(); }
  setLeerNavegacion(valor: boolean) { this.leerNavegacion.set(valor); localStorage.setItem('agro-leer-navegacion', String(valor)); }
  setReducirMovimiento(valor: boolean) {
    this.reducirMovimiento.set(valor); localStorage.setItem('agro-reducir-movimiento', String(valor)); this.aplicar();
  }
  setCursorGrande(valor: boolean) {
    this.cursorGrande.set(valor); localStorage.setItem('agro-cursor-grande', String(valor)); this.aplicar();
  }
  reproducir() { this.voz.leerPagina(); }
  restablecer() {
    for (const clave of ['agro-tema','agro-contraste','agro-escala','agro-fuente','agro-subrayar','agro-leer-navegacion','agro-reducir-movimiento','agro-cursor-grande','agro-voz-id','agro-voz-volumen','agro-voz-velocidad']) localStorage.removeItem(clave);
    this.tema.set('claro'); this.contraste.set(false); this.escala.set(100); this.fuente.set('sans');
    this.subrayar.set(false); this.leerNavegacion.set(false); this.reducirMovimiento.set(false); this.cursorGrande.set(false);
    this.voz.cambiarVolumen(80); this.voz.cambiarVelocidad(1); this.voz.seleccionarVoz('');
    this.voz.detener(); this.aplicar();
  }
  private aplicar() {
    const html = document.documentElement;
    // Escala real de toda la interfaz (textos, controles, tarjetas y paneles).
    // CSS zoom aplica también a los componentes que utilizan medidas en px.
    html.style.setProperty('--agro-escala', `${this.escala()}%`);
    html.style.setProperty('--agro-escala-factor', String(this.escala() / 100));
    html.dataset['tema'] = this.tema();
    html.dataset['contraste'] = String(this.contraste());
    html.dataset['escala'] = String(this.escala());
    html.dataset['fuente'] = this.fuente();
    html.dataset['subrayar'] = String(this.subrayar());
    html.dataset['reducirMovimiento'] = String(this.reducirMovimiento());
    html.dataset['cursorGrande'] = String(this.cursorGrande());
  }
}
