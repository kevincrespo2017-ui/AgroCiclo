package ec.agrociclo.repository;
import ec.agrociclo.model.Insumo;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * InsumoRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class InsumoRepository {
    private final BaseDatos db;
    public InsumoRepository(BaseDatos db) { this.db=db; }
    public Insumo buscarPorId(int id) {
        return MapeadorModelos.convertir(db.requerida("el registro", "SELECT * FROM insumos WHERE id=?", id),Insumo.class);
    }
    public List<Insumo> listar() {
        return db.filas("SELECT * FROM insumos ORDER BY nombre, id").stream().map(f->MapeadorModelos.convertir(f,Insumo.class)).toList();
    }
}
