package ec.agrociclo.repository;

import ec.agrociclo.model.Usuario;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
/**
 * NotificacionesRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class NotificacionesRepository {
    private final BaseDatos db;
    public NotificacionesRepository(BaseDatos db) { this.db = db; }

    public List<Map<String,Object>> listar(Usuario usuario) {
        return db.filas("""
            SELECT id, titulo, mensaje, tipo, ruta, creada_en, leida_en
            FROM notificaciones
            WHERE usuario_id = ? OR (usuario_id IS NULL AND rol_destino = ?)
            ORDER BY creada_en DESC, id DESC
            """, usuario.getId(), usuario.getRol());
    }

    public int noLeidas(Usuario usuario) {
        var fila = db.una("""
            SELECT count(*)::integer AS total FROM notificaciones
            WHERE leida_en IS NULL AND (usuario_id = ? OR (usuario_id IS NULL AND rol_destino = ?))
            """, usuario.getId(), usuario.getRol());
        return BaseDatos.entero(fila, "total");
    }

    public int marcar(Usuario usuario, long id) {
        return db.ejecutar("""
            UPDATE notificaciones SET leida_en = now()
            WHERE id = ? AND leida_en IS NULL
              AND (usuario_id = ? OR (usuario_id IS NULL AND rol_destino = ?))
            """, id, usuario.getId(), usuario.getRol());
    }

    public int marcarTodas(Usuario usuario) {
        return db.ejecutar("""
            UPDATE notificaciones SET leida_en = now()
            WHERE leida_en IS NULL AND (usuario_id = ? OR (usuario_id IS NULL AND rol_destino = ?))
            """, usuario.getId(), usuario.getRol());
    }

    public void paraUsuario(int id, String titulo, String mensaje, String tipo, String ruta) {
        db.ejecutar("""
            INSERT INTO notificaciones(usuario_id, titulo, mensaje, tipo, ruta)
            VALUES (?, ?, ?, ?, ?)
            """, id, titulo, mensaje, tipo, ruta);
    }

    public void paraRol(String rol, String titulo, String mensaje, String tipo, String ruta) {
        db.ejecutar("""
            INSERT INTO notificaciones(rol_destino, titulo, mensaje, tipo, ruta)
            VALUES (?, ?, ?, ?, ?)
            """, rol, titulo, mensaje, tipo, ruta);
    }
}
