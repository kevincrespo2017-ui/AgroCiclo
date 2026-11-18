import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Api, mensajeError } from '../comun/api';
import { Traduccion } from '../comun/traduccion';
import { Aviso, Cargando, Icono } from '../comun/ui';
import { Terreno } from '../modelos';

/**
 * InteligenciaAgricolaPagina: integra pronóstico meteorológico y análisis orientativo
 * de imágenes de cultivos mediante los servicios externos configurados en el Backend.
 */
@Component({
  selector: 'agro-inteligencia-agricola',
  host: { class: 'agro-pagina' },
  imports: [Aviso, Cargando, Icono],
  template: `
    <header class="encabezado-pagina">
      <div><span class="sobre-titulo">AGRICULTURA BASADA EN DATOS</span>
        <h1>Inteligencia agrícola</h1>
        <p>Anticipa condiciones climáticas y analiza visualmente el estado de tus cultivos.</p>
      </div>
    </header>

    <section class="tarjeta agro-clima">
      <header class="tarjeta-titulo">
        <div><h2><span class="inteligencia-icono clima"><agro-icon nombre="cloud-sun" [tam]="21"/></span> AgroClima</h2>
          <p>Condiciones actuales y pronóstico de tres días para la ubicación de tu terreno.</p></div>
        <span class="proveedor-etiqueta">Open-Meteo · Tiempo real</span>
      </header>
      <div class="clima-contenido">
        @if (terrenosError()) { <agro-aviso [mensaje]="terrenosError()"/> }
        <form class="clima-formulario" (submit)="consultarClima($event)">
          <div class="campo">
            <label for="terreno-clima">Seleccionar terreno</label>
            <select id="terreno-clima" [value]="terrenoId() ?? ''" (change)="seleccionarTerreno($any($event.target).value)">
              <option value="">Ingresar coordenadas manualmente</option>
              @for (terreno of terrenos(); track terreno.id) {
                <option [value]="terreno.id">{{terreno.nombre}} · {{terreno.ubicacion}}</option>
              }
            </select>
          </div>
          @if (!terrenoSeleccionado() || terrenoSeleccionado()?.latitud == null || terrenoSeleccionado()?.longitud == null) {
            <div class="clima-coordenadas">
              <div class="campo"><label for="lat">Latitud</label>
                <input id="lat" name="lat" type="number" step="any" min="-90" max="90" required [value]="latitud()" (input)="latitud.set($any($event.target).value)" placeholder="-2.9001"></div>
              <div class="campo"><label for="lon">Longitud</label>
                <input id="lon" name="lon" type="number" step="any" min="-180" max="180" required [value]="longitud()" (input)="longitud.set($any($event.target).value)" placeholder="-79.0059"></div>
            </div>
          } @else {
            <div class="clima-terreno-seleccionado"><agro-icon nombre="map-pin" [tam]="17"/>
              <span><strong>{{terrenoSeleccionado()?.nombre}}</strong><small>{{terrenoSeleccionado()?.latitud}}, {{terrenoSeleccionado()?.longitud}}</small></span>
            </div>
          }
          <button class="boton primario" [disabled]="cargandoClima()">
            <agro-icon nombre="cloud-sun" [tam]="17"/>{{cargandoClima()?'Consultando…':'Consultar condiciones'}}
          </button>
        </form>

        @if (climaError()) { <agro-aviso [mensaje]="climaError()"/> }
        @if (cargandoClima()) { <agro-cargando texto="Consultando información meteorológica…"/> }
        @if (clima(); as c) {
          <div class="clima-actual">
            <div class="clima-actual-intro"><span>CONDICIONES ACTUALES</span><strong>{{c.current?.temperature_2m ?? '—'}}°C</strong>
              <small>{{c.current?.relative_humidity_2m ?? '—'}}% humedad relativa</small></div>
            <div class="clima-dato"><agro-icon nombre="cloud-sun" [tam]="19"/><span><small>Precipitación</small><strong>{{c.current?.precipitation ?? '—'}} mm</strong></span></div>
            <div class="clima-dato"><agro-icon nombre="cloud-sun" [tam]="19"/><span><small>Viento</small><strong>{{c.current?.wind_speed_10m ?? '—'}} km/h</strong></span></div>
          </div>
          @if (alertasClima().length) {
            <div class="clima-alertas">
              @for (alerta of alertasClima(); track alerta) {
                <div class="clima-alerta"><agro-icon nombre="alert-circle" [tam]="17"/>{{alerta}}</div>
              }
            </div>
          } @else {
            <div class="clima-alerta clima-alerta-ok"><agro-icon nombre="circle-check" [tam]="17"/>No se detectan umbrales meteorológicos de alerta en el pronóstico consultado.</div>
          }
          <h3 class="clima-subtitulo">Pronóstico de los próximos tres días</h3>
          <div class="clima-pronostico">
            @for (dia of pronostico(); track dia.fecha) {
              <article class="clima-dia">
                <strong>{{dia.fecha}}</strong>
                <span class="clima-dia-temperatura">{{dia.maxima}}° <small>/ {{dia.minima}}°</small></span>
                <span class="clima-dia-lluvia"><agro-icon nombre="cloud-sun" [tam]="14"/>{{dia.lluvia}}% prob. lluvia</span>
              </article>
            }
          </div>
          <p class="clima-nota">Pronóstico orientativo de Open-Meteo. La decisión de riego o aplicación de productos debe considerar también las condiciones observadas en el terreno.</p>
        }
      </div>
    </section>

    <section class="tarjeta agro-vision">
      <header class="tarjeta-titulo">
        <div><h2><span class="inteligencia-icono vision"><agro-icon nombre="eye" [tam]="21"/></span> AgroVisión AI</h2>
          <p>Obtén observaciones orientativas sobre hojas, frutos o plantas a partir de una fotografía.</p></div>
        <span class="proveedor-etiqueta">IA multimodal</span>
      </header>
      <div class="vision-contenido">
        <div class="vision-subida">
          <label class="vision-drop" for="foto">
            <agro-icon nombre="file-down" [tam]="28"/>
            <strong>Selecciona una fotografía del cultivo</strong>
            <span>JPG, PNG o WebP · máximo 5 MB</span>
            <input id="foto" type="file" accept="image/jpeg,image/png,image/webp" (change)="seleccionarFoto($event)">
          </label>
          @if (fotoBase64()) {
            <div class="vision-preview"><img [src]="fotoBase64()" alt="Vista previa de la fotografía agrícola">
              <div><strong>{{fotoNombre()}}</strong><button type="button" class="enlace" (click)="limpiarFoto()">Cambiar fotografía</button></div>
            </div>
          }
          @if (fotoError()) { <agro-aviso [mensaje]="fotoError()"/> }
          <button class="boton primario" type="button" [disabled]="!fotoBase64()||analizando()" (click)="analizarFoto()">
            <agro-icon [nombre]="analizando()?'loader-circle':'sparkles'" [tam]="17"/>
            {{analizando()?'Analizando imagen…':'Analizar fotografía'}}
          </button>
        </div>
        <div class="vision-explicacion">
          <h3>¿Qué puede observar la IA?</h3>
          <ul><li>Manchas, cambios de color y daños visibles.</li><li>Posibles señales de plagas o enfermedades.</li>
            <li>Indicadores visuales de deficiencias nutricionales.</li><li>Recomendaciones preventivas iniciales.</li></ul>
          <p>El resultado es orientativo. No sustituye el diagnóstico de un técnico agrícola ni recomienda aplicar productos sin verificar el problema.</p>
        </div>
        @if (analisis()) {
          <article class="vision-resultado"><header><agro-icon nombre="sparkles" [tam]="18"/> Análisis de AgroVisión AI</header>
            <p>{{analisis()}}</p></article>
        }
      </div>
    </section>
  `
})
/** InteligenciaAgricolaPagina: administra la consulta del clima y el análisis visual de cultivos. */
export class InteligenciaAgricolaPagina implements OnInit {
  private readonly api = inject(Api);
  private readonly traduccion = inject(Traduccion);
  readonly terrenos = signal<Terreno[]>([]);
  readonly terrenosError = signal('');
  readonly terrenoId = signal<number | null>(null);
  readonly terrenoSeleccionado = computed(() => this.terrenos().find(t => t.id === this.terrenoId()) ?? null);
  readonly latitud = signal('-2.9001');
  readonly longitud = signal('-79.0059');
  readonly clima = signal<any | null>(null);
  readonly climaError = signal('');
  readonly cargandoClima = signal(false);
  readonly pronostico = computed(() => {
    const diario = this.clima()?.daily;
    if (!diario?.time) return [];
    return diario.time.map((fecha: string, i: number) => ({
      fecha: new Intl.DateTimeFormat(this.locale(), { weekday:'short', day:'2-digit', month:'short' }).format(new Date(fecha + 'T12:00:00')),
      maxima: diario.temperature_2m_max?.[i] ?? '—',
      minima: diario.temperature_2m_min?.[i] ?? '—',
      lluvia: diario.precipitation_probability_max?.[i] ?? 0
    }));
  });
  readonly alertasClima = computed(() => {
    const diario = this.clima()?.daily;
    if (!diario?.time) return [];
    const alertas: string[] = [];
    diario.time.forEach((fecha: string, i: number) => {
      const lluvia = Number(diario.precipitation_probability_max?.[i] ?? 0);
      const maxima = Number(diario.temperature_2m_max?.[i] ?? 0);
      const minima = Number(diario.temperature_2m_min?.[i] ?? 99);
      const dia = new Intl.DateTimeFormat(this.locale(), { weekday:'long', day:'2-digit', month:'short' }).format(new Date(fecha + 'T12:00:00'));
      if (lluvia >= 70) alertas.push(`${dia}: ${this.traduccion.t('Probabilidad alta de lluvia')} (${lluvia}%). ${this.traduccion.t('Revisa el drenaje y las labores programadas.')}`);
      if (maxima >= 32) alertas.push(`${dia}: ${this.traduccion.t('Temperatura máxima elevada')} (${maxima} °C). ${this.traduccion.t('Vigila el estrés térmico del cultivo.')}`);
      if (minima <= 5) alertas.push(`${dia}: ${this.traduccion.t('Temperatura mínima baja')} (${minima} °C). ${this.traduccion.t('Verifica la sensibilidad del cultivo al frío.')}`);
    });
    return alertas;
  });

