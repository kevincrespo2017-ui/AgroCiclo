package ec.agrociclo.repository;

import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.dto.Pagina;
import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.validation.Validaciones;
import ec.agrociclo.model.Usuario;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Repository
/**
 * ReportesRepository: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public class ReportesRepository {
    private final BaseDatos db;
    private static final String ALERTAS = """
        WITH fecha_ref AS (SELECT cast(? as date) AS hoy),
        alertas AS (
            SELECT 'ACTIVIDAD' AS tipo, a.id AS origen_id, c.id AS ciclo_id, NULL::integer AS insumo_id,
                   a.nombre AS titulo, c.codigo || ' · ' || v.nombre AS referencia, a.programada_para AS vence,
                   CASE WHEN a.programada_para < f.hoy THEN 'VENCIDA' ELSE 'PROXIMA' END AS nivel
            FROM actividades a JOIN ciclos c ON c.id = a.ciclo_id JOIN cultivos v ON v.id = c.cultivo_id
            CROSS JOIN fecha_ref f
            WHERE a.obligatoria AND a.completada_en IS NULL AND c.estado <> 'CERRADO' AND a.programada_para <= f.hoy + 7
            UNION ALL
            SELECT 'COSECHA', c.id, c.id, NULL, 'Revisar cosecha', c.codigo || ' · ' || v.nombre, c.fecha_estimada,
                   CASE WHEN c.fecha_estimada < f.hoy THEN 'VENCIDA' ELSE 'PROXIMA' END
            FROM ciclos c JOIN cultivos v ON v.id = c.cultivo_id CROSS JOIN fecha_ref f
            WHERE c.estado = 'SEMBRADO' AND c.fecha_estimada <= f.hoy + 7
              AND NOT EXISTS (SELECT 1 FROM cosechas h WHERE h.ciclo_id = c.id)
            UNION ALL
            SELECT 'INVENTARIO', i.id, NULL, i.id, i.nombre,
                   'Disponible ' || trim_scale(i.stock) || ' ' || i.unidad ||
                   ' · mínimo ' || trim_scale(i.stock_minimo) || ' ' || i.unidad, NULL, 'BAJO_STOCK'
            FROM insumos i WHERE i.stock <= i.stock_minimo
        )
        SELECT * FROM alertas
        ORDER BY CASE nivel WHEN 'VENCIDA' THEN 0 WHEN 'BAJO_STOCK' THEN 1 ELSE 2 END,
                 vence NULLS LAST, tipo, origen_id
        """;

    public ReportesRepository(BaseDatos db) { this.db = db; }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> indicadores() {
        var resumen = db.una("""
            SELECT count(*)::integer AS ciclos,
              count(*) FILTER (WHERE estado = 'SEMBRADO')::integer AS sembrados,
              count(*) FILTER (WHERE estado = 'PLANIFICADO')::integer AS planificados,
              count(*) FILTER (WHERE estado = 'CERRADO')::integer AS cerrados,
              coalesce(sum(kg_producidos), 0) AS produccion_kg,
              coalesce(sum(costo_total), 0) AS costo_usd,
              round(sum(kg_producidos) / nullif(sum(area_ha) FILTER (WHERE kg_producidos > 0), 0), 2) AS rendimiento_kg_ha
            FROM resumen_ciclos
            """);
        var cultivos = db.filas("""
            SELECT cultivo_id, cultivo, count(*)::integer AS ciclos,
              sum(kg_producidos) AS produccion_kg, sum(costo_total) AS costo_usd,
              round(sum(kg_producidos) / nullif(sum(area_ha) FILTER (WHERE kg_producidos > 0), 0), 2) AS rendimiento_kg_ha
            FROM resumen_ciclos GROUP BY cultivo_id, cultivo ORDER BY produccion_kg DESC, cultivo
            """);
        return Map.of("resumen", resumen, "cultivos", cultivos);
    }

    public List<Map<String, Object>> alertas() { return db.filas(ALERTAS, Validaciones.hoy()); }


    /**
     * Obtiene las filas del reporte solicitado. El tipo se resuelve mediante una lista cerrada
     * de consultas para evitar que un parámetro del usuario se convierta en SQL dinámico.
     */
    public List<Map<String,Object>> reporteGeneral(String tipo) {
        return switch (tipo) {
            case "produccion" -> db.filas("""
                SELECT codigo, cultivo, terreno, estado, fecha_inicio, fecha_siembra,
                       fecha_estimada, kg_producidos, rendimiento_kg_ha, costo_total
                FROM resumen_ciclos ORDER BY fecha_inicio DESC, codigo
                """);
            case "terrenos" -> db.filas("""
                SELECT nombre, ubicacion, area_ha FROM terrenos ORDER BY nombre
                """);
            case "insumos" -> db.filas("""
                SELECT nombre, categoria, stock, unidad, stock_minimo
                FROM insumos ORDER BY nombre
                """);
            case "actividades" -> db.filas("""
                SELECT a.nombre AS actividad, c.codigo AS ciclo, t.nombre AS terreno,
                       a.programada_para, a.obligatoria, a.completada_en, a.costo
                FROM actividades a JOIN ciclos c ON c.id=a.ciclo_id
                JOIN terrenos t ON t.id=c.terreno_id
                ORDER BY a.programada_para DESC, c.codigo, a.nombre
                """);
            default -> throw ErrorApi.campo("tipo", "Selecciona un tipo de reporte válido.");
        };
    }

    public Pagina auditoria(Usuario actor, Map<String, String> consulta) {

        Validaciones.parametros(consulta, Set.of("q", "desde", "pagina", "tam"));
        int pagina = Validaciones.enteroConsulta(consulta, "pagina", 1, Integer.MAX_VALUE);
        int tam = Validaciones.enteroConsulta(consulta, "tam", 10, 50);
        String q = consulta.getOrDefault("q", "").strip();
        if (q.length() > 100) throw ErrorApi.campo("q", "Usa hasta 100 caracteres.");
        var desde = Validaciones.fechaFiltro(consulta, "desde");
        var filas = db.pagina("""
            SELECT a.id, a.usuario_id, a.fecha_hora, a.accion, a.entidad, a.registro_id,
                   a.detalle::text AS detalle, u.nombre AS usuario, u.email
            FROM auditoria a JOIN usuarios u ON u.id = a.usuario_id
            WHERE strpos(lower(concat_ws(' ', u.nombre, a.accion, a.entidad)), lower(?)) > 0
              AND (cast(? as date) IS NULL OR a.fecha_hora >= (cast(? as timestamp) AT TIME ZONE 'America/Guayaquil'))
            """, BaseDatos.parametros(q, desde, desde), "fecha_hora DESC, id DESC", pagina, tam);
        filas.datos().forEach(db::leerDetalleAuditoria);
        return filas;
    }
}
