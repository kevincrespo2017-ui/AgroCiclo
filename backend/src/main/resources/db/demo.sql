INSERT INTO terrenos(nombre,ubicacion,area_ha) VALUES
 ('San Joaquín','Cuenca, Azuay',4.2),
 ('Quingeo','Cuenca, Azuay',5.6),
 ('Jadán','Gualaceo, Azuay',3.8),
 ('El Paraíso','Ponce Enríquez, Azuay',6.4);

INSERT INTO cultivos(nombre,dias_estimados) VALUES
 ('Maíz',110), ('Papa',120), ('Fréjol',95), ('Cacao',160);

WITH reloj AS (SELECT (now() AT TIME ZONE 'America/Guayaquil')::date AS hoy),
datos(codigo, terreno, cultivo, area, inicio, siembra, cierre) AS (VALUES
 ('MAI-001','San Joaquín','Maíz',2.4,-225,-218,-102),
 ('PAP-001','Quingeo','Papa',1.8,-190,-185,-52),
 ('MAI-002','Jadán','Maíz',2.1,-118,-110,NULL),
 ('CAC-001','El Paraíso','Cacao',2.8,-162,-156,NULL),
 ('PAP-002','Quingeo','Papa',1.6,-65,-59,NULL),
 ('FRE-001','San Joaquín','Fréjol',0.9,-2,NULL,NULL),
 ('MAI-003','Jadán','Maíz',1.0,7,NULL,NULL)
)
INSERT INTO ciclos(codigo,terreno_id,cultivo_id,area_ha,estado,fecha_inicio,fecha_siembra,fecha_estimada,fecha_cierre,notas)
SELECT d.codigo,t.id,v.id,d.area,
       CASE WHEN d.cierre IS NOT NULL THEN 'CERRADO' WHEN d.siembra IS NOT NULL THEN 'SEMBRADO' ELSE 'PLANIFICADO' END,
       r.hoy+d.inicio,r.hoy+d.siembra,r.hoy+d.siembra+v.dias_estimados,r.hoy+d.cierre,
       'Registro ficticio para la demostración del caso 09.'
FROM datos d
JOIN terrenos t ON t.nombre=d.terreno
JOIN cultivos v ON v.nombre=d.cultivo
CROSS JOIN reloj r;

WITH reloj AS (SELECT (now() AT TIME ZONE 'America/Guayaquil')::date AS hoy),
datos(codigo,nombre,programada,obligatoria,completa,costo) AS (VALUES
 ('MAI-001','Preparación del suelo',-223,true,-222,138),
 ('MAI-001','Jornada de cosecha',-108,true,-106,185),
 ('PAP-001','Preparación del terreno',-189,true,-188,172),
 ('PAP-001','Clasificación de tubérculos',-62,true,-59,246),
 ('MAI-002','Control de malezas',-93,true,-92,115),
 ('MAI-002','Secado y clasificación',3,true,NULL,0),
 ('CAC-001','Inspección de frutos',-3,true,NULL,0),
 ('PAP-002','Aporque y revisión',-28,true,-28,94),
 ('PAP-002','Inspección foliar',5,true,NULL,0),
 ('FRE-001','Preparar surcos',1,true,NULL,0)
)
INSERT INTO actividades(ciclo_id,nombre,programada_para,obligatoria,completada_en,costo)
SELECT c.id,d.nombre,r.hoy+d.programada,d.obligatoria,r.hoy+d.completa,d.costo
FROM datos d JOIN ciclos c USING(codigo) CROSS JOIN reloj r;

WITH reloj AS (SELECT (now() AT TIME ZONE 'America/Guayaquil')::date AS hoy),
datos(codigo,folio,dia,kg) AS (VALUES
 ('MAI-001','COS-010',-107,6220), ('PAP-001','COS-020',-61,8560),
 ('PAP-001','COS-021',-58,1200), ('MAI-002','COS-031',-1,6790)
)
INSERT INTO cosechas(ciclo_id,folio,fecha,kilogramos)
SELECT c.id,d.folio,r.hoy+d.dia,d.kg FROM datos d JOIN ciclos c USING(codigo) CROSS JOIN reloj r;

WITH datos(nombre,categoria,unidad,minimo,inicial,precio) AS (VALUES
 ('Fertilizante NPK','FERTILIZANTE','KG',25,180,1.48),
 ('Compost maduro','FERTILIZANTE','KG',45,260,0.64),
 ('Biofungicida','FITOSANITARIO','L',8,24,6.40),
 ('Semilla certificada de frejol','SEMILLA','KG',10,32,3.80)
), nuevos AS (
 INSERT INTO insumos(nombre,categoria,unidad,stock,stock_minimo)
 SELECT nombre,categoria,unidad,inicial,minimo FROM datos RETURNING id,nombre
)
INSERT INTO movimientos_insumo(insumo_id,tipo,folio,fecha,cantidad,costo_unitario)
SELECT n.id,'ENTRADA','ENT-DEMO',(now() AT TIME ZONE 'America/Guayaquil')::date-238,d.inicial,d.precio
FROM nuevos n JOIN datos d USING(nombre);

WITH datos(insumo,codigo,dia,cantidad) AS (VALUES
 ('Fertilizante NPK','MAI-001',-195,74), ('Fertilizante NPK','MAI-002',-82,58),
 ('Fertilizante NPK','PAP-002',-35,32), ('Compost maduro','PAP-001',-164,105),
 ('Compost maduro','PAP-002',-38,61), ('Biofungicida','CAC-001',-53,12),
 ('Biofungicida','PAP-002',-21,8)
)
INSERT INTO movimientos_insumo(insumo_id,ciclo_id,tipo,folio,fecha,cantidad,costo_unitario)
SELECT i.id,c.id,'CONSUMO','APL-'||d.codigo,(now() AT TIME ZONE 'America/Guayaquil')::date+d.dia,d.cantidad,m.costo_unitario
FROM datos d JOIN insumos i ON i.nombre=d.insumo JOIN ciclos c USING(codigo)
JOIN movimientos_insumo m ON m.insumo_id=i.id AND m.tipo='ENTRADA' AND m.folio='ENT-DEMO';

UPDATE insumos i SET stock=i.stock-coalesce((
 SELECT sum(m.cantidad) FROM movimientos_insumo m WHERE m.tipo='CONSUMO' AND m.insumo_id=i.id
), 0)
WHERE EXISTS (
 SELECT 1 FROM movimientos_insumo m WHERE m.tipo='ENTRADA' AND m.folio='ENT-DEMO' AND m.insumo_id=i.id
);