  readonly fotoBase64 = signal('');
  readonly fotoMime = signal('');
  readonly fotoNombre = signal('');
  readonly fotoError = signal('');
  readonly analisis = signal('');
  readonly analizando = signal(false);

  private locale() {
    const idioma = this.traduccion.idioma();
    return idioma === 'en' ? 'en-US' : idioma === 'pt' ? 'pt-BR' : 'es-EC';
  }

  async ngOnInit() {
    try { this.terrenos.set(await this.api.pedir<Terreno[]>('/terrenos')); }
    catch (error) { this.terrenosError.set(mensajeError(error)); }
  }

  seleccionarTerreno(valor: string) {
    const id = Number(valor);
    this.terrenoId.set(Number.isFinite(id) && id > 0 ? id : null);
    const terreno = this.terrenos().find(t => t.id === id);
    if (terreno?.latitud != null && terreno.longitud != null) {
      this.latitud.set(String(terreno.latitud));
      this.longitud.set(String(terreno.longitud));
    }
  }

  async consultarClima(evento: Event) {
    evento.preventDefault();
    const terreno = this.terrenoSeleccionado();
    const lat = terreno?.latitud != null ? Number(terreno.latitud) : Number(this.latitud());
    const lon = terreno?.longitud != null ? Number(terreno.longitud) : Number(this.longitud());
    if (!Number.isFinite(lat) || !Number.isFinite(lon)) { this.climaError.set('Ingresa coordenadas válidas.'); return; }
    this.cargandoClima.set(true); this.climaError.set(''); this.clima.set(null);
    try {
      this.clima.set(await this.api.pedir<any>(`/inteligencia-agricola/clima?lat=${lat}&lon=${lon}`));
    } catch (error) { this.climaError.set(mensajeError(error)); }
    finally { this.cargandoClima.set(false); }
  }

