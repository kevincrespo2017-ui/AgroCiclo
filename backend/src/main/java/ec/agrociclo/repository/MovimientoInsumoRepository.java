package ec.agrociclo.repository;
import ec.agrociclo.model.MovimientoInsumo;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * MovimientoInsumoRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class MovimientoInsumoRepository {
    private final BaseDatos db;
    public MovimientoInsumoRepository(BaseDatos db) { this.db=db; }
    public MovimientoInsumo buscarPorId(int id) {
        return MapeadorModelos.convertir(db.requerida("el registro", "SELECT * FROM movimientos_insumo WHERE id=?", id),MovimientoInsumo.class);
    }
    public List<MovimientoInsumo> listar() {
        return db.filas("SELECT * FROM movimientos_insumo ORDER BY id").stream().map(f->MapeadorModelos.convertir(f,MovimientoInsumo.class)).toList();
    }
}
