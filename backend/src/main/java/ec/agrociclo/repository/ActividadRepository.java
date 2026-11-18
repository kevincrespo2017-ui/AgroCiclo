package ec.agrociclo.repository;
import ec.agrociclo.model.Actividad;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * ActividadRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class ActividadRepository {
    private final BaseDatos db;
    public ActividadRepository(BaseDatos db) { this.db=db; }
    public Actividad buscarPorId(int id) {
        return MapeadorModelos.convertir(db.requerida("el registro", "SELECT * FROM actividades WHERE id=?", id),Actividad.class);
    }
    public List<Actividad> listar() {
        return db.filas("SELECT * FROM actividades ORDER BY programada_para, id").stream().map(f->MapeadorModelos.convertir(f,Actividad.class)).toList();
    }
    public List<Actividad> buscarPorCiclo(int cicloId) {
        return db.filas("SELECT * FROM actividades WHERE ciclo_id=? ORDER BY programada_para, id",cicloId).stream()
            .map(f->MapeadorModelos.convertir(f,Actividad.class)).toList();
    }
}
