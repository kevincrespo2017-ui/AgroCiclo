import { provideHttpClient } from '@angular/common/http';
import { Component, provideZonelessChangeDetection } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { RouterOutlet, provideRouter, withInMemoryScrolling } from '@angular/router';
import { rutas } from './app/rutas';

@Component({ selector: 'agro-app', imports: [RouterOutlet], template: '<router-outlet />' })
/** AgroCiclo: raíz de la aplicación Angular y punto de montaje del enrutador. */
class AgroCiclo {}

bootstrapApplication(AgroCiclo, {
  providers: [provideZonelessChangeDetection(), provideHttpClient(),
    provideRouter(rutas, withInMemoryScrolling({ scrollPositionRestoration: 'top' }))]
}).catch((error) => console.error(error));

if ('serviceWorker' in navigator && location.protocol.startsWith('http')) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/service-worker.js').catch(() => undefined));
}
