import { Component, OnInit, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { Api, Consulta, conConsulta } from '../comun/api';
import { formato } from '../comun/formato';
import { Aviso, CampoUi, Cargando, Dialogo, Icono, Paginador, Tabla, Vacio } from '../comun/ui';
import { Campo, ValorCampo } from '../comun/campos';
import { Auditoria, Pagina } from '../modelos';

@Component({
  selector: 'agro-auditoria', host: { class: 'agro-pagina' },
  imports: [Aviso, CampoUi, Cargando, Dialogo, Icono, Paginador, Tabla, Vacio, JsonPipe],
  template: `
  <header class="encabezado-pagina"><div><h1>Auditoría</h1><p>Consulta quién realizó cada cambio y cuándo.</p></div></header>
  <section class="tarjeta">
    <form class="filtros" (submit)="aplicar($event)">
      <div class="filtros-grilla auditoria-filtros">
        <agro-campo [campo]="buscar" [valor]="borrador.q" (valorChange)="cambio('q', $event)" />
        <agro-campo [campo]="desde" [valor]="borrador.desde" (valorChange)="cambio('desde', $event)" />
      </div>
      <button class="boton primario"><agro-icon nombre="filter" [tam]="16" /> Aplicar filtros</button>
    </form>
    @if (recurso.cargando()) { <agro-cargando /> }
    @else if (recurso.error()) { <agro-aviso [mensaje]="recurso.error()" [reintento]="true" (reintentar)="recurso.cargar()" /> }
    @else if (recurso.datos(); as pagina) {
      @if (pagina.datos.length) {
        <agro-tabla etiqueta="Operaciones auditadas" [columnas]="['Fecha y hora','Usuario','Acción','Entidad','Registro','Detalle']">
          @for (item of pagina.datos; track item.id) {
            <tr><td>{{ f.hora(item.fecha_hora) }}</td><th scope="row">{{ item.usuario }}<small>{{ item.email }}</small></th>
              <td class="capitalizar">{{ nombre(item.accion) }}</td><td>{{ nombre(item.entidad) }}</td><td>#{{ item.registro_id }}</td>
              <td><button type="button" class="boton secundario pequeno" [attr.aria-label]="'Ver detalle de la operación ' + item.id"
                (click)="registro.set(item)">Ver</button></td></tr>
          }
        </agro-tabla>
      } @else { <agro-vacio titulo="No hay operaciones con esos criterios" /> }
      <agro-paginador [datos]="pagina" (cambiar)="listar($event)" />
    }
  </section>
  @if (registro(); as item) {
    <agro-dialogo [titulo]="'Detalle de la operación #' + item.id" [descripcion]="f.hora(item.fecha_hora) + ' · ' + item.usuario"
      (cerrar)="registro.set(null)"><pre class="detalle-auditoria">{{ item.detalle | json }}</pre></agro-dialogo>
  }`
})
/**
 * AuditoriaPagina: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
export class AuditoriaPagina implements OnInit {
  private readonly api = inject(Api);
  readonly recurso = new Consulta<Pagina<Auditoria>>(this.api);
  readonly registro = signal<Auditoria | null>(null);
  readonly f = formato;
  readonly buscar: Campo = { nombre: 'q', etiqueta: 'Buscar', tipo: 'search', placeholder: 'Usuario, acción o entidad', maxLength: 100 };
  readonly desde: Campo = { nombre: 'desde', etiqueta: 'Fecha desde', tipo: 'date' };
  borrador = { q: '', desde: '' };
  private filtros = { q: '', desde: '' };
  ngOnInit() { this.listar(1); }
  nombre(valor: string) { return valor.toLowerCase().replaceAll('_', ' '); }
  cambio(campo: 'q' | 'desde', valor: ValorCampo) { this.borrador[campo] = String(valor ?? ''); }
  listar(pagina: number) { void this.recurso.cargar(conConsulta('/auditoria', { ...this.filtros, pagina })); }
  aplicar(evento: Event) { evento.preventDefault(); this.filtros = { ...this.borrador }; this.listar(1); }
}
