import { Decimal } from '../modelos';

const zona = 'America/Guayaquil';
const cantidad = new Intl.NumberFormat('es-EC', { maximumFractionDigits: 3 });
const dolares = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' });
const fechaCorta = new Intl.DateTimeFormat('es-EC', { timeZone: zona, day: 'numeric', month: 'short', year: 'numeric' });
const fechaHora = new Intl.DateTimeFormat('es-EC', { timeZone: zona, dateStyle: 'medium', timeStyle: 'short' });

export const formato = {
  numero: (valor: Decimal) => cantidad.format(Number(valor)),
  moneda: (valor: Decimal) => dolares.format(Number(valor)),
  fecha: (valor?: string | null) => valor ? fechaCorta.format(new Date(valor + 'T12:00:00Z')) : '—',
  hora: (valor: string) => fechaHora.format(new Date(valor)),
  hoy: () => new Intl.DateTimeFormat('en-CA', {
    timeZone: zona, year: 'numeric', month: '2-digit', day: '2-digit'
  }).format(new Date()),
  diaLargo: () => new Intl.DateTimeFormat('es-EC', {
    timeZone: zona, day: 'numeric', month: 'long', year: 'numeric'
  }).format(new Date()),
  rendimiento: (valor?: Decimal | null) => valor == null ? '—' : cantidad.format(Number(valor)) + ' kg/ha',
  estados: { PLANIFICADO: 'Planificado', SEMBRADO: 'Sembrado', CERRADO: 'Cerrado' },
  unidades: { KG: 'kg', L: 'L', UN: 'un.' },
  categorias: { FERTILIZANTE: 'Fertilizante', SEMILLA: 'Semilla', FITOSANITARIO: 'Fitosanitario', OTRO: 'Otro' }
};
