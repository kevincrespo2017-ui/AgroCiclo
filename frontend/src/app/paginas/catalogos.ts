import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Cultivo, Insumo, Movimiento, Pagina, Terreno } from '../modelos';
import { Api, Consulta, conConsulta, mensajeError } from '../comun/api';
import { Acceso } from '../comun/sesion';
import { camposCultivo, camposInsumo, camposMovimiento, camposTerreno, Formulario, Valores } from '../comun/campos';
import { formato } from '../comun/formato';
import { Aviso, Cargando, Dialogo, Estado, FormularioUi, Icono, Paginador, Tabla, Vacio } from '../comun/ui';

type Catalogo = 'terrenos' | 'cultivos' | 'insumos';
type Registro = Terreno | Cultivo | Insumo;
const catalogos = {
  terrenos: { singular: 'terreno', columnas: ['Terreno','Ubicación','Superficie'], campos: camposTerreno },
  cultivos: { singular: 'cultivo', columnas: ['Cultivo','Cosecha estimada'], campos: camposCultivo },
  insumos: { singular: 'insumo', columnas: ['Insumo','Categoría','Disponible','Umbral'], campos: camposInsumo }
};

@Component({
  selector: 'agro-catalogos', host: { class: 'agro-pagina' },
  imports: [Aviso, Cargando, Dialogo, Estado, FormularioUi, Icono, Paginador, Tabla, Vacio],
  templateUrl: './catalogos.html'
})
/**
 * CatalogosPagina: administra terrenos, cultivos e insumos; incluye fotografías, QR,
 * edición de catálogos y consulta de movimientos de inventario.
 */
