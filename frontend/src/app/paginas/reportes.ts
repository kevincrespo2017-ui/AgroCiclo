import { Component, inject, signal } from '@angular/core';
import { Api, mensajeError } from '../comun/api';
import { Aviso, Icono } from '../comun/ui';

/**
 * ReportesPagina: centro de descarga de documentos PDF consolidados.
 * Cada botón llama al endpoint real del Backend; no utiliza datos de demostración.
 */
@Component({
  selector: 'agro-reportes',
  host: { class: 'agro-pagina' },
  imports: [Aviso, Icono],
  template: `
    <header class="encabezado-pagina">
      <div>
        <span class="sobre-titulo">INFORMACIÓN PARA DECIDIR</span>
        <h1>Centro de reportes PDF</h1>
        <p>Genera documentos descargables con la información registrada en AgroCiclo.</p>
      </div>
    </header>
    @if (error()) { <agro-aviso [mensaje]="error()"/> }
    <section class="reportes-portada">
      <div><span>AGROCICLO · INFORMES</span><h2>La operación agrícola, en documentos claros.</h2>
        <p>Selecciona el informe que necesitas. Cada archivo se genera con los datos actuales de la base de datos.</p></div>
      <agro-icon nombre="file-down" [tam]="54"/>
    </section>
    <section class="reportes-pdf-grid reportes-pdf-grid-amplia">
      @for (reporte of reportes; track reporte.tipo) {
        <article class="reporte-pdf-card">
          <span class="reporte-pdf-icono"><agro-icon [nombre]="reporte.icono" [tam]="25"/></span>
          <div class="reporte-pdf-datos"><strong>{{reporte.titulo}}</strong><p>{{reporte.detalle}}</p></div>
          <button type="button" class="boton primario" [disabled]="activo()!==''"
            (click)="descargar(reporte.tipo)">
            <agro-icon [nombre]="activo()===reporte.tipo?'loader-circle':'download'" [tam]="16"/>
            {{activo()===reporte.tipo?'Generando…':'Descargar PDF'}}
          </button>
        </article>
      }
    </section>
    <p class="reportes-nota"><agro-icon nombre="shield-check" [tam]="15"/> Los informes se generan desde la información persistida. Si no existen registros para una categoría, el PDF indicará que no hay datos disponibles.</p>
  `
})
export class ReportesPagina {
  private readonly api = inject(Api);
  readonly activo = signal('');
  readonly error = signal('');
  readonly reportes = [
    { tipo: 'produccion', titulo: 'Producción agrícola', detalle: 'Ciclos, cultivos, cosechas, rendimiento y costos consolidados.', icono: 'wheat' },
    { tipo: 'terrenos', titulo: 'Terrenos agrícolas', detalle: 'Listado de parcelas, ubicación y superficie registrada.', icono: 'map' },
    { tipo: 'insumos', titulo: 'Inventario de insumos', detalle: 'Existencias, unidades y niveles mínimos de reposición.', icono: 'package' },
    { tipo: 'actividades', titulo: 'Actividades agrícolas', detalle: 'Programación, cumplimiento y costos de las actividades.', icono: 'clipboard-list' }
  ];

  /** Solicita al Backend el PDF del tipo seleccionado y fuerza su descarga en el navegador. */
  async descargar(tipo: string) {
    if (this.activo()) return;
    this.error.set('');
    this.activo.set(tipo);
    try { await this.api.descargarReporte(tipo); }
    catch (error) { this.error.set(mensajeError(error)); }
    finally { this.activo.set(''); }
  }
}
