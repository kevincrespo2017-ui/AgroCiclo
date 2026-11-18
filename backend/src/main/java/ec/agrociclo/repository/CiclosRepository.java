package ec.agrociclo.repository;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
/**
 * CiclosRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class CiclosRepository {
    private final BaseDatos db;
    public CiclosRepository(BaseDatos db) { this.db=db; }
    public List<Map<String,Object>> detalleListarMovimientosInsumo(Object... valores) {
        return db.filas("""
                SELECT m.*, i.nombre AS insumo, i.unidad, round(m.cantidad * m.costo_unitario, 2) AS costo
                FROM movimientos_insumo m JOIN insumos i ON i.id = m.insumo_id
                WHERE m.ciclo_id = ? ORDER BY m.fecha DESC, m.id DESC
                """, valores);
    }
    public Map<String,Object> crearConsultarCiclos(Object... valores) {
        return db.una("""
                INSERT INTO ciclos(codigo, terreno_id, cultivo_id, area_ha, fecha_inicio, notas)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING *
                """, valores);
    }
    public Map<String,Object> editarConsultarActividades(Object... valores) {
        return db.una("""
                SELECT id FROM actividades WHERE ciclo_id = ?
                AND (programada_para < ? OR completada_en < ?) LIMIT 1
                """, valores);
    }
    public Map<String,Object> editarConsultarActividades2(Object... valores) {
        return db.una("SELECT id FROM actividades WHERE ciclo_id = ? AND completada_en IS NOT NULL LIMIT 1", valores);
    }
    public Map<String,Object> editarConsultarCiclos(Object... valores) {
        return db.una("""
                UPDATE ciclos SET codigo = ?, terreno_id = ?, cultivo_id = ?, area_ha = ?, fecha_inicio = ?, notas = ?
                WHERE id = ? RETURNING *
                """, valores);
    }
    public Map<String,Object> sembrarConsultarCiclos(Object... valores) {
        return db.una("""
                UPDATE ciclos c SET estado = 'SEMBRADO', fecha_siembra = ?, fecha_estimada = cast(? as date) + v.dias_estimados
                FROM cultivos v WHERE c.id = ? AND v.id = c.cultivo_id RETURNING c.*
                """, valores);
    }
    public Map<String,Object> cerrarConsultarActividades(Object... valores) {
        return db.una("SELECT id FROM actividades WHERE ciclo_id = ? AND obligatoria AND completada_en IS NULL LIMIT 1", valores);
    }
    public Map<String,Object> cerrarConsultarActividades2(Object... valores) {
        return db.una("""
                SELECT greatest(
                  (SELECT max(completada_en) FROM actividades WHERE ciclo_id = ?),
                  (SELECT max(fecha) FROM cosechas WHERE ciclo_id = ?),
                  (SELECT max(fecha) FROM movimientos_insumo WHERE ciclo_id = ?)
                ) AS fecha
                """, valores);
    }
    public Map<String,Object> cerrarConsultarCiclos(Object... valores) {
        return db.una("UPDATE ciclos SET estado = 'CERRADO', fecha_cierre = ? WHERE id = ? RETURNING *", valores);
    }
    public int eliminarActualizarCiclos(Object... valores) {
        return db.ejecutar("DELETE FROM ciclos WHERE id = ?", valores);
    }
    public Map<String,Object> validarCatalogosRequerirTerrenos(Object... valores) {
        return db.requerida("el terreno", "SELECT area_ha FROM terrenos WHERE id = ? FOR UPDATE", valores);
    }
    public Map<String,Object> validarCatalogosRequerirCultivos(Object... valores) {
        return db.requerida("el cultivo", "SELECT id FROM cultivos WHERE id = ?", valores);
    }
}