export class CatalogosPagina implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly acceso = inject(Acceso);
  readonly admin = computed(() => this.acceso.usuario()?.rol === 'ADMIN');
  readonly gestor = computed(() => ['ADMIN', 'SUPERVISOR'].includes(this.acceso.usuario()?.rol ?? ''));
  readonly inventario = Boolean(this.ruta.snapshot.data['inventario']);
  readonly tipo = signal<Catalogo>(this.inventario ? 'insumos' : 'terrenos');
  readonly configuracion = computed(() => catalogos[this.tipo()]);
  readonly columnas = computed(() => [...this.configuracion().columnas, ...(this.tipo() === 'terrenos' ? ['Código QR'] : []), ...(this.gestor() || this.inventario ? ['Acciones'] : [])]);
  readonly f = formato;
  readonly recurso = new Consulta<Registro[]>(this.api);
  readonly movimientos = new Consulta<Pagina<Movimiento>>(this.api);
  readonly modal = signal<Formulario | null>(null);
  readonly historial = signal<Insumo | null>(null);
  readonly qrTerreno = signal<Terreno | null>(null);
  readonly exito = signal('');
  readonly error = signal('');
  readonly filas = computed(() => (this.recurso.datos() ?? []).map(registro => {
    if ('stock' in registro) return {
      registro, otros: [formato.categorias[registro.categoria],
        formato.numero(registro.stock) + ' ' + formato.unidades[registro.unidad],
        formato.numero(registro.stock_minimo) + ' ' + formato.unidades[registro.unidad]],
      insumo: registro, terreno: null as Terreno | null, bajo: Number(registro.stock) <= Number(registro.stock_minimo)
    };
    if ('area_ha' in registro) return {
      registro, otros: [registro.ubicacion, formato.numero(registro.area_ha) + ' ha'], insumo: null, terreno: registro, bajo: false
    };
    return { registro, otros: [registro.dias_estimados + ' días después de la siembra'], insumo: null, terreno: null as Terreno | null, bajo: false };
  }));

  constructor() {
    this.api.sincronizacionCompletada.pipe(takeUntilDestroyed()).subscribe(cantidad => {
      this.exito.set(`${cantidad} operación(es) de inventario sincronizada(s) correctamente.`);
      void this.recurso.cargar('/' + this.tipo());
    });
  }

  async ngOnInit() {
    await this.recurso.cargar('/' + this.tipo());
    const id = Number(this.ruta.snapshot.queryParamMap.get('inventario'));
    const seleccionado = this.filas().find(fila => fila.insumo?.id === id)?.insumo;
    if (seleccionado) this.abrirHistorial(seleccionado);
  }

  cambiar(tipo: Catalogo) {
    this.tipo.set(tipo);
    this.exito.set('');
    void this.recurso.cargar('/' + tipo);
  }

  abrirHistorial(insumo: Insumo) { this.historial.set(insumo); this.paginaHistorial(1); }
  urlImagen(url: string | null | undefined) { return this.api.urlImagen(url); }

  async subirImagen(terreno: Terreno, evento: Event) {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;
    this.error.set('');
    if (archivo.size > 5 * 1024 * 1024) {
      this.error.set('La fotografía no puede superar 5 MB.');
      input.value = '';
      return;
    }
    if (!['image/jpeg','image/png','image/webp'].includes(archivo.type)) {
      this.error.set('Formato no permitido. Selecciona JPG, PNG o WebP.');
      input.value = '';
      return;
    }
    try {
      await this.api.subirImagenTerreno(terreno.id, archivo);
      this.exito.set('Fotografía del terreno actualizada correctamente.');
      await this.recurso.cargar('/terrenos');
    } catch (error) {
      this.error.set(mensajeError(error));
    } finally {
      input.value = '';
    }
  }

  mostrarQr(terreno: Terreno) { this.qrTerreno.set(terreno); }
  mapaUrl(terreno: Terreno) {
    return terreno.latitud != null && terreno.longitud != null
      ? `https://www.google.com/maps?q=${terreno.latitud},${terreno.longitud}`
      : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(terreno.ubicacion)}`;
  }
  qrUrl(terreno: Terreno) {
    return 'https://api.qrserver.com/v1/create-qr-code/?size=260x260&margin=12&data=' + encodeURIComponent(this.mapaUrl(terreno));
  }

  paginaHistorial(pagina: number) {
    void this.movimientos.cargar(conConsulta('/insumos/' + this.historial()?.id + '/movimientos', { pagina }));
  }

  cerrarHistorial() {
    this.historial.set(null);
    if (this.ruta.snapshot.queryParamMap.has('inventario')) {
      void this.router.navigate([], { relativeTo: this.ruta, queryParams: {}, replaceUrl: true });
    }
  }

  crear(registro?: Registro) {
    const ruta = '/' + this.tipo() + (registro ? '/' + registro.id : '');
    this.abrir({
      titulo: (registro ? 'Editar ' : 'Nuevo ') + this.configuracion().singular,
      campos: this.configuracion().campos,
      inicial: registro ? { ...registro } : { stock_minimo: 0 }
    }, ruta, registro ? 'PUT' : 'POST', registro ? 'Registro actualizado.' : 'Registro creado.');
  }

  borrar(registro: Registro) {
    this.abrir({
      titulo: 'Eliminar ' + registro.nombre, descripcion: 'La eliminación requiere que el registro no tenga movimientos ni ciclos vinculados.',
      accion: 'Eliminar', peligro: true
    }, '/' + this.tipo() + '/' + registro.id, 'DELETE', 'Registro eliminado.');
  }

  entrada(insumo: Insumo) {
    this.abrir({
      titulo: 'Ingresar stock · ' + insumo.nombre, campos: camposMovimiento(insumo.unidad),
      inicial: { fecha: formato.hoy(), costo_unitario: 0 }
    }, '/insumos/' + insumo.id + '/entradas', 'POST', 'Entrada registrada.');
  }

  private abrir(formulario: Omit<Formulario, 'enviar'>, ruta: string, metodo: string, mensaje: string) {
    this.exito.set('');
    this.modal.set({ ...formulario, enviar: async (datos: Valores) => {
      const respuesta = await this.api.pedir<{offlineQueued?: boolean; mensaje?: string}>(
        ruta, metodo, metodo === 'DELETE' ? undefined : datos
      );
      this.modal.set(null);
      if (respuesta?.offlineQueued) {
        this.exito.set(respuesta.mensaje || 'Movimiento guardado en el dispositivo; se sincronizará al recuperar Internet.');
        return;
      }
      this.exito.set(mensaje);
      await this.recurso.cargar();
    } });
  }
}
