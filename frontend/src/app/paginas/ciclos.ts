import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api, Consulta, conConsulta } from '../comun/api';
import { Acceso } from '../comun/sesion';
import { camposCiclo, Formulario } from '../comun/campos';
import { formato } from '../comun/formato';
import { Aviso, Cargando, FormularioUi, Icono, Paginador } from '../comun/ui';
import { FiltrosCiclos, TablaCiclos, filtrosIniciales } from '../comun/ciclos-ui';
import { Ciclo, Cultivo, Pagina, Terreno } from '../modelos';

@Component({
  selector: 'agro-ciclos', host: { class: 'agro-pagina' },
  imports: [Aviso, Cargando, FormularioUi, Icono, Paginador, FiltrosCiclos, TablaCiclos],
  template: `
    <header class="encabezado-pagina"><div><h1>Ciclos agrícolas</h1>
      <p>Planifica la siembra y registra lo que ocurre en cada terreno.</p></div>
      <button type="button" class="boton primario" [disabled]="!puedeCrear()" (click)="crear()"><agro-icon nombre="plus" /> Nuevo ciclo</button>
    </header>
    @if (exito) { <agro-aviso [mensaje]="exito" tipo="bien" /> }
    @if (terrenos.error()) { <agro-aviso [mensaje]="terrenos.error()" [reintento]="true" (reintentar)="terrenos.cargar()" /> }
    @if (cultivos.error()) { <agro-aviso [mensaje]="cultivos.error()" [reintento]="true" (reintentar)="cultivos.cargar()" /> }
    @if (!puedeGestionar()) {
      <agro-aviso tipo="informacion" mensaje="Tu perfil operativo puede ejecutar actividades, registrar consumos y cosechas. La planificación y cierre de ciclos corresponde al supervisor o administrador." />
    } @else if (!terrenos.datos()?.length || !cultivos.datos()?.length) {
      <agro-aviso tipo="advertencia" mensaje="Registra al menos un terreno y un cultivo antes de planificar el primer ciclo." />
    }
    <section class="tarjeta">
      <agro-filtros [filtros]="borrador()" (filtrosChange)="borrador.set($event)" [terrenos]="terrenos.datos() ?? []"
        [cultivos]="cultivos.datos() ?? []" (aplicar)="aplicar()" (limpiar)="limpiar()" />
      @if (ciclos.cargando()) { <agro-cargando /> }
      @else if (ciclos.error()) { <agro-aviso [mensaje]="ciclos.error()" [reintento]="true" (reintentar)="ciclos.cargar()" /> }
      @else if (ciclos.datos(); as pagina) {
        <agro-tabla-ciclos [ciclos]="pagina.datos" /><agro-paginador [datos]="pagina" (cambiar)="listar($event)" />
      }
    </section>
    @if (modal(); as config) { <agro-formulario [config]="config" (cerrar)="modal.set(null)" /> }
    `
})
/**
 * CiclosPagina: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
export class CiclosPagina implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly acceso = inject(Acceso);
  readonly terrenos = new Consulta<Terreno[]>(this.api);
  readonly cultivos = new Consulta<Cultivo[]>(this.api);
  readonly ciclos = new Consulta<Pagina<Ciclo>>(this.api);
  readonly borrador = signal({ ...filtrosIniciales });
  readonly modal = signal<Formulario | null>(null);
  readonly puedeGestionar = computed(() => ['ADMIN','SUPERVISOR'].includes(this.acceso.usuario()?.rol ?? ''));
  readonly puedeCrear = computed(() => this.puedeGestionar() && Boolean(this.terrenos.datos()?.length && this.cultivos.datos()?.length));
  readonly exito: string | undefined = history.state?.exito;
  private filtros = { ...filtrosIniciales };
  ngOnInit() {
    void this.terrenos.cargar('/terrenos');
    void this.cultivos.cargar('/cultivos');
    this.listar(1);
  }
  listar(pagina: number) { void this.ciclos.cargar(conConsulta('/ciclos', { ...this.filtros, pagina, tam: 6 })); }
  aplicar() { this.filtros = this.borrador(); this.listar(1); }
  limpiar() { this.borrador.set({ ...filtrosIniciales }); this.aplicar(); }
  crear() {
    this.modal.set({
      titulo: 'Planificar ciclo', accion: 'Crear ciclo', inicial: { fecha_inicio: formato.hoy(), notas: '' },
      campos: (valores) => camposCiclo(this.terrenos.datos() ?? [], this.cultivos.datos() ?? [], valores),
      enviar: async (datos) => {
        const creado = await this.api.pedir<{ id: number }>('/ciclos', 'POST', datos);
        this.modal.set(null);
        await this.router.navigate(['/ciclos', creado.id], { state: { exito: 'Ciclo planificado.' } });
      }
    });
  }
}
