package ec.agrociclo.repository;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
/**
 * CuentasRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class CuentasRepository {
    private final BaseDatos db;
    public CuentasRepository(BaseDatos db) { this.db=db; }
    public Map<String,Object> registrarConsultarUsuarios(Object... valores) {
        return db.una("INSERT INTO usuarios(nombre,email,password_hash,rol,estado_aprobacion,verificado) VALUES (?,?,?,'OPERATIVO','PENDIENTE',false) ON CONFLICT(email) DO NOTHING RETURNING id", valores);
    }
    public Map<String,Object> solicitarConsultarUsuarios(Object... valores) {
        return db.una("SELECT * FROM usuarios WHERE email=? FOR UPDATE", valores);
    }
    public Map<String,Object> solicitarConsultarTokensCuenta(Object... valores) {
        return db.una("SELECT token_hash FROM tokens_cuenta WHERE usuario_id=? AND tipo=? AND creado_en>now()-interval '60 seconds'", valores);
    }
    public int emitirActualizarTokensCuenta(Object... valores) {
        return db.ejecutar("DELETE FROM tokens_cuenta WHERE usuario_id=? AND tipo=?", valores);
    }
    public int emitirActualizarTokensCuenta2(Object... valores) {
        return db.ejecutar("INSERT INTO tokens_cuenta(token_hash,usuario_id,tipo,expira_en) VALUES (?,?,?,now()+interval '30 minutes')", valores);
    }
    public int emitirCodigoActualizarTokensCuenta(Object... valores) {
        return db.ejecutar("INSERT INTO tokens_cuenta(token_hash,usuario_id,tipo,expira_en) VALUES (?,?,?,now()+interval '10 minutes')", valores);
    }
    public Map<String,Object> consumirConsultarUsuarios(Object... valores) {
        return db.una("SELECT u.* FROM usuarios u JOIN tokens_cuenta t ON t.usuario_id=u.id WHERE t.token_hash=? FOR UPDATE OF u", valores);
    }
    public Map<String,Object> consumirConsultarTokensCuenta(Object... valores) {
        return db.una("DELETE FROM tokens_cuenta WHERE token_hash=? AND tipo=? AND expira_en>now() RETURNING usuario_id", valores);
    }
    public Map<String,Object> consumirConsultarCodigoCuenta(Object... valores) {
        return db.una("SELECT token_hash FROM tokens_cuenta WHERE usuario_id=? AND tipo=? AND expira_en>now() ORDER BY creado_en DESC LIMIT 1 FOR UPDATE", valores);
    }
    public int consumirEliminarTokensCuenta(Object... valores) {
        return db.ejecutar("DELETE FROM tokens_cuenta WHERE usuario_id=? AND tipo=?", valores);
    }
    public int consumirActualizarUsuarios(Object... valores) {
        return db.ejecutar("UPDATE usuarios SET verificado=true WHERE id=?", valores);
    }
    public int consumirActualizarUsuarios2(Object... valores) {
        return db.ejecutar("UPDATE usuarios SET password_hash=? WHERE id=?", valores);
    }
    public int consumirActualizarSesiones(Object... valores) {
        return db.ejecutar("DELETE FROM sesiones WHERE usuario_id=?", valores);
    }
}
