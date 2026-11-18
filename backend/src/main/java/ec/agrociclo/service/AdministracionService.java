package ec.agrociclo.service;

import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.model.Usuario;
import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.security.Permisos;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * AdministracionService: gestiona solicitudes de acceso, aprobación/rechazo manual,
 * asignación de roles, notificaciones y auditoría de decisiones administrativas.
 */
public class AdministracionService {
    private final BaseDatos db;
    private final NotificacionesService notificaciones;
    private final Correo correo;
    public AdministracionService(BaseDatos db, NotificacionesService notificaciones, Correo correo) {
        this.db = db; this.notificaciones = notificaciones; this.correo = correo;
    }

    public List<Map<String,Object>> usuarios(Usuario actor) {
        Permisos.exigirAdmin(actor);
        return db.filas("""
            SELECT id, nombre, email, rol, verificado, estado_aprobacion, verificacion_manual,
                   motivo_rechazo, creado_en
            FROM usuarios WHERE rol <> 'ADMIN'
            ORDER BY CASE estado_aprobacion WHEN 'PENDIENTE' THEN 0 WHEN 'RECHAZADO' THEN 1 ELSE 2 END,
                     creado_en DESC, id DESC
            """);
    }

    @Transactional
    public Map<String,Object> aprobar(Usuario actor, int id) {
        Permisos.exigirAdmin(actor);
        var usuario = db.requerida("la cuenta", "SELECT id,nombre,email,verificado,estado_aprobacion FROM usuarios WHERE id=? AND rol='OPERATIVO' FOR UPDATE", id);
        db.ejecutar("UPDATE usuarios SET verificado=true, estado_aprobacion='APROBADO', verificacion_manual=NOT ?, motivo_rechazo=NULL WHERE id=?", Boolean.TRUE.equals(usuario.get("verificado")), id);
        db.ejecutar("DELETE FROM tokens_cuenta WHERE usuario_id=? AND tipo='VERIFICAR'", id);
        db.ejecutar("DELETE FROM sesiones WHERE usuario_id=?", id);
        notificaciones.paraUsuario(id, "Cuenta aprobada", "Tu cuenta de AgroCiclo fue aprobada. Ya puedes iniciar sesión.", "CUENTA", "/acceso");
        try { correo.estadoCuenta(usuario.get("email").toString(), true, ""); } catch (ErrorApi ignorado) { /* La notificación interna se conserva aunque el SMTP no responda. */ }
        db.auditar(actor, "APROBAR_USUARIO", "usuarios", id, Map.of("correo", usuario.get("email"), "verificacion_omitida", !Boolean.TRUE.equals(usuario.get("verificado"))));
        return Map.of("mensaje", "Cuenta aprobada correctamente.");
    }

    @Transactional
    public Map<String,Object> cambiarRol(Usuario actor, int id, String rol) {
        Permisos.exigirAdmin(actor);
        String nuevo = rol == null ? "" : rol.strip().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("OPERATIVO", "SUPERVISOR").contains(nuevo))
            throw ErrorApi.campo("rol", "El rol debe ser OPERATIVO o SUPERVISOR.");
        var cuenta = db.requerida("la cuenta", "SELECT id,nombre,email,rol,estado_aprobacion FROM usuarios WHERE id=? AND rol <> 'ADMIN' FOR UPDATE", id);
        if (!"APROBADO".equals(cuenta.get("estado_aprobacion")))
            throw ErrorApi.conflicto("CUENTA_PENDIENTE", "Aprueba primero la cuenta antes de asignar el rol definitivo.");
        String anterior = String.valueOf(cuenta.get("rol"));
        db.ejecutar("UPDATE usuarios SET rol=? WHERE id=?", nuevo, id);
        db.auditar(actor, "CAMBIAR_ROL", "usuarios", id, Map.of("anterior", anterior, "nuevo", nuevo));
        return Map.of("mensaje", "Rol actualizado correctamente.", "rol", nuevo);
    }

    @Transactional
    public Map<String,Object> rechazar(Usuario actor, int id, String motivo) {
        Permisos.exigirAdmin(actor);
        String detalle = motivo == null ? "" : motivo.strip();
        var usuario = db.requerida("la cuenta", "SELECT id,nombre,email FROM usuarios WHERE id=? AND rol='OPERATIVO' FOR UPDATE", id);
        db.ejecutar("UPDATE usuarios SET estado_aprobacion='RECHAZADO', motivo_rechazo=? WHERE id=?", detalle.isBlank() ? null : detalle, id);
        db.ejecutar("DELETE FROM sesiones WHERE usuario_id=?", id);
        db.ejecutar("DELETE FROM tokens_cuenta WHERE usuario_id=?", id);
        notificaciones.paraUsuario(id, "Cuenta no aprobada", detalle.isBlank() ? "El administrador no aprobó tu solicitud de acceso. Contacta con la administración." : "Tu solicitud fue rechazada. Motivo: " + detalle, "CUENTA", "/acceso");
        try { correo.estadoCuenta(usuario.get("email").toString(), false, detalle); } catch (ErrorApi ignorado) { /* La notificación interna se conserva aunque el SMTP no responda. */ }
        db.auditar(actor, "RECHAZAR_USUARIO", "usuarios", id, Map.of("correo", usuario.get("email"), "motivo", detalle));
        return Map.of("mensaje", "Cuenta rechazada.");
    }
}
