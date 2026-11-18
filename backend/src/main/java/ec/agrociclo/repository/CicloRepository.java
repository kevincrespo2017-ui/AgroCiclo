package ec.agrociclo.repository;
import ec.agrociclo.model.Ciclo;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * CicloRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class CicloRepository {
    private final BaseDatos db;
    public CicloRepository(BaseDatos db) { this.db=db; }
    public Ciclo buscarPorId(int id) {
        return MapeadorModelos.convertir(db.requerida("el registro", "SELECT * FROM ciclos WHERE id=?", id),Ciclo.class);
    }
    public List<Ciclo> listar() {
        return db.filas("SELECT * FROM ciclos ORDER BY id").stream().map(f->MapeadorModelos.convertir(f,Ciclo.class)).toList();
    }
}
