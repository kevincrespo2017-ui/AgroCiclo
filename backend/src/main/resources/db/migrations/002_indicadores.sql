CREATE VIEW resumen_ciclos AS
WITH produccion AS (
    SELECT ciclo_id, sum(kilogramos) AS kg
    FROM cosechas
    GROUP BY ciclo_id
), actividad AS (
    SELECT ciclo_id, sum(costo) AS costo,
           count(*) FILTER (WHERE obligatoria AND completada_en IS NULL)::integer AS pendientes
    FROM actividades
    GROUP BY ciclo_id
), insumo AS (
    SELECT ciclo_id, sum(round(cantidad * costo_unitario, 2)) AS costo
    FROM movimientos_insumo
    WHERE tipo = 'CONSUMO'
    GROUP BY ciclo_id
)
SELECT c.*, t.nombre AS terreno, v.nombre AS cultivo,
       coalesce(p.kg, 0) AS kg_producidos,
       coalesce(a.costo, 0) AS costo_actividades,
       coalesce(i.costo, 0) AS costo_insumos,
       coalesce(a.costo, 0) + coalesce(i.costo, 0) AS costo_total,
       coalesce(a.pendientes, 0) AS obligatorias_pendientes,
       CASE WHEN p.kg > 0 THEN round(p.kg / c.area_ha, 2) END AS rendimiento_kg_ha
FROM ciclos c
JOIN terrenos t ON t.id = c.terreno_id
JOIN cultivos v ON v.id = c.cultivo_id
LEFT JOIN produccion p ON p.ciclo_id = c.id
LEFT JOIN actividad a ON a.ciclo_id = c.id
LEFT JOIN insumo i ON i.ciclo_id = c.id;
