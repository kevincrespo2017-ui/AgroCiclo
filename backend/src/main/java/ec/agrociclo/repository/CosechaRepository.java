package ec.agrociclo.repository;
import ec.agrociclo.model.Cosecha;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * CosechaRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class CosechaRepository {
    private final BaseDatos db;
    public CosechaRepository(BaseDatos db) { this.db=db; }
    public Cosecha buscarPorId(int id) {
        return MapeadorModelos.convertir(db.requerida("el registro", "SELECT * FROM cosechas WHERE id=?", id),Cosecha.class);
    }
    public List<Cosecha> listar() {
        return db.filas("SELECT * FROM cosechas ORDER BY fecha DESC, id DESC").stream().map(f->MapeadorModelos.convertir(f,Cosecha.class)).toList();
    }
    public List<Cosecha> buscarPorCiclo(int cicloId) {
        return db.filas("SELECT * FROM cosechas WHERE ciclo_id=? ORDER BY fecha DESC, id DESC",cicloId).stream()
            .map(f->MapeadorModelos.convertir(f,Cosecha.class)).toList();
    }
}
