package ec.agrociclo.repository;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
/**
 * AutenticacionRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class AutenticacionRepository {
    private final BaseDatos db;
    public AutenticacionRepository(BaseDatos db) { this.db=db; }
    public Map<String,Object> entrarConsultarUsuarios(Object... valores) {
        return db.una("SELECT id, nombre, email, rol, password_hash, verificado, estado_aprobacion FROM usuarios WHERE email = ?", valores);
    }
    public int entrarActualizarSesiones(Object... valores) {
        return db.ejecutar("DELETE FROM sesiones WHERE usuario_id = ? AND expira_en <= now()", valores);
    }
    public int entrarActualizarSesiones2(Object... valores) {
        return db.ejecutar("""
                INSERT INTO sesiones(token_hash, usuario_id, expira_en)
                VALUES (?, ?, now() + interval '8 hours')
                """, valores);
    }
    public Map<String,Object> deCabeceraConsultarSesiones(Object... valores) {
        return db.una("""
                SELECT u.id, u.nombre, u.email, u.rol
                FROM sesiones s JOIN usuarios u ON u.id = s.usuario_id
                WHERE s.token_hash = ? AND s.expira_en > now()
                """, valores);
    }
    public int salirActualizarSesiones(Object... valores) {
        return db.ejecutar("DELETE FROM sesiones WHERE token_hash = ?", valores);
    }
}
