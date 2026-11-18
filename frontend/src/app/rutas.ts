import { Component } from '@angular/core';
import { Routes, RouterLink } from '@angular/router';
import { Shell } from './comun/shell';
import { requiereSesion, requiereAdmin } from './comun/sesion';

@Component({
  selector: 'agro-no-encontrada', imports: [RouterLink], host: { class: 'agro-pagina' },
  template: '<h1>Página no encontrada</h1><a class="enlace" routerLink="/">Volver al resumen</a>'
})
/**
 * NoEncontrada: Punto de entrada y configuración principal de la aplicación.
 */
class NoEncontrada {}

export const rutas: Routes = [
  { path: 'cuenta/:modo', title: 'Tu cuenta | AgroCiclo', loadComponent: () => import('./paginas/cuenta').then(m => m.CuentaPagina) },
  { path: 'acceso', title: 'Acceso | AgroCiclo', loadComponent: () => import('./paginas/acceso').then(m => m.AccesoPagina) },
  { path: '', component: Shell, canActivate: [requiereSesion], children: [
    { path: '', pathMatch: 'full', title: 'Resumen', loadComponent: () => import('./paginas/panel').then(m => m.Panel) },
    { path: 'ciclos', title: 'Ciclos agrícolas', loadComponent: () => import('./paginas/ciclos').then(m => m.CiclosPagina) },
    { path: 'ciclos/:id', title: 'Ciclos agrícolas', loadComponent: () => import('./paginas/ciclo-detalle').then(m => m.CicloDetalle) },
    { path: 'catalogos', title: 'Terrenos y cultivos', loadComponent: () => import('./paginas/catalogos').then(m => m.CatalogosPagina) },
    { path: 'insumos', title: 'Insumos', data: { inventario: true }, loadComponent: () => import('./paginas/catalogos').then(m => m.CatalogosPagina) },
     { path: 'productividad', title: 'Productividad', loadComponent: () => import('./paginas/productividad').then(m => m.Productividad) },
    { path: 'reportes', title: 'Centro de reportes PDF', loadComponent: () => import('./paginas/reportes').then(m => m.ReportesPagina) },
    { path: 'inteligencia-agricola', title: 'Inteligencia agrícola', loadComponent: () => import('./paginas/inteligencia-agricola').then(m => m.InteligenciaAgricolaPagina) },
    { path: 'auditoria', title: 'Auditoría', canActivate: [requiereAdmin],
      loadComponent: () => import('./paginas/auditoria').then(m => m.AuditoriaPagina) },
    { path: 'usuarios', title: 'Gestión de usuarios', canActivate: [requiereAdmin],
      loadComponent: () => import('./paginas/usuarios').then(m => m.UsuariosPagina) },
    { path: '**', component: NoEncontrada, title: 'Página no encontrada' }
  ] }
];
