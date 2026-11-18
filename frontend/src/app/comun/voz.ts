import { Injectable, signal } from '@angular/core';

/**
 * VozLectura: administra la lectura de contenido de AgroCiclo mediante Web Speech API.
 * La lista de voces depende del navegador y del sistema operativo; no se inventan voces
 * ni se descargan servicios externos. Se lee el contenido principal, nunca el panel a11y.
 */
@Injectable({ providedIn: 'root' })
export class VozLectura {
  readonly voces = signal<SpeechSynthesisVoice[]>([]);
  readonly vozSeleccionada = signal(localStorage.getItem('agro-voz-id') || '');
  readonly volumen = signal(Number(localStorage.getItem('agro-voz-volumen') || 80));
  readonly velocidad = signal(Number(localStorage.getItem('agro-voz-velocidad') || 1));
  readonly reproduciendo = signal(false);
  readonly pausada = signal(false);
  readonly compatible = signal(typeof window !== 'undefined' && 'speechSynthesis' in window);

  private fragmentos: string[] = [];
  private indice = 0;
  private idioma = 'es-EC';
  private generacion = 0;

  constructor() {
    if (!this.compatible()) return;
    this.refrescarVoces();
    window.speechSynthesis.addEventListener('voiceschanged', () => this.refrescarVoces());
    // Algunos navegadores entregan la lista después de la primera consulta.
    window.setTimeout(() => this.refrescarVoces(), 250);
    window.setTimeout(() => this.refrescarVoces(), 1000);
  }

  /** Recarga todas las voces instaladas y disponibles en el navegador. */
  refrescarVoces() {
    if (!this.compatible()) return;
    const disponibles = window.speechSynthesis.getVoices();
    this.voces.set(disponibles);
  }

  seleccionarVoz(uri: string) {
    this.vozSeleccionada.set(uri);
    localStorage.setItem('agro-voz-id', uri);
    this.refrescarVoces();
  }

  cambiarVolumen(valor: number) {
    const seguro = Math.max(0, Math.min(100, valor));
    this.volumen.set(seguro);
    localStorage.setItem('agro-voz-volumen', String(seguro));
  }

  cambiarVelocidad(valor: number) {
    const segura = Math.max(0.5, Math.min(2, valor));
    this.velocidad.set(segura);
    localStorage.setItem('agro-voz-velocidad', String(segura));
  }

  /** Lee la pantalla activa: el main de la aplicación o el contenido de acceso/registro. */
  leerPagina() {
    if (!this.compatible()) return;
    const raiz = document.querySelector<HTMLElement>('#contenido')
      ?? document.querySelector<HTMLElement>('main.acceso')
      ?? document.querySelector<HTMLElement>('.acceso-centro');
    if (!raiz) return;

    const raices: HTMLElement[] = raiz.matches('main.acceso')
      ? [raiz.querySelector<HTMLElement>('.acceso-centro'), raiz.querySelector<HTMLElement>('.relato-campo')]
          .filter((n): n is HTMLElement => !!n)
      : [raiz];

    const bloques: string[] = [];
    for (const elemento of raices) {
      const walker = document.createTreeWalker(elemento, NodeFilter.SHOW_TEXT, {
        acceptNode: (nodo: Node) => {
          const padre = nodo.parentElement;
          if (!padre || !nodo.nodeValue?.trim()) return NodeFilter.FILTER_REJECT;
          if (padre.closest(
            '[data-no-lectura],agro-accesibilidad-publica,.panel-a11y,script,style,svg,code,pre,' +
            'input,textarea,select,[aria-hidden="true"],[hidden],.sr-only'
          )) return NodeFilter.FILTER_REJECT;
          const estilo = window.getComputedStyle(padre);
          if (estilo.display === 'none' || estilo.visibility === 'hidden') return NodeFilter.FILTER_REJECT;
          return NodeFilter.FILTER_ACCEPT;
        }
      });
      const textos: string[] = [];
      while (walker.nextNode()) {
        const valor = walker.currentNode.nodeValue?.replace(/\\s+/g, ' ').trim();
        if (valor) textos.push(valor);
      }
      if (textos.length) bloques.push(textos.join('. '));
    }

    const texto = bloques.join('. ').replace(/(?:\\.\\s*){2,}/g, '. ').trim();
    if (texto) this.leer(texto, document.documentElement.lang || 'es');
  }

