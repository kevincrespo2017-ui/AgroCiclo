export type Decimal = string | number;
export type EstadoCiclo = 'PLANIFICADO' | 'SEMBRADO' | 'CERRADO';
export type Unidad = 'KG' | 'L' | 'UN';
export type Categoria = 'FERTILIZANTE' | 'SEMILLA' | 'FITOSANITARIO' | 'OTRO';

/**
 * Usuario: Punto de entrada y configuración principal de la aplicación.
 */
export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'SUPERVISOR' | 'OPERATIVO';
}

/**
 * Sesion: Punto de entrada y configuración principal de la aplicación.
 */
export interface Sesion { token: string; usuario: Usuario }

/**
 * Terreno: Punto de entrada y configuración principal de la aplicación.
 */
export interface Terreno {
  id: number; nombre: string; ubicacion: string; area_ha: Decimal;
  imagen_url?: string | null; latitud?: Decimal | null; longitud?: Decimal | null;
}

/**
 * Cultivo: Punto de entrada y configuración principal de la aplicación.
 */
export interface Cultivo { id: number; nombre: string; dias_estimados: number }

/**
 * Insumo: Punto de entrada y configuración principal de la aplicación.
 */
export interface Insumo {
  id: number;
  nombre: string;
  categoria: Categoria;
  unidad: Unidad;
  stock: Decimal;
  stock_minimo: Decimal;
}

/**
 * Ciclo: Punto de entrada y configuración principal de la aplicación.
 */
export interface Ciclo {
  id: number;
  codigo: string;
  terreno_id: number;
  cultivo_id: number;
  terreno: string;
  cultivo: string;
  area_ha: Decimal;
  estado: EstadoCiclo;
  fecha_inicio: string;
  fecha_siembra: string | null;
  fecha_estimada: string | null;
  fecha_cierre: string | null;
  notas: string;
  kg_producidos: Decimal;
  costo_total: Decimal;
  costo_actividades: Decimal;
  costo_insumos: Decimal;
  rendimiento_kg_ha: Decimal | null;
  obligatorias_pendientes: number;
}

/**
 * Actividad: Punto de entrada y configuración principal de la aplicación.
 */
export interface Actividad {
  id: number;
  nombre: string;
  programada_para: string;
  obligatoria: boolean;
  completada_en: string | null;
  costo: Decimal;
  nota: string;
}

/**
 * Cosecha: Punto de entrada y configuración principal de la aplicación.
 */
export interface Cosecha { id: number; folio: string; fecha: string; kilogramos: Decimal; nota: string }

/**
 * Movimiento: Punto de entrada y configuración principal de la aplicación.
 */
export interface Movimiento {
  id: number;
  folio: string;
  tipo: 'ENTRADA' | 'CONSUMO';
  fecha: string;
  cantidad: Decimal;
  costo_unitario: Decimal;
  ciclo?: string | null;
}

/**
 * Consumo: Punto de entrada y configuración principal de la aplicación.
 */
export interface Consumo extends Movimiento { insumo: string; unidad: Unidad; costo: Decimal }

/**
 * DetalleCiclo: Punto de entrada y configuración principal de la aplicación.
 */
export interface DetalleCiclo extends Ciclo { actividades: Actividad[]; cosechas: Cosecha[]; consumos: Consumo[] }

/**
 * Pagina: Punto de entrada y configuración principal de la aplicación.
 */
export interface Pagina<T> { datos: T[]; pagina: number; tam: number; total: number }

/**
 * FiltrosCiclo: Punto de entrada y configuración principal de la aplicación.
 */
export interface FiltrosCiclo {
  q?: string;
  estado?: string;
  cultivoId?: string;
  terrenoId?: string;
  desde?: string;
  hasta?: string;
  ordenar: string;
  direccion: string;
}

/**
 * Indicadores: Punto de entrada y configuración principal de la aplicación.
 */
export interface Indicadores {
  resumen: { ciclos: number; sembrados: number; planificados: number; cerrados: number;
    produccion_kg: Decimal; costo_usd: Decimal; rendimiento_kg_ha: Decimal | null };
  cultivos: { cultivo_id: number; cultivo: string; produccion_kg: Decimal; costo_usd: Decimal;
    rendimiento_kg_ha: Decimal | null; ciclos: number }[];
}

/**
 * Alerta: Punto de entrada y configuración principal de la aplicación.
 */
export interface Alerta {
  tipo: 'INVENTARIO' | 'COSECHA' | 'ACTIVIDAD';
  origen_id: number;
  titulo: string;
  referencia: string;
  vence: string | null;
  ciclo_id: number | null;
  insumo_id: number | null;
  nivel: 'VENCIDA' | 'PROXIMA' | 'BAJO_STOCK';
}

/**
 * Auditoria: Punto de entrada y configuración principal de la aplicación.
 */
export interface Auditoria {
  id: number;
  usuario: string;
  email: string;
  fecha_hora: string;
  entidad: string;
  accion: string;
  registro_id: number;
  detalle: unknown;
}
