import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Actividad, Cosecha, Cultivo, DetalleCiclo, Insumo, Terreno } from '../modelos';
import { Api, Consulta, mensajeError } from '../comun/api';
import { Acceso } from '../comun/sesion';
import { formato } from '../comun/formato';
import { Formulario, camposCiclo, camposMovimiento, cantidad, fechaCampo, folioCampo, notas, selector, texto } from '../comun/campos';
import { Aviso, Cargando, Estado, FormularioUi, Icono, Tabla, Vacio } from '../comun/ui';
import { EstadoCicloUi } from '../comun/ciclos-ui';

type Vista = 'actividades' | 'insumos' | 'cosechas';

@Component({
  selector: 'agro-ciclo-detalle', host: { class: 'agro-pagina' },
  imports: [RouterLink, Aviso, Cargando, Estado, FormularioUi, Icono, Tabla, Vacio, EstadoCicloUi],
  templateUrl: './ciclo-detalle.html'
})
/**
 * CicloDetalle: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
export class CicloDetalle {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly acceso = inject(Acceso);
  readonly admin = computed(() => this.acceso.usuario()?.rol === 'ADMIN');
  readonly gestor = computed(() => ['ADMIN','SUPERVISOR'].includes(this.acceso.usuario()?.rol ?? ''));
  readonly f = formato;
  readonly ciclo = new Consulta<DetalleCiclo>(this.api);
  readonly stock = new Consulta<Insumo[]>(this.api);
  readonly insumos = computed(() => (this.stock.datos() ?? []).filter(i => Number(i.stock) > 0));
  readonly modal = signal<Formulario | null>(null);
  readonly exito = signal<string>(history.state?.exito ?? '');
  readonly error = signal('');
  readonly pdfActivo = signal(false);
  readonly vista = signal<Vista>('actividades');
  readonly pestanas: { clave: Vista; texto: string }[] = [
    { clave: 'actividades', texto: 'Actividades' },
    { clave: 'insumos', texto: 'Insumos aplicados' },
    { clave: 'cosechas', texto: 'Cosechas' }
  ];
  private id = 0;

  constructor() {
    this.api.sincronizacionCompletada.pipe(takeUntilDestroyed()).subscribe(cantidad => {
      this.exito.set(`${cantidad} operación(es) de campo sincronizada(s) correctamente.`);
      void this.ciclo.cargar();
      void this.stock.cargar('/insumos');
    });
    this.ruta.paramMap.pipe(takeUntilDestroyed()).subscribe(parametros => {
      this.id = Number(parametros.get('id'));
      this.modal.set(null);
      this.error.set('');
      void this.ciclo.cargar('/ciclos/' + this.id);
    });
    this.ruta.queryParamMap.pipe(takeUntilDestroyed()).subscribe(parametros => {
      const vista = parametros.get('vista');
      this.vista.set(vista === 'insumos' || vista === 'cosechas' ? vista : 'actividades');
      if (vista === 'insumos') void this.stock.cargar('/insumos');
    });
  }

  abrirVista(vista: Vista) {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { vista }, replaceUrl: true });
  }

  async editar(ciclo: DetalleCiclo) {
    this.error.set('');
    try {
      const [terrenos, cultivos] = await Promise.all([this.api.pedir<Terreno[]>('/terrenos'), this.api.pedir<Cultivo[]>('/cultivos')]);
      this.abrir({
        titulo: 'Editar ciclo',
        inicial: { codigo: ciclo.codigo, terreno_id: ciclo.terreno_id, cultivo_id: ciclo.cultivo_id,
          area_ha: ciclo.area_ha, fecha_inicio: ciclo.fecha_inicio, notas: ciclo.notas },
        campos: valores => camposCiclo(terrenos, cultivos, valores)
      }, '', 'PUT', 'Ciclo actualizado.');
    } catch (error) {
      this.error.set(mensajeError(error));
    }
  }

  sembrar(ciclo: DetalleCiclo) {
    this.abrir({
      titulo: 'Registrar siembra', accion: 'Confirmar siembra', inicial: { fecha: formato.hoy() },
      campos: [fechaCampo('fecha', 'Fecha de siembra', ciclo.fecha_inicio, formato.hoy())]
    }, '/sembrar', 'POST', 'Siembra registrada.');
  }

  cerrar(ciclo: DetalleCiclo) {
    this.abrir({
      titulo: 'Cerrar ciclo', descripcion: 'El cierre conserva el historial y bloquea nuevos movimientos.',
      accion: 'Cerrar ciclo', inicial: { fecha: formato.hoy() },
      campos: [fechaCampo('fecha', 'Fecha de cierre', ciclo.fecha_siembra, formato.hoy())]
    }, '/cerrar', 'POST', 'Ciclo cerrado.');
  }

  actividad(ciclo: DetalleCiclo, registro?: Actividad) {
    this.abrir({
      titulo: registro ? 'Editar actividad' : 'Programar actividad',
      inicial: registro ? { nombre: registro.nombre, programada_para: registro.programada_para,
        obligatoria: registro.obligatoria, nota: registro.nota } : { obligatoria: false, programada_para: formato.hoy(), nota: '' },
      campos: [texto('nombre', 'Actividad'), fechaCampo('programada_para', 'Fecha programada', ciclo.fecha_inicio),
        { nombre: 'obligatoria', etiqueta: 'Obligatoria para cerrar el ciclo', tipo: 'checkbox' }, notas()]
    }, '/actividades' + (registro ? '/' + registro.id : ''), registro ? 'PUT' : 'POST',
      registro ? 'Actividad actualizada.' : 'Actividad programada.');
  }

  completar(ciclo: DetalleCiclo, registro: Actividad) {
    this.abrir({
      titulo: 'Completar actividad', descripcion: registro.nombre, inicial: { fecha: formato.hoy(), costo: 0 },
      campos: [fechaCampo('fecha', 'Fecha de ejecución', ciclo.fecha_inicio, formato.hoy()),
        cantidad('costo', 'Costo (USD)', 0, 1_000_000, 0.01)]
    }, '/actividades/' + registro.id + '/completar', 'POST', 'Actividad completada.');
  }

  consumir(ciclo: DetalleCiclo) {
    this.abrir({
      titulo: 'Aplicar insumo', descripcion: 'El movimiento descuenta existencias y registra el costo en este ciclo.',
      inicial: { fecha: formato.hoy(), costo_unitario: 0 },
      campos: valores => {
        const seleccionado = this.insumos().find(item => item.id === Number(valores['insumo_id']));
        const opciones = this.insumos().map(item => ({
          value: item.id, label: item.nombre + ' · ' + formato.numero(item.stock) + ' ' + formato.unidades[item.unidad]
        }));
        return [selector('insumo_id', 'Insumo disponible', opciones, true),
          ...camposMovimiento(seleccionado?.unidad, ciclo.fecha_siembra, seleccionado && Number(seleccionado.stock))];
      }
    }, '/consumos', 'POST', 'Aplicación registrada y stock actualizado.');
  }

  cosechar(ciclo: DetalleCiclo) {
    this.abrir({
      titulo: 'Registrar cosecha', accion: 'Guardar cosecha', inicial: { fecha: formato.hoy(), nota: '' },
      campos: [folioCampo(), fechaCampo('fecha', 'Fecha de cosecha', ciclo.fecha_siembra, formato.hoy()),
        cantidad('kilogramos', 'Cantidad (kg)'), notas()]
    }, '/cosechas', 'POST', 'Cosecha registrada.');
  }

  borrarActividad(registro: Actividad) {
    this.confirmar('Eliminar actividad', 'Se retirará la actividad pendiente.', '/actividades/' + registro.id, 'Actividad eliminada.');
  }

  retirarCosecha(registro: Cosecha) {
    this.confirmar('Retirar cosecha', 'La producción y el rendimiento se recalcularán con las cosechas restantes.',
      '/cosechas/' + registro.id, 'Cosecha retirada.');
  }

  borrarCiclo(ciclo: DetalleCiclo) {
    this.confirmar('Eliminar ciclo', 'Se eliminará la planificación de ' + ciclo.codigo + '.', '', 'Ciclo eliminado.');
  }

  async pdf(ciclo: DetalleCiclo) {
    this.pdfActivo.set(true);
    this.error.set('');
    try { await this.api.descargarFicha(ciclo.id, ciclo.codigo); }
    catch (error) { this.error.set(mensajeError(error)); }
    finally { this.pdfActivo.set(false); }
  }

  private confirmar(titulo: string, descripcion: string, ruta: string, exito: string) {
    this.abrir({ titulo, descripcion, accion: 'Confirmar eliminación', peligro: true }, ruta, 'DELETE', exito);
  }

  private abrir(formulario: Omit<Formulario, 'enviar'>, ruta: string, metodo: string, mensaje: string) {
    this.exito.set('');
    this.error.set('');
    const id = this.id;
    this.modal.set({ ...formulario, enviar: async datos => {
      const respuesta = await this.api.pedir<{offlineQueued?: boolean; mensaje?: string}>(
        '/ciclos/' + id + ruta, metodo, metodo === 'DELETE' ? undefined : datos
      );
      this.modal.set(null);
      if (respuesta?.offlineQueued) {
        this.exito.set(respuesta.mensaje || 'Operación guardada en el dispositivo; se sincronizará al recuperar Internet.');
        return;
      }
      if (metodo === 'DELETE' && !ruta) {
        await this.router.navigate(['/ciclos'], { replaceUrl: true, state: { exito: mensaje } });
      } else {
        this.exito.set(mensaje);
        await this.ciclo.cargar();
        if (ruta === '/consumos') await this.stock.cargar();
      }
    } });
  }
}
