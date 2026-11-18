import { Cultivo, Terreno, Unidad } from '../modelos';
import { formato } from './formato';

export type ValorCampo = string | number | boolean | null | undefined;
export type Valores = Record<string, ValorCampo>;
/**
 * Opcion: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export interface Opcion { value: string | number; label: string }
/**
 * Campo: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export interface Campo {
  nombre: string; etiqueta: string; tipo?: string; required?: boolean; numerico?: boolean;
  opciones?: Opcion[]; min?: string | number; max?: string | number; step?: number;
  maxLength?: number; minLength?: number; placeholder?: string; ayuda?: string;
}
/**
 * Formulario: Componente o servicio compartido para mantener una responsabilidad reutilizable de la interfaz.
 */
export interface Formulario {
  titulo: string; descripcion?: string; accion?: string; peligro?: boolean; inicial?: Valores;
  campos?: Campo[] | ((valores: Valores) => Campo[]);
  enviar: (valores: Valores) => Promise<void>;
}

export const texto = (nombre: string, etiqueta: string, maxLength = 100): Campo =>
  ({ nombre, etiqueta, maxLength, required: true });
export const cantidad = (nombre: string, etiqueta: string, min = 0.001, max = 1_000_000, step = 0.001): Campo =>
  ({ nombre, etiqueta, tipo: 'number', min, max, step, required: true });
export const fechaCampo = (nombre: string, etiqueta: string, min?: string | null, max?: string): Campo =>
  ({ nombre, etiqueta, tipo: 'date', min: min ?? undefined, max, required: true });
export const notas = (nombre = 'nota', etiqueta = 'Observaciones'): Campo => ({ nombre, etiqueta, tipo: 'textarea', maxLength: 500 });
export const folioCampo = (nombre = 'folio', etiqueta = 'Folio'): Campo => ({
  ...texto(nombre, etiqueta, 30), minLength: 2, ayuda: 'Usa letras, números, guiones o guiones bajos.'
});
export const opcionesCatalogo = (lista: { id: number; nombre: string }[]): Opcion[] =>
  lista.map(({ id, nombre }) => ({ value: id, label: nombre }));
export const opcionesDe = (objeto: Record<string, string>): Opcion[] =>
  Object.entries(objeto).map(([value, label]) => ({ value, label }));
export const selector = (nombre: string, etiqueta: string, opciones: Opcion[], numerico = false): Campo => ({
  nombre, etiqueta, numerico, required: true, opciones: [{ value: '', label: 'Selecciona una opción' }, ...opciones]
});

export const camposTerreno = [texto('nombre', 'Nombre'), texto('ubicacion', 'Ubicación', 120),
  cantidad('area_ha', 'Superficie (ha)', 0.001, 100_000),
  { nombre: 'imagen_url', etiqueta: 'URL de fotografía del terreno', tipo: 'text', required: false, maxLength: 500, placeholder: 'https://… o /uploads/terrenos/…', ayuda: 'Puedes pegar un enlace público o subir una fotografía desde la tarjeta del terreno.' },
  { nombre: 'latitud', etiqueta: 'Latitud', tipo: 'number', required: false, min: -90, max: 90, step: 0.000001, placeholder: '-1.492…' },
  { nombre: 'longitud', etiqueta: 'Longitud', tipo: 'number', required: false, min: -180, max: 180, step: 0.000001, placeholder: '-78.001…' }];
export const camposCultivo = [texto('nombre', 'Nombre'), cantidad('dias_estimados', 'Días estimados hasta cosecha', 1, 730, 1)];
export const camposInsumo = (valores: Valores) => [
  texto('nombre', 'Nombre'), selector('categoria', 'Categoría', opcionesDe(formato.categorias)),
  selector('unidad', 'Unidad de inventario', opcionesDe(formato.unidades)),
  cantidad('stock_minimo', 'Umbral de stock bajo', 0, 100_000, valores['unidad'] === 'UN' ? 1 : 0.001)
];

export function camposCiclo(terrenos: Terreno[], cultivos: Cultivo[], valores: Valores) {
  const terreno = terrenos.find((item) => item.id === Number(valores['terreno_id']));
  return [folioCampo('codigo', 'Código del ciclo'),
    selector('terreno_id', 'Terreno', opcionesCatalogo(terrenos), true),
    selector('cultivo_id', 'Cultivo', opcionesCatalogo(cultivos), true),
    cantidad('area_ha', 'Área del ciclo (ha)', 0.001, terreno ? Number(terreno.area_ha) : 100_000),
    fechaCampo('fecha_inicio', 'Fecha de inicio'), notas('notas')];
}

export const camposMovimiento = (unidad?: Unidad, fechaMinima?: string | null, maximo?: number) => [
  folioCampo(), fechaCampo('fecha', 'Fecha del movimiento', fechaMinima, formato.hoy()),
  cantidad('cantidad', unidad ? 'Cantidad (' + formato.unidades[unidad] + ')' : 'Cantidad',
    unidad === 'UN' ? 1 : 0.001, maximo == null ? 1_000_000 : Math.min(maximo, 1_000_000), unidad === 'UN' ? 1 : 0.001),
  cantidad('costo_unitario', 'Costo unitario (USD)', 0, 1_000_000, 0.01)
];
