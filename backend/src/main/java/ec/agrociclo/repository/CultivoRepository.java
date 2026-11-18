package ec.agrociclo.repository;
import ec.agrociclo.model.Cultivo;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * CultivoRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class CultivoRepository {
    private final BaseDatos db;
    public CultivoRepository(BaseDatos db) { this.db=db; }
    public Cultivo buscarPorId(int id) {
        return MapeadorModelos.convertir(db.requerida("el registro", "SELECT * FROM cultivos WHERE id=?", id),Cultivo.class);
    }
    public List<Cultivo> listar() {
        return db.filas("SELECT * FROM cultivos ORDER BY nombre, id").stream().map(f->MapeadorModelos.convertir(f,Cultivo.class)).toList();
    }
}
