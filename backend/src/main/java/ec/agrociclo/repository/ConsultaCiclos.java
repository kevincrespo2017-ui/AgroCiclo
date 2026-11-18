package ec.agrociclo.repository;

import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.dto.Pagina;
import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.validation.Validaciones;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
/**
 * ConsultaCiclos: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class ConsultaCiclos {
    private static final Map<String, String> ORDEN = Map.of(
        "fechaInicio", "fecha_inicio", "codigo", "codigo", "rendimiento", "rendimiento_kg_ha", "costo", "costo_total");
    private static final Set<String> ESTADOS = Set.of("PLANIFICADO", "SEMBRADO", "CERRADO");
    private static final Set<String> CAMPOS = Set.of("q", "estado", "cultivoId", "terrenoId",
        "desde", "hasta", "ordenar", "direccion", "pagina", "tam");

    private final BaseDatos db;

    public ConsultaCiclos(BaseDatos db) { this.db = db; }

    public Pagina buscar(Map<String, String> consulta) {
        Validaciones.parametros(consulta, CAMPOS);
        int pagina = Validaciones.enteroConsulta(consulta, "pagina", 1, Integer.MAX_VALUE);
        int tam = Validaciones.enteroConsulta(consulta, "tam", 10, 50);
        String q = consulta.getOrDefault("q", "").strip();
        if (q.length() > 100) throw ErrorApi.campo("q", "Usa hasta 100 caracteres.");
        String estado = consulta.get("estado");
        if (estado != null && !ESTADOS.contains(estado)) throw ErrorApi.campo("estado", "Estado inválido.");
        int cultivo = Validaciones.enteroConsulta(consulta, "cultivoId", 0, Integer.MAX_VALUE);
        int terreno = Validaciones.enteroConsulta(consulta, "terrenoId", 0, Integer.MAX_VALUE);
        LocalDate desde = Validaciones.fechaFiltro(consulta, "desde");
        LocalDate hasta = Validaciones.fechaFiltro(consulta, "hasta");
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw ErrorApi.campo("hasta", "La fecha final debe ser posterior o igual a la inicial.");
        }
        String columna = ORDEN.get(consulta.getOrDefault("ordenar", "fechaInicio"));
        if (columna == null) throw ErrorApi.campo("ordenar", "Opción de orden inválida.");
        String direccion = consulta.getOrDefault("direccion", "desc");
        if (!"asc".equals(direccion) && !"desc".equals(direccion)) {
            throw ErrorApi.campo("direccion", "Usa asc o desc.");
        }

        var condiciones = new ArrayList<String>();
        var argumentos = new ArrayList<Object>();
        if (!q.isEmpty()) agregar(condiciones, argumentos, "strpos(lower(concat_ws(' ', codigo, terreno, cultivo)), lower(?)) > 0", q);
        if (estado != null) agregar(condiciones, argumentos, "estado = ?", estado);
        if (cultivo > 0) agregar(condiciones, argumentos, "cultivo_id = ?", cultivo);
        if (terreno > 0) agregar(condiciones, argumentos, "terreno_id = ?", terreno);
        if (desde != null) agregar(condiciones, argumentos, "fecha_inicio >= ?", desde);
        if (hasta != null) agregar(condiciones, argumentos, "fecha_inicio <= ?", hasta);

        var seleccion = "SELECT * FROM resumen_ciclos";
        if (!condiciones.isEmpty()) seleccion += " WHERE " + String.join(" AND ", condiciones);
        return db.pagina(seleccion, argumentos, columna + " " + direccion.toUpperCase(Locale.ROOT) + " NULLS LAST, id DESC", pagina, tam);
    }

    public Map<String, Object> resumen(int id) {
        return db.requerida("el ciclo", "SELECT * FROM resumen_ciclos WHERE id = ?", Validaciones.id(id));
    }

    public Map<String, Object> bloquear(int id) {
        return db.requerida("el ciclo", "SELECT * FROM ciclos WHERE id = ? FOR UPDATE", Validaciones.id(id));
    }

    private static void agregar(List<String> condiciones, List<Object> argumentos, String condicion, Object valor) {
        condiciones.add(condicion);
        argumentos.add(valor);
    }
}
