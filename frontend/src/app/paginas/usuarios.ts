import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { Api, mensajeError } from '../comun/api';
import { Aviso, Cargando, Dialogo, Estado, Icono, Tabla, Vacio } from '../comun/ui';

/**
 * CuentaPendiente: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
interface CuentaPendiente {
  id: number; nombre: string; email: string; rol: 'OPERATIVO'|'SUPERVISOR'; verificado: boolean;
  estado_aprobacion: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO'; verificacion_manual: boolean;
  motivo_rechazo: string | null; creado_en: string;
}

@Component({
  selector: 'agro-usuarios', host: { class: 'agro-pagina' },
  imports: [Aviso, Cargando, Dialogo, Estado, Icono, Tabla, Vacio],
  template: `
    <header class="encabezado-pagina"><div><h1>Gestión de usuarios</h1><p>Revisa las cuentas registradas y controla su acceso a AgroCiclo.</p></div>
      <button class="boton secundario" type="button" (click)="cargar()"><agro-icon nombre="refresh-cw"/> Actualizar</button>
    </header>
    <section class="usuarios-resumen">
      <article><span>Pendientes</span><strong>{{ pendientes() }}</strong></article>
      <article><span>Correo sin verificar</span><strong>{{ sinVerificar() }}</strong></article>
      <article><span>Aprobados</span><strong>{{ aprobados() }}</strong></article>
      <article><span>Rechazados</span><strong>{{ rechazados() }}</strong></article>
    </section>
    @if (exito()) {<agro-aviso [mensaje]="exito()" tipo="bien"/>}
    @if (error()) {<agro-aviso [mensaje]="error()"/>}
    <section class="tarjeta">
      <header class="tarjeta-titulo"><div><h2>Solicitudes de acceso</h2><p>El administrador puede aprobar incluso si el usuario no logró recibir el código de correo.</p></div>
        <div class="pestanas" role="group" aria-label="Filtrar usuarios">
          <button type="button" [class.activa]="filtro()==='TODOS'" (click)="filtro.set('TODOS')">Todos</button>
          <button type="button" [class.activa]="filtro()==='PENDIENTE'" (click)="filtro.set('PENDIENTE')">Pendientes</button>
          <button type="button" [class.activa]="filtro()==='APROBADO'" (click)="filtro.set('APROBADO')">Aprobados</button>
          <button type="button" [class.activa]="filtro()==='RECHAZADO'" (click)="filtro.set('RECHAZADO')">Rechazados</button>
        </div>
      </header>
      @if (cargando()) {<agro-cargando texto="Cargando usuarios…"/>}
      @else if (usuariosFiltrados().length) {
        <agro-tabla etiqueta="Usuarios registrados" [columnas]="['Usuario','Correo electrónico','Rol','Verificación','Estado','Registro','Acciones']">
          @for (u of usuariosFiltrados(); track u.id) {
            <tr>
              <th scope="row"><div class="usuario-identidad"><span class="avatar">{{ iniciales(u.nombre) }}</span><span><strong>{{u.nombre}}</strong><small>Cuenta operativa #{{u.id}}</small></span></div></th>
              <td>{{u.email}}</td>
              <td><select aria-label="Rol del usuario" [value]="u.rol" [disabled]="procesando()===u.id || u.estado_aprobacion!=='APROBADO'" (change)="cambiarRol(u,$any($event.target).value)"><option value="OPERATIVO">Operativo</option><option value="SUPERVISOR">Supervisor</option></select></td>
              <td><agro-estado [tono]="u.verificado?'sembrado':'planificado'">{{u.verificado?'Correo verificado':'Sin verificar'}}</agro-estado></td>
              <td><agro-estado [tono]="u.estado_aprobacion.toLowerCase()">{{estadoTexto(u.estado_aprobacion)}}</agro-estado>
                @if (u.verificacion_manual) {<small class="nota-manual">Aprobación administrativa</small>}
                @if (u.motivo_rechazo) {<small class="nota-manual">{{u.motivo_rechazo}}</small>}
              </td>
              <td>{{fecha(u.creado_en)}}</td>
              <td><div class="acciones fila-acciones">
                @if (u.estado_aprobacion !== 'APROBADO') {
                  <button type="button" class="boton primario pequeno" [disabled]="procesando()===u.id" (click)="aprobar(u)"><agro-icon nombre="check" [tam]="14"/> Aprobar</button>
                }
                @if (u.estado_aprobacion !== 'RECHAZADO') {
                  <button type="button" class="boton secundario pequeno" [disabled]="procesando()===u.id" (click)="seleccionarRechazo(u)"><agro-icon nombre="x" [tam]="14"/> Rechazar</button>
                }
              </div></td>
            </tr>
          }
        </agro-tabla>
      } @else {<agro-vacio titulo="No hay cuentas en este filtro" texto="Las nuevas solicitudes aparecerán aquí cuando alguien se registre."/>}
    </section>
    @if (rechazo(); as u) {
      <agro-dialogo [titulo]="'Rechazar solicitud de '+u.nombre" descripcion="Puedes indicar un motivo para que el usuario sepa por qué no fue aprobada su cuenta." (cerrar)="rechazo.set(null)">
        <form class="form-rechazo" (submit)="confirmarRechazo($event)">
          <div class="campo"><label for="motivo">Motivo (opcional)</label><textarea id="motivo" name="motivo" rows="4" maxlength="300" placeholder="Escribe el motivo del rechazo…"></textarea></div>
          <div class="dialogo-pie"><button type="button" class="boton secundario" (click)="rechazo.set(null)">Cancelar</button><button class="boton peligro" [disabled]="procesando()===u.id">Confirmar rechazo</button></div>
        </form>
      </agro-dialogo>
    }
  `
})
/**
 * UsuariosPagina: Componente de pantalla que presenta y coordina las operaciones de su módulo.
 */