  seleccionarFoto(evento: Event) {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    this.fotoError.set(''); this.analisis.set(''); this.fotoBase64.set('');
    if (!archivo) return;
    if (archivo.size > 5 * 1024 * 1024) { this.fotoError.set('La fotografía debe pesar máximo 5 MB.'); return; }
    if (!['image/jpeg','image/png','image/webp'].includes(archivo.type)) { this.fotoError.set('Formato no permitido. Usa JPG, PNG o WebP.'); return; }
    this.fotoNombre.set(archivo.name); this.fotoMime.set(archivo.type);
    const lector = new FileReader();
    lector.onload = () => this.fotoBase64.set(String(lector.result || ''));
    lector.onerror = () => this.fotoError.set('No se pudo leer la fotografía.');
    lector.readAsDataURL(archivo);
  }

  limpiarFoto() {
    this.fotoBase64.set(''); this.fotoMime.set(''); this.fotoNombre.set(''); this.analisis.set('');
  }

  async analizarFoto() {
    if (!this.fotoBase64()) return;
    this.analizando.set(true); this.fotoError.set('');
    try {
      const respuesta = await this.api.pedir<{analisis:string}>('/inteligencia-agricola/analizar-foto','POST',
        { imagenBase64:this.fotoBase64(), tipoMime:this.fotoMime() });
      this.analisis.set(respuesta.analisis);
    } catch (error) { this.fotoError.set(mensajeError(error)); }
    finally { this.analizando.set(false); }
  }
}
