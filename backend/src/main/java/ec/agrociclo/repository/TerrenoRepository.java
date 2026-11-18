package ec.agrociclo.repository;
import ec.agrociclo.model.Terreno;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * TerrenoRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class TerrenoRepository {
    private final BaseDatos db;
    public TerrenoRepository(BaseDatos db) { this.db=db; }
    public Terreno buscarPorId(int id) {
        return MapeadorModelos.convertir(db.requerida("el registro", "SELECT * FROM terrenos WHERE id=?", id),Terreno.class);
    }
    public List<Terreno> listar() {
        return db.filas("SELECT * FROM terrenos ORDER BY nombre, id").stream().map(f->MapeadorModelos.convertir(f,Terreno.class)).toList();
    }
}
