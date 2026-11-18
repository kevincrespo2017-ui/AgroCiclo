import { Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Alerta, Ciclo, Indicadores, Pagina } from '../modelos';
import { Api, Consulta, conConsulta } from '../comun/api';
import { formato } from '../comun/formato';
import { Aviso, Cargando, Estado, Icono, Vacio } from '../comun/ui';
import { TablaCiclos } from '../comun/ciclos-ui';

@Component({
  selector: 'agro-panel', host: { class: 'agro-pagina' },
  imports: [RouterLink, Aviso, Cargando, Estado, Icono, Vacio, TablaCiclos],
  templateUrl: './panel.html'
})
/**
 * Panel: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
export class Panel implements OnInit {
  private readonly api = inject(Api);
  readonly f = formato;
  readonly indicadores = new Consulta<Indicadores>(this.api);
  readonly alertas = new Consulta<Alerta[]>(this.api);
  readonly recientes = new Consulta<Pagina<Ciclo>>(this.api);
  readonly niveles = { VENCIDA: 'Vencida', PROXIMA: 'Próxima', BAJO_STOCK: 'Stock bajo' };
  readonly productivos = computed(() => this.indicadores.datos()?.cultivos.filter(c => Number(c.produccion_kg) > 0) ?? []);
  readonly mayor = computed(() => Math.max(1, ...this.productivos().map(c => Number(c.produccion_kg))));
  readonly metricas = computed(() => {
    const datos = this.indicadores.datos()?.resumen;
    if (!datos) return [];
    return [
      { titulo: 'Ciclos sembrados', icono: 'sprout', tono: 'musgo', valor: datos.sembrados, nota: datos.ciclos + ' ciclos en total' },
      { titulo: 'Producción registrada', icono: 'wheat', tono: 'trigo', valor: this.f.numero(datos.produccion_kg) + ' kg', nota: '' },
      { titulo: 'Rendimiento ponderado', icono: 'leaf', tono: 'menta', valor: this.f.rendimiento(datos.rendimiento_kg_ha), nota: 'Área de ciclos con cosecha' },
      { titulo: 'Costo de producción', icono: 'coins', tono: 'avena', valor: this.f.moneda(datos.costo_usd), nota: 'Actividades e insumos aplicados' }
    ];
  });
  ngOnInit() {
    void this.indicadores.cargar('/reportes/indicadores');
    void this.alertas.cargar('/alertas');
    void this.recientes.cargar(conConsulta('/ciclos', { tam: 4 }));
  }
  ancho(valor: string | number) { return Number(valor) * 100 / this.mayor(); }
}
