import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Ciclo, Cultivo, EstadoCiclo, FiltrosCiclo, Terreno } from '../modelos';
import { formato } from './formato';
import { Campo, ValorCampo, opcionesCatalogo, opcionesDe } from './campos';
import { CampoUi, Estado, Icono, Tabla, Vacio } from './ui';

export const filtrosIniciales: FiltrosCiclo = { ordenar: 'fechaInicio', direccion: 'desc' };

@Component({
  selector: 'agro-estado-ciclo',
  imports: [Estado],
  template: '<agro-estado [tono]="estado.toLowerCase()">{{ nombres[estado] }}</agro-estado>'
})
/**
 * EstadoCicloUi: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class EstadoCicloUi {
  @Input({ required: true }) estado!: EstadoCiclo;
  readonly nombres = formato.estados;
}

@Component({
  selector: 'agro-tabla-ciclos', imports: [Tabla, EstadoCicloUi, Icono, RouterLink, Vacio],
  template: `@if (ciclos.length) {
    <agro-tabla [columnas]="columnas" etiqueta="Ciclos agrícolas">
      @for (ciclo of ciclos; track ciclo.id) {
        <tr>
          @if (comparar) {
            <td><input type="checkbox" [attr.aria-label]="'Comparar ' + ciclo.codigo"
              [checked]="seleccion.includes(ciclo.id)" [disabled]="seleccion.length >= 2 && !seleccion.includes(ciclo.id)"
              (change)="alternar.emit(ciclo)"></td>
          }
          <th scope="row"><a class="enlace-tabla" [routerLink]="['/ciclos', ciclo.id]">{{ ciclo.codigo }}
            <agro-icon nombre="arrow-up-right" [tam]="13" /></a></th>
          <td><strong>{{ ciclo.cultivo }}</strong><small>{{ ciclo.terreno }}</small></td>
          <td><agro-estado-ciclo [estado]="ciclo.estado" /></td>
          <td>{{ f.fecha(ciclo.fecha_inicio) }}</td><td class="valor">{{ f.numero(ciclo.kg_producidos) }} kg</td>
          <td class="valor">{{ f.rendimiento(ciclo.rendimiento_kg_ha) }}</td><td class="valor">{{ f.moneda(ciclo.costo_total) }}</td>
          @if (ficha) {
            <td><button class="icono-boton" type="button" title="Descargar ficha PDF"
              [attr.aria-label]="'Descargar ficha PDF de ' + ciclo.codigo" [disabled]="pdfActivo"
              (click)="descargar.emit(ciclo)"><agro-icon nombre="file-down" /></button></td>
          }
        </tr>
      }
    </agro-tabla>
    } @else { <agro-vacio titulo="No hay ciclos para mostrar" texto="Ajusta los filtros o planifica un ciclo." /> }`
})
/**
 * TablaCiclos: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class TablaCiclos {
  @Input({ required: true }) ciclos!: Ciclo[];
  @Input() comparar = false;
  @Input() ficha = false;
  @Input() pdfActivo = false;
  @Input() seleccion: number[] = [];
  @Output() alternar = new EventEmitter<Ciclo>();
  @Output() descargar = new EventEmitter<Ciclo>();
  readonly f = formato;
  get columnas() {
    return [...(this.comparar ? ['Comparar'] : []), 'Ciclo', 'Cultivo y terreno', 'Estado', 'Inicio',
      'Producción', 'Rendimiento', 'Costo', ...(this.ficha ? ['Ficha'] : [])];
  }
}

@Component({
  selector: 'agro-filtros', imports: [CampoUi, Icono],
  template: `<form class="filtros" (submit)="enviar($event)">
    <div class="filtros-grilla">
      @for (campo of campos; track campo.nombre) {
        <agro-campo [campo]="campo" [valor]="valor(campo.nombre)" (valorChange)="modificar(campo.nombre, $event)" />
      }
    </div>
    <div class="acciones">
      <button class="boton primario"><agro-icon nombre="filter" [tam]="15" /> Aplicar filtros</button>
      <button class="boton secundario" type="button" (click)="limpiar.emit()">Limpiar</button>
    </div>
  </form>`
})
/**
 * FiltrosCiclos: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export class FiltrosCiclos {
  @Input({ required: true }) filtros!: FiltrosCiclo;
  @Input() terrenos: Terreno[] = [];
  @Input() cultivos: Cultivo[] = [];
  @Output() filtrosChange = new EventEmitter<FiltrosCiclo>();
  @Output() aplicar = new EventEmitter<void>();
  @Output() limpiar = new EventEmitter<void>();

  get campos(): Campo[] {
    const todos = { value: '', label: 'Todos' };
    return [
      { nombre: 'q', etiqueta: 'Buscar', tipo: 'search', placeholder: 'Código, cultivo o terreno', maxLength: 100 },
      { nombre: 'estado', etiqueta: 'Estado', opciones: [todos, ...opcionesDe(formato.estados)] },
      { nombre: 'cultivoId', etiqueta: 'Cultivo', opciones: [todos, ...opcionesCatalogo(this.cultivos)] },
      { nombre: 'terrenoId', etiqueta: 'Terreno', opciones: [todos, ...opcionesCatalogo(this.terrenos)] },
      { nombre: 'desde', etiqueta: 'Inicio desde', tipo: 'date' },
      { nombre: 'hasta', etiqueta: 'Inicio hasta', tipo: 'date', min: this.filtros.desde || undefined },
      { nombre: 'ordenar', etiqueta: 'Ordenar por', opciones: opcionesDe({
        fechaInicio: 'Fecha de inicio', codigo: 'Código', rendimiento: 'Rendimiento', costo: 'Costo total'
      }) },
      { nombre: 'direccion', etiqueta: 'Dirección', opciones: opcionesDe({ desc: 'Descendente', asc: 'Ascendente' }) }
    ];
  }
  valor(nombre: string) { return this.filtros[nombre as keyof FiltrosCiclo]; }
  modificar(nombre: string, valor: ValorCampo) {
    this.filtrosChange.emit({ ...this.filtros, [nombre]: valor == null ? '' : String(valor) });
  }
  enviar(evento: Event) { evento.preventDefault(); this.aplicar.emit(); }
}
