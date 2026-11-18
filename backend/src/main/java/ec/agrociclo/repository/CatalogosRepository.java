package ec.agrociclo.repository;
import ec.agrociclo.model.TipoCatalogo;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.stereotype.Repository;
@Repository
/**
 * CatalogosRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class CatalogosRepository {
    private final BaseDatos db;
    public CatalogosRepository(BaseDatos db) { this.db=db; }
    public Map<String,Object> crear(TipoCatalogo tipo, Map<String,Object> datos) {
        return db.una("INSERT INTO "+tipo.tabla()+" ("+String.join(", ",tipo.columnas())+") VALUES ("+
            String.join(", ",Collections.nCopies(tipo.columnas().size(),"?"))+") RETURNING *",
            tipo.columnas().stream().map(datos::get).toArray());
    }
    public Map<String,Object> bloquear(TipoCatalogo tipo,int id) {
        return db.requerida("el registro del catálogo","SELECT * FROM "+tipo.tabla()+" WHERE id=? FOR UPDATE",id);
    }
    public Map<String,Object> areaMaxima(int id) { return db.una("SELECT max(area_ha) AS area FROM ciclos WHERE terreno_id=?",id); }
    public boolean tieneMovimientos(int id) { return db.una("SELECT id FROM movimientos_insumo WHERE insumo_id=? LIMIT 1",id)!=null; }
    public Map<String,Object> editar(TipoCatalogo tipo,int id,Map<String,Object> datos) {
        var valores=new ArrayList<Object>(tipo.columnas().stream().map(datos::get).toList());valores.add(id);
        var asignaciones=String.join(", ",tipo.columnas().stream().map(c->c+"=?").toList());
        return db.una("UPDATE "+tipo.tabla()+" SET "+asignaciones+" WHERE id=? RETURNING *",valores.toArray());
    }
    public Map<String,Object> actualizarImagenTerreno(int id, String imagenUrl) {
        return db.una("UPDATE terrenos SET imagen_url=? WHERE id=? RETURNING *", imagenUrl, id);
    }
    public Map<String,Object> borrar(TipoCatalogo tipo,int id) {
        return db.requerida("el registro del catálogo","DELETE FROM "+tipo.tabla()+" WHERE id=? RETURNING nombre",id);
    }
}
