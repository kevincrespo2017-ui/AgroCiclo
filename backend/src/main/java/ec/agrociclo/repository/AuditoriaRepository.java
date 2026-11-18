package ec.agrociclo.repository;
import ec.agrociclo.model.Auditoria;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * AuditoriaRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class AuditoriaRepository {
    private final BaseDatos db;
    public AuditoriaRepository(BaseDatos db) { this.db=db; }
    public Auditoria buscarPorId(int id) {
        var fila=db.requerida("el registro", "SELECT id, usuario_id, fecha_hora, accion, entidad, registro_id, detalle::text AS detalle FROM auditoria WHERE id=?", id);
        db.leerDetalleAuditoria(fila);
        return MapeadorModelos.convertir(fila,Auditoria.class);
    }
    public List<Auditoria> listar() {
        return db.filas("SELECT id, usuario_id, fecha_hora, accion, entidad, registro_id, detalle::text AS detalle FROM auditoria ORDER BY id").stream().map(f->{ db.leerDetalleAuditoria(f); return MapeadorModelos.convertir(f,Auditoria.class); }).toList();
    }
}
