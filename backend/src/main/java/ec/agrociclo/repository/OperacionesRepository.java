package ec.agrociclo.repository;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * OperacionesRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class OperacionesRepository {
    private final BaseDatos db;
    public OperacionesRepository(BaseDatos db) { this.db=db; }
    public Map<String,Object> programarConsultarActividades(Object... valores) {
        return db.una("""
                INSERT INTO actividades(ciclo_id, nombre, programada_para, obligatoria, nota)
                VALUES (?, ?, ?, ?, ?) RETURNING *
                """, valores);
    }
    public Map<String,Object> editarActividadConsultarActividades(Object... valores) {
        return db.una("""
                UPDATE actividades SET nombre = ?, programada_para = ?, obligatoria = ?, nota = ?
                WHERE id = ? RETURNING *
                """, valores);
    }
    public Map<String,Object> completarConsultarActividades(Object... valores) {
        return db.una("UPDATE actividades SET completada_en = ?, costo = ? WHERE id = ? RETURNING *", valores);
    }
    public int borrarActividadActualizarActividades(Object... valores) {
        return db.ejecutar("DELETE FROM actividades WHERE id = ?", valores);
    }
    public Map<String,Object> cosecharConsultarCosechas(Object... valores) {
        return db.una("""
                INSERT INTO cosechas(ciclo_id, folio, fecha, kilogramos, nota)
                VALUES (?, ?, ?, ?, ?) RETURNING *
                """, valores);
    }
    public Map<String,Object> retirarCosechaRequerirCosechas(Object... valores) {
        return db.requerida("la cosecha", "DELETE FROM cosechas WHERE ciclo_id = ? AND id = ? RETURNING *", valores);
    }
    public Map<String,Object> movimientosRequerirInsumos(Object... valores) {
        return db.requerida("el insumo", "SELECT id FROM insumos WHERE id = ?", valores);
    }
    public Map<String,Object> moverRequerirInsumos(Object... valores) {
        return db.requerida("el insumo", "SELECT * FROM insumos WHERE id = ? FOR UPDATE", valores);
    }
    public Map<String,Object> moverConsultarMovimientosInsumo(Object... valores) {
        return db.una("SELECT id FROM movimientos_insumo WHERE insumo_id = ? AND tipo = ? AND folio = ?", valores);
    }
    public Map<String,Object> moverConsultarInsumos(Object... valores) {
        return db.una("UPDATE insumos SET stock = ? WHERE id = ? RETURNING stock", valores);
    }
    public Map<String,Object> moverConsultarMovimientosInsumo2(Object... valores) {
        return db.una("""
                INSERT INTO movimientos_insumo(insumo_id, tipo, ciclo_id, folio, fecha, cantidad, costo_unitario)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *
                """, valores);
    }
    public Map<String,Object> actividadPendienteRequerirActividades(Object... valores) {
        return db.requerida("la actividad", "SELECT * FROM actividades WHERE ciclo_id = ? AND id = ?", valores);
    }
    public ec.agrociclo.dto.Pagina movimientosPaginados(int id,int pagina,int tam) {
        return db.pagina("""
                SELECT m.*, c.codigo AS ciclo FROM movimientos_insumo m
                LEFT JOIN ciclos c ON c.id = m.ciclo_id WHERE m.insumo_id = ?
                """, List.of(id), "fecha DESC, id DESC", pagina, tam);
    }
}
