package ec.agrociclo.repository;

import ec.agrociclo.dto.Pagina;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.model.Usuario;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
/**
 * BaseDatos: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class BaseDatos {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public BaseDatos(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Map<String, Object>> filas(String sql, Object... argumentos) {
        return jdbc.queryForList(sql, argumentos).stream().map(BaseDatos::convertir).toList();
    }

    public Map<String, Object> una(String sql, Object... argumentos) {
        var filas = filas(sql, argumentos);
        return filas.isEmpty() ? null : filas.get(0);
    }

    public Map<String, Object> requerida(String sujeto, String sql, Object... argumentos) {
        var fila = una(sql, argumentos);
        if (fila == null) throw ErrorApi.noEncontrado(sujeto);
        return fila;
    }

    public int ejecutar(String sql, Object... argumentos) {
        return jdbc.update(sql, argumentos);
    }

    public void instruccion(String sql) {
        jdbc.execute(sql);
    }

    // La selección y el orden proceden de consultas internas. Los datos de la petición se parametrizan.
    public Pagina pagina(String seleccion, List<?> parametros, String orden, int pagina, int tam) {
        var argumentos = new ArrayList<Object>(parametros);
        argumentos.add(tam);
        argumentos.add((long) (pagina - 1) * tam);
        var filas = filas("""
                WITH filtrados AS MATERIALIZED (%s),
                conteo AS (SELECT count(*)::integer AS total FROM filtrados)
                SELECT pagina.*, conteo.total AS total_filas FROM conteo
                LEFT JOIN LATERAL (
                    SELECT * FROM filtrados ORDER BY %s LIMIT ? OFFSET ?
                ) pagina ON true
                ORDER BY %s
                """.formatted(seleccion, orden, orden), argumentos.toArray());
        int total = entero(filas.get(0), "total_filas");
        var datos = filas.stream().filter(fila -> fila.get("id") != null).toList();
        datos.forEach(fila -> fila.remove("total_filas"));
        return new Pagina(datos, pagina, tam, total);
    }

    public void auditar(Usuario actor, String accion, String entidad, int id, Map<String, ?> detalle) {
        try {
            ejecutar("""
                    INSERT INTO auditoria(usuario_id, accion, entidad, registro_id, detalle)
                    VALUES (?, ?, ?, ?, cast(? as jsonb))
                    """, actor.getId(), accion, entidad, id, json.writeValueAsString(detalle));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo registrar la auditoría.", error);
        }
    }

    public void leerDetalleAuditoria(Map<String, Object> fila) {
        try {
            fila.put("detalle", json.readTree(fila.get("detalle").toString()));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Detalle de auditoría inválido.", error);
        }
    }

    private static Map<String, Object> convertir(Map<String, Object> fila) {
        var copia = new LinkedHashMap<String, Object>();
        fila.forEach((llave, valor) -> {
            Object convertido = valor;
            if (valor instanceof java.sql.Date fecha) {
                convertido = fecha.toLocalDate();
            } else if (valor instanceof Timestamp tiempo) {
                convertido = tiempo.toInstant();
            } else if (valor instanceof BigDecimal numero) {
                convertido = numero.toPlainString();
            }
            copia.put(llave, convertido);
        });
        return copia;
    }

    public static int entero(Map<String, Object> fila, String llave) {
        return ((Number) fila.get(llave)).intValue();
    }

    public static BigDecimal decimal(Map<String, Object> fila, String llave) {
        return new BigDecimal(fila.get(llave).toString());
    }

    public static LocalDate fecha(Map<String, Object> fila, String llave) {
        return (LocalDate) fila.get(llave);
    }

    public static List<Object> parametros(Object... valores) {
        return Arrays.asList(valores);
    }

}