export class UsuariosPagina implements OnInit {
  private api = inject(Api);
  usuarios = signal<CuentaPendiente[]>([]);
  cargando = signal(false); error = signal(''); exito = signal(''); filtro = signal('TODOS');
  procesando = signal<number | null>(null); rechazo = signal<CuentaPendiente | null>(null);
  usuariosFiltrados = computed(() => this.filtro()==='TODOS' ? this.usuarios() : this.usuarios().filter(u=>u.estado_aprobacion===this.filtro()));
  pendientes = computed(()=>this.usuarios().filter(u=>u.estado_aprobacion==='PENDIENTE').length);
  sinVerificar = computed(()=>this.usuarios().filter(u=>!u.verificado && u.estado_aprobacion==='PENDIENTE').length);
  aprobados = computed(()=>this.usuarios().filter(u=>u.estado_aprobacion==='APROBADO').length);
  rechazados = computed(()=>this.usuarios().filter(u=>u.estado_aprobacion==='RECHAZADO').length);
  ngOnInit(){void this.cargar();}
  async cargar(){this.cargando.set(true);this.error.set('');try{this.usuarios.set(await this.api.pedir<CuentaPendiente[]>('/usuarios'));}catch(e){this.error.set(mensajeError(e));}finally{this.cargando.set(false);}}
  iniciales(nombre:string){return nombre.split(/\s+/).slice(0,2).map(p=>p[0]?.toUpperCase()||'').join('');}
  estadoTexto(estado:string){return estado==='APROBADO'?'Aprobado':estado==='RECHAZADO'?'Rechazado':'Pendiente';}
  fecha(valor:string){return new Intl.DateTimeFormat('es-EC',{dateStyle:'medium'}).format(new Date(valor));}
  async aprobar(u:CuentaPendiente){this.procesando.set(u.id);this.error.set('');this.exito.set('');try{const r=await this.api.pedir<{mensaje:string}>(`/usuarios/${u.id}/aprobar`,'PATCH',{});this.exito.set(r.mensaje);await this.cargar();}catch(e){this.error.set(mensajeError(e));}finally{this.procesando.set(null);}}
  async cambiarRol(u:CuentaPendiente, rol:'OPERATIVO'|'SUPERVISOR') {
    if (u.rol === rol) return;
    this.procesando.set(u.id); this.error.set(''); this.exito.set('');
    try { const r=await this.api.pedir<{mensaje:string}>(`/usuarios/${u.id}/rol`,'PATCH',{rol}); this.exito.set(r.mensaje); await this.cargar(); }
    catch(e){this.error.set(mensajeError(e));}
    finally{this.procesando.set(null);}
  }
  seleccionarRechazo(u:CuentaPendiente){this.rechazo.set(u);}
  async confirmarRechazo(evento:Event){evento.preventDefault();const u=this.rechazo();if(!u)return;const f=new FormData(evento.target as HTMLFormElement);this.procesando.set(u.id);this.error.set('');try{const r=await this.api.pedir<{mensaje:string}>(`/usuarios/${u.id}/rechazar`,'PATCH',{motivo:String(f.get('motivo')||'')});this.exito.set(r.mensaje);this.rechazo.set(null);await this.cargar();}catch(e){this.error.set(mensajeError(e));}finally{this.procesando.set(null);}}
}
