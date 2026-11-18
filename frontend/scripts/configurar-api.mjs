import { existsSync, writeFileSync } from 'node:fs';

const archivoEntorno = new URL('../.env', import.meta.url);
if (existsSync(archivoEntorno)) process.loadEnvFile(archivoEntorno);
const url = new URL(process.env.AGROCICLO_API_URL || 'http://localhost:4000/api');
if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password ||
    url.search || url.hash || !/^[\/]api\/?$/.test(url.pathname)) {
  throw Error('AGROCICLO_API_URL debe ser una URL HTTP(S) seguida de /api.');
}
const destino = new URL('../public/api-config.js', import.meta.url);
writeFileSync(destino, 'globalThis.AGROCICLO_API_URL = ' + JSON.stringify(url.origin + '/api') + ';\n');
