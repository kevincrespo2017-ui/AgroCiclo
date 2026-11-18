import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api, Consulta, conConsulta, mensajeError } from '../comun/api';
import { FiltrosCiclos, TablaCiclos, filtrosIniciales } from '../comun/ciclos-ui';
import { formato } from '../comun/formato';
import { Aviso, Cargando, Icono, Paginador, Tabla, Vacio } from '../comun/ui';
import { Ciclo, Cultivo, Pagina, Terreno } from '../modelos';

@Component({
  selector: 'agro-productividad', host: { class: 'agro-pagina' },
  imports: [RouterLink, Aviso, Cargando, Icono, Paginador, Tabla, Vacio, FiltrosCiclos, TablaCiclos],
  templateUrl: './productividad.html'
})
/**
 * Productividad: compara ciclos agrícolas y permite descargar la ficha PDF individual de cada ciclo.
 */
export class Productividad implements OnInit {
  private readonly api = inject(Api);
  readonly terrenos = new Consulta<Terreno[]>(this.api);
  readonly cultivos = new Consulta<Cultivo[]>(this.api);
  readonly reporte = new Consulta<Pagina<Ciclo>>(this.api);
  readonly borrador = signal({ ...filtrosIniciales });
  readonly seleccion = signal<Ciclo[]>([]);
  readonly ids = computed(() => this.seleccion().map(ciclo => ciclo.id));
  readonly referencia = computed(() => this.seleccion()[0]);
  readonly comparado = computed(() => this.seleccion()[1] as Ciclo | undefined);
  readonly error = signal('');
  readonly pdfActivo = signal(false);
  readonly f = formato;
  private filtros = { ...filtrosIniciales };
  private readonly porcentaje = new Intl.NumberFormat('es-EC', {
    style: 'percent', maximumFractionDigits: 1, signDisplay: 'exceptZero'
  });
  readonly variacion = computed(() => {
    const a = Number(this.referencia()?.rendimiento_kg_ha);
    const b = this.comparado()?.rendimiento_kg_ha;
    return a > 0 && b != null ? this.porcentaje.format((Number(b) - a) / a) : '';
  });
  readonly metricas = [
    { texto: 'Área', valor: (ciclo: Ciclo) => formato.numero(ciclo.area_ha) + ' ha' },
    { texto: 'Producción', valor: (ciclo: Ciclo) => formato.numero(ciclo.kg_producidos) + ' kg' },
    { texto: 'Rendimiento', valor: (ciclo: Ciclo) => formato.rendimiento(ciclo.rendimiento_kg_ha) },
    { texto: 'Costo total', valor: (ciclo: Ciclo) => formato.moneda(ciclo.costo_total) }
  ];

  ngOnInit() {
    void this.terrenos.cargar('/terrenos');
    void this.cultivos.cargar('/cultivos');
    this.listar(1);
  }
  listar(pagina: number) {
    void this.reporte.cargar(conConsulta('/reportes/ciclos', { ...this.filtros, pagina, tam: 8 }));
  }
  aplicar() { this.filtros = this.borrador(); this.listar(1); }
  limpiar() { this.borrador.set({ ...filtrosIniciales }); this.aplicar(); }
  elegir(ciclo: Ciclo) {
    this.seleccion.update(lista => lista.some(item => item.id === ciclo.id)
      ? lista.filter(item => item.id !== ciclo.id) : [...lista, ciclo].slice(0, 2));
  }

  async descargar(ciclo: Ciclo) {
    this.error.set('');
    this.pdfActivo.set(true);
    try { await this.api.descargarFicha(ciclo.id, ciclo.codigo); }
    catch (error) { this.error.set(mensajeError(error)); }
    finally { this.pdfActivo.set(false); }
  }
}
