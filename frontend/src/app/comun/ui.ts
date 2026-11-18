import { AfterViewInit, Component, ElementRef, EventEmitter, Input, Output, ViewChild, signal } from '@angular/core';
import { ApiError, mensajeError } from './api';
import { Campo, Formulario, ValorCampo, Valores } from './campos';

@Component({
  selector: 'agro-icon',
  template: '<span class="agro-icono" aria-hidden="true" [style.width.px]="tam" [style.height.px]="tam" [style.mask-image]="url"></span>'
})
/**
 * Icono: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Icono {
  @Input({ required: true }) nombre!: string;
  @Input() tam = 18;
  get url() { return 'url(/iconos/' + this.nombre + '.svg)'; }
}

@Component({
  selector: 'agro-marca', imports: [Icono],
  template: '<span class="marca"><span class="marca-icono"><agro-icon nombre="sprout" [tam]="20" /></span><span>AgroCiclo</span></span>'
})
/**
 * Marca: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Marca {}

@Component({
  selector: 'agro-aviso', imports: [Icono],
  template: `@if (mensaje) {
    <div [class]="'aviso ' + tipo" [attr.role]="tipo === 'bien' || tipo === 'informacion' ? 'status' : 'alert'">
      <agro-icon [nombre]="tipo === 'bien' ? 'circle-check' : 'alert-circle'" [tam]="19" />
      <span>{{ mensaje }}</span>
      @if (reintento) { <button type="button" class="boton secundario pequeno" (click)="reintentar.emit()">Reintentar</button> }
    </div>
  }`
})
/**
 * Aviso: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Aviso {
  @Input() mensaje = '';
  @Input() tipo: 'bien' | 'advertencia' | 'error' | 'informacion' = 'error';
  @Input() reintento = false;
  @Output() reintentar = new EventEmitter<void>();
}

@Component({
  selector: 'agro-cargando', imports: [Icono],
  template: '<div class="estado-carga" role="status"><agro-icon nombre="loader-circle" class="gira" [tam]="19" />{{ texto }}</div>'
})
/**
 * Cargando: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Cargando { @Input() texto = 'Cargando datos…'; }

@Component({
  selector: 'agro-vacio', imports: [Icono],
  template: `<div class="vacio"><agro-icon nombre="inbox" [tam]="25" />
    <strong>{{ titulo }}</strong>@if (texto) { <span>{{ texto }}</span> }</div>`
})
/**
 * Vacio: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Vacio {
  @Input({ required: true }) titulo!: string;
  @Input() texto = '';
}

@Component({
  selector: 'agro-estado',
  template: '<span [class]="\'estado \' + tono"><span aria-hidden="true"></span><ng-content /></span>'
})
/**
 * Estado: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Estado { @Input({ required: true }) tono!: string; }

@Component({
  selector: 'agro-tabla',
  template: `<div class="tabla-scroll" tabindex="0" role="region" [attr.aria-label]="etiqueta">
    <table>
      <thead><tr>@for (columna of columnas; track columna) { <th scope="col">{{ columna }}</th> }</tr></thead>
      <tbody><ng-content /></tbody>
    </table>
  </div>`
})
/**
 * Tabla: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Tabla {
  @Input({ required: true }) columnas!: string[];
  @Input({ required: true }) etiqueta!: string;
}

@Component({
  selector: 'agro-paginador', imports: [Icono],
  template: `<div class="paginador">
    <span>{{ rango }}</span>
    <div class="acciones">
      <button type="button" class="icono-boton" aria-label="Página anterior" [disabled]="datos.pagina <= 1"
        (click)="cambiar.emit(datos.pagina - 1)"><agro-icon nombre="chevron-left" [tam]="17" /></button>
      <span class="paginador-numero">Página {{ datos.pagina }} / {{ paginas }}</span>
      <button type="button" class="icono-boton" aria-label="Página siguiente" [disabled]="datos.pagina >= paginas"
        (click)="cambiar.emit(datos.pagina + 1)"><agro-icon nombre="chevron-right" [tam]="17" /></button>
    </div>
  </div>`
})
/**
 * Paginador: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Paginador {
  @Input({ required: true }) datos!: { pagina: number; tam: number; total: number };
  @Output() cambiar = new EventEmitter<number>();
  get paginas() { return Math.max(1, Math.ceil(this.datos.total / this.datos.tam)); }
  get rango() {
    const { pagina, tam, total } = this.datos;
    const inicio = (pagina - 1) * tam;
    return total === 0 || inicio >= total ? total + ' registros' : (inicio + 1) + '–' + Math.min(inicio + tam, total) + ' de ' + total;
  }
}

let siguienteId = 0;

@Component({
  selector: 'agro-campo', host: { '[class.ancho]': 'campo.tipo === "textarea" || campo.tipo === "checkbox"' },
  template: `<div [class]="campo.tipo === 'checkbox' ? 'campo campo-check' : 'campo'">
    @if (campo.tipo === 'checkbox') {
      <label [attr.for]="id">
        <input [id]="id" type="checkbox" [checked]="valor === true" (change)="leer($event)"
          [attr.aria-describedby]="ayuda ? id + '-ayuda' : null"> {{ campo.etiqueta }}
      </label>
    } @else {
      <label [attr.for]="id">{{ campo.etiqueta }}</label>
      @if (campo.opciones; as opciones) {
        <select [id]="id" [value]="valor ?? ''" [required]="campo.required ?? false"
          [attr.aria-invalid]="error ? 'true' : null" [attr.aria-describedby]="ayuda ? id + '-ayuda' : null" (change)="leer($event)">
          @for (opcion of opciones; track opcion.value) { <option [value]="opcion.value" [selected]="esSeleccion(opcion.value)">{{ opcion.label }}</option> }
        </select>
      } @else if (campo.tipo === 'textarea') {
        <textarea [id]="id" [value]="valor ?? ''" rows="3" [attr.maxlength]="campo.maxLength"
          [attr.aria-invalid]="error ? 'true' : null" [attr.aria-describedby]="ayuda ? id + '-ayuda' : null" (input)="leer($event)"></textarea>
      } @else {
        <input [id]="id" [type]="campo.tipo || 'text'" [value]="valor ?? ''"
          [required]="campo.required ?? false" [attr.min]="campo.min" [attr.max]="campo.max"
          [attr.step]="campo.step" [attr.maxlength]="campo.maxLength" [attr.minlength]="campo.minLength"
          [attr.placeholder]="campo.placeholder" [attr.aria-invalid]="error ? 'true' : null"
          [attr.aria-describedby]="ayuda ? id + '-ayuda' : null" (input)="leer($event)">
      }
    }
    @if (ayuda) { <small [id]="id + '-ayuda'" [class.campo-error]="error">{{ ayuda }}</small> }
  </div>`
})
/**
 * CampoUi: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class CampoUi {
  @Input({ required: true }) campo!: Campo;
  @Input() valor: ValorCampo;
  @Input() error?: string;
  @Output() valorChange = new EventEmitter<ValorCampo>();
  readonly id = 'campo-' + ++siguienteId;
  get ayuda() { return this.error || this.campo.ayuda; }
  esSeleccion(valor: string | number) { return String(this.valor ?? '') === String(valor); }
  leer(evento: Event) {
    const control = evento.target as HTMLInputElement;
    this.valorChange.emit(this.campo.tipo === 'checkbox' ? control.checked : control.value);
  }
}

@Component({
  selector: 'agro-dialogo', imports: [Icono],
  template: `<dialog #dialogo class="dialogo" [attr.aria-labelledby]="id" (cancel)="cancelar($event)">
    <div class="dialogo-cabecera">
      <div><h2 [id]="id">{{ titulo }}</h2>@if (descripcion) { <p>{{ descripcion }}</p> }</div>
      <button class="icono-boton" type="button" aria-label="Cerrar ventana" [disabled]="bloqueado"
        (click)="cerrar.emit()"><agro-icon nombre="x" [tam]="17" /></button>
    </div>
    <ng-content />
  </dialog>`
})
/**
 * Dialogo: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class Dialogo implements AfterViewInit {
  @Input({ required: true }) titulo!: string;
  @Input() descripcion?: string;
  @Input() bloqueado = false;
  @Output() cerrar = new EventEmitter<void>();
  @ViewChild('dialogo', { static: true }) dialogo!: ElementRef<HTMLDialogElement>;
  readonly id = 'dialogo-' + ++siguienteId;
  ngAfterViewInit() { this.dialogo.nativeElement.showModal(); }
  cancelar(evento: Event) {
    evento.preventDefault();
    if (!this.bloqueado) this.cerrar.emit();
  }
}

@Component({
  selector: 'agro-formulario', imports: [Dialogo, Aviso, CampoUi, Icono],
  template: `<agro-dialogo [titulo]="config.titulo" [descripcion]="config.descripcion" [bloqueado]="guardando()" (cerrar)="cerrar.emit()">
    <form (submit)="guardar($event)">
      @if (error()) { <agro-aviso [mensaje]="error()" /> }
      @if (campos.length) {
        <div class="formulario-grilla">
          @for (campo of campos; track campo.nombre) {
            <agro-campo [campo]="campo" [valor]="valores[campo.nombre]" [error]="erroresCampo()[campo.nombre]"
              (valorChange)="valores[campo.nombre] = $event" />
          }
        </div>
      }
      <div class="dialogo-pie">
        <button type="button" class="boton secundario" [disabled]="guardando()" (click)="cerrar.emit()">Cancelar</button>
        <button [class]="'boton ' + (config.peligro ? 'peligro' : 'primario')" [disabled]="guardando()">
          @if (guardando()) { <agro-icon nombre="loader-circle" class="gira" [tam]="16" /> Guardando… }
          @else { {{ config.accion || 'Guardar' }} }
        </button>
      </div>
    </form>
  </agro-dialogo>`
})
/**
 * FormularioUi: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class FormularioUi {
  private actual!: Formulario;
  @Input({ required: true }) set config(formulario: Formulario) {
    this.actual = formulario;
    this.valores = { ...formulario.inicial };
  }
  get config() { return this.actual; }
  @Output() cerrar = new EventEmitter<void>();
  valores: Valores = {};

  readonly guardando = signal(false);
  readonly error = signal('');
  readonly erroresCampo = signal<Record<string, string>>({});

  get campos() {
    const campos = this.config.campos;
    return typeof campos === 'function' ? campos(this.valores) : campos ?? [];
  }

  async guardar(evento: Event) {
    evento.preventDefault();
    if (this.guardando() || !(evento.target as HTMLFormElement).reportValidity()) return;
    this.guardando.set(true);
    this.error.set('');
    this.erroresCampo.set({});
    try {
      const valores: Valores = {};
      for (const campo of this.campos) {
        const valor = this.valores[campo.nombre];
        valores[campo.nombre] = campo.numerico || campo.tipo === 'number'
          ? valor === '' || valor == null ? null : Number(valor)
          : valor;
      }
      await this.config.enviar(valores);
    } catch (error) {
      this.error.set(mensajeError(error));
      if (error instanceof ApiError && error.campos) this.erroresCampo.set(error.campos);
    } finally {
      this.guardando.set(false);
    }
  }
}