  /** Reproduce una frase corta para comprobar la voz seleccionada. */
  probarVoz() {
    if (!this.compatible()) return;
    this.detener();
    const idioma = this.codigoIdioma(document.documentElement.lang || 'es');
    const voz = this.obtenerVoz(idioma);
    const frase = idioma.startsWith('en') ? 'This is a voice test for AgroCiclo.'
      : idioma.startsWith('pt') ? 'Este é um teste de voz do AgroCiclo.'
      : 'Esta es una prueba de voz de AgroCiclo.';
    const generacion = ++this.generacion;
    const lectura = new SpeechSynthesisUtterance(frase);
    lectura.lang = voz?.lang || idioma;
    lectura.rate = this.velocidad();
    lectura.volume = this.volumen() / 100;
    if (voz) lectura.voice = voz;
    this.reproduciendo.set(true);
    lectura.onend = () => {
      if (generacion !== this.generacion) return;
      this.reproduciendo.set(false); this.pausada.set(false);
    };
    lectura.onerror = () => {
      if (generacion !== this.generacion) return;
      this.reproduciendo.set(false); this.pausada.set(false);
    };
    window.speechSynthesis.speak(lectura);
  }

  leer(texto: string, idioma = 'es') {
    if (!this.compatible() || !texto.trim()) return;
    this.detener();
    const actual = ++this.generacion;
    this.idioma = this.codigoIdioma(idioma);
    this.fragmentos = this.dividir(texto);
    this.indice = 0;
    this.reproduciendo.set(true);
    this.pausada.set(false);
    this.siguiente(actual);
  }

  pausar() {
    if (!this.compatible() || !this.reproduciendo() || this.pausada()) return;
    window.speechSynthesis.pause();
    this.pausada.set(true);
  }

  reanudar() {
    if (!this.compatible() || !this.pausada()) return;
    window.speechSynthesis.resume();
    this.pausada.set(false);
  }

  detener() {
    if (this.compatible()) window.speechSynthesis.cancel();
    this.generacion++;
    this.fragmentos = [];
    this.indice = 0;
    this.reproduciendo.set(false);
    this.pausada.set(false);
  }

  private codigoIdioma(idioma: string) {
    if (idioma === 'en' || idioma.startsWith('en-')) return 'en-US';
    if (idioma === 'pt' || idioma.startsWith('pt-')) return 'pt-BR';
    // Los navegadores habituales no incluyen voces Kichwa; se usa una voz española
    // disponible como alternativa sin afirmar que pronuncia Kichwa correctamente.
    return 'es-EC';
  }

  private obtenerVoz(idioma: string) {
    const voces = this.voces();
    return voces.find(v => v.voiceURI === this.vozSeleccionada())
      ?? voces.find(v => v.lang.toLowerCase() === idioma.toLowerCase())
      ?? voces.find(v => v.lang.toLowerCase().startsWith(idioma.slice(0, 2).toLowerCase()))
      ?? voces.find(v => v.lang.toLowerCase().startsWith('es'))
      ?? voces[0];
  }

  private siguiente(generacion: number) {
    if (generacion !== this.generacion) return;
    if (this.indice >= this.fragmentos.length) {
      this.reproduciendo.set(false);
      this.pausada.set(false);
      return;
    }
    const utterance = new SpeechSynthesisUtterance(this.fragmentos[this.indice++]);
    utterance.lang = this.idioma;
    utterance.rate = this.velocidad();
    utterance.volume = this.volumen() / 100;
    const voz = this.obtenerVoz(this.idioma);
    if (voz) {
      utterance.voice = voz;
      utterance.lang = voz.lang || this.idioma;
    }
    utterance.onend = () => this.siguiente(generacion);
    utterance.onerror = evento => {
      if (evento.error !== 'canceled' && evento.error !== 'interrupted') {
        this.reproduciendo.set(false);
        this.pausada.set(false);
      }
    };
    window.speechSynthesis.speak(utterance);
  }

  private dividir(texto: string): string[] {
    const frases = texto.match(/[^.!?。！？]+[.!?。！？]?/g) ?? [texto];
    const resultado: string[] = [];
    let actual = '';
    for (const frase of frases) {
      const parte = frase.trim();
      if (!parte) continue;
      if ((actual + ' ' + parte).trim().length <= 180) actual = (actual + ' ' + parte).trim();
      else {
        if (actual) resultado.push(actual);
        if (parte.length <= 180) actual = parte;
        else {
          for (let i = 0; i < parte.length; i += 170) resultado.push(parte.slice(i, i + 170));
          actual = '';
        }
      }
    }
    if (actual) resultado.push(actual);
    return resultado;
  }
}
