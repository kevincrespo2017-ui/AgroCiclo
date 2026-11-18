package ec.agrociclo.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * ReportePdfService: construye reportes PDF consolidados de la operación agrícola.
 * Mantiene la presentación tabular y controla saltos de página y textos largos.
 */
@Component
public class ReportePdfService {
    private static final Color VERDE = new Color(35, 86, 62);
    private static final Color TINTA = new Color(31, 54, 43);
    private static final Color SUAVE = new Color(91, 108, 96);
    private static final Color LINEA = new Color(220, 230, 221);
    private static final Color ALTERNADO = new Color(244, 248, 243);

    public byte[] generar(String tipo, List<Map<String,Object>> filas) throws IOException {
        var columnas = columnas(tipo);
        try (var pdf = new PDDocument(); var salida = new ByteArrayOutputStream()) {
            var regular = fuente(pdf, "Regular");
            var negrita = fuente(pdf, "Bold");
            float ancho = PDRectangle.A4.getHeight();
            float alto = PDRectangle.A4.getWidth();
            var pagina = new PDPage(new PDRectangle(ancho, alto));
            pdf.addPage(pagina);
            int numeroPagina = 1;
            var lienzo = new PDPageContentStream(pdf, pagina);
            float y = encabezado(lienzo, ancho, alto, titulo(tipo), subtitulo(tipo), numeroPagina, regular, negrita);
            float[] anchos = new float[columnas.size()];
            float util = ancho - 64;
            for (int i=0;i<anchos.length;i++) anchos[i] = util / anchos.length;
            y = dibujarCabecera(lienzo, columnas, anchos, 64, y, negrita);
            int indice = 0;
            for (var fila : filas) {
                List<String> valores = new ArrayList<>();
                for (var columna : columnas) valores.add(valor(fila.get(columna.clave())));
                List<List<String>> lineas = new ArrayList<>();
                int maxLineas = 1;
                for (int i=0;i<valores.size();i++) {
                    var celda = envolver(valores.get(i), regular, 7.2f, anchos[i]-10);
                    lineas.add(celda);
                    maxLineas = Math.max(maxLineas, Math.min(3, celda.size()));
                }
                float altoFila = Math.max(23, maxLineas*9+10);
                if (y - altoFila < 48) {
                    lienzo.close();
                    pagina = new PDPage(new PDRectangle(ancho, alto));
                    pdf.addPage(pagina);
                    numeroPagina++;
                    lienzo = new PDPageContentStream(pdf, pagina);
                    y = encabezado(lienzo, ancho, alto, titulo(tipo), subtitulo(tipo), numeroPagina, regular, negrita);
                    y = dibujarCabecera(lienzo, columnas, anchos, 64, y, negrita);
                }
                float x = 64;
                if (indice % 2 == 1) {
                    lienzo.setNonStrokingColor(ALTERNADO);
                    lienzo.addRect(64, y-altoFila, util, altoFila);
                    lienzo.fill();
                }
                lienzo.setStrokingColor(LINEA);
                lienzo.setLineWidth(.35f);
                lienzo.moveTo(64, y-altoFila);
                lienzo.lineTo(ancho-64, y-altoFila);
                lienzo.stroke();
                for (int c=0;c<columnas.size();c++) {
                    var celda = lineas.get(c);
                    float yy = y-13;
                    for (int j=0;j<Math.min(3,celda.size());j++) {
                        escribir(lienzo, regular, 7.2f, x+5, yy, TINTA, celda.get(j));
                        yy -= 9;
                    }
                    x += anchos[c];
                }
                y -= altoFila;
                indice++;
            }
            if (filas.isEmpty()) {
                escribir(lienzo, regular, 10, 64, y-20, SUAVE, "No existen registros para los criterios seleccionados.");
            }
            lienzo.close();
            pdf.getDocumentInformation().setTitle("AgroCiclo - " + titulo(tipo));
            pdf.getDocumentInformation().setAuthor("AgroCiclo");
            pdf.save(salida);
            return salida.toByteArray();
        }
    }

    private float encabezado(PDPageContentStream lienzo, float ancho, float alto, String titulo,
                             String subtitulo, int pagina, PDType0Font regular, PDType0Font negrita) throws IOException {
        lienzo.setNonStrokingColor(VERDE);
        lienzo.addRect(0, alto-9, ancho, 9);
        lienzo.fill();
        escribir(lienzo, negrita, 9, 64, alto-42, VERDE, "AGROCICLO  /  GESTIÓN DE PRODUCCIÓN AGRÍCOLA");
        escribir(lienzo, negrita, 23, 64, alto-76, TINTA, titulo);
        escribir(lienzo, regular, 9, 64, alto-94, SUAVE, subtitulo);
        escribir(lienzo, regular, 8, ancho-100, 28, SUAVE, "Página " + pagina);
        return alto-116;
    }

    private float dibujarCabecera(PDPageContentStream lienzo, List<Columna> columnas,
                                  float[] anchos, float x, float y, PDType0Font fuente) throws IOException {
        float alto = 25;
        float ancho = 0;
        for (float a : anchos) ancho += a;
        lienzo.setNonStrokingColor(VERDE);
        lienzo.addRect(x,y-alto,ancho,alto);
        lienzo.fill();
        float xx=x;
        for (int i=0;i<columnas.size();i++) {
            escribir(lienzo,fuente,7.4f,xx+5,y-16,Color.WHITE,columnas.get(i).titulo());
            xx+=anchos[i];
        }
        return y-alto;
    }

    private static List<String> envolver(String texto, PDType0Font fuente, float tam, float ancho) throws IOException {
        var palabras = texto.replaceAll("\\s+"," ").strip().split(" ");
        var lineas = new ArrayList<String>();
        var actual = new StringBuilder();
        for (var palabra : palabras) {
            String candidata = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (fuente.getStringWidth(disponible(candidata,fuente))*tam/1000 <= ancho) actual = new StringBuilder(candidata);
            else {
                if (!actual.isEmpty()) lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            }
        }
        if (!actual.isEmpty()) lineas.add(actual.toString());
        return lineas.isEmpty() ? List.of("—") : lineas;
    }

    private static String disponible(String texto, PDType0Font fuente) throws IOException {
        var salida = new StringBuilder();
        for (int cp : texto.codePoints().toArray()) {
            var c = new String(Character.toChars(cp));
            try { fuente.encode(c); salida.append(c); } catch (IllegalArgumentException e) { salida.append('?'); }
        }
        return salida.toString();
    }

    private static void escribir(PDPageContentStream lienzo, PDType0Font fuente, float tam,
                                 float x, float y, Color color, String texto) throws IOException {
        lienzo.beginText();
        lienzo.setNonStrokingColor(color);
        lienzo.setFont(fuente,tam);
        lienzo.newLineAtOffset(x,y);
        lienzo.showText(disponible(texto,fuente));
        lienzo.endText();
    }

    private static PDType0Font fuente(PDDocument pdf, String peso) throws IOException {
        try (var archivo = new ClassPathResource("tipografias/LiberationSans-"+peso+".ttf").getInputStream()) {
            return PDType0Font.load(pdf,archivo);
        }
    }

    private static String valor(Object dato) {
        if (dato == null) return "—";
        if (dato instanceof Boolean b) return b ? "Sí" : "No";
        return dato.toString();
    }

    private static String titulo(String tipo) {
        return switch (tipo) {
            case "produccion" -> "Reporte general de producción";
            case "terrenos" -> "Reporte de terrenos agrícolas";
            case "insumos" -> "Reporte de inventario de insumos";
            case "actividades" -> "Reporte de actividades agrícolas";
            default -> "Reporte AgroCiclo";
        };
    }

    private static String subtitulo(String tipo) {
        return switch (tipo) {
            case "produccion" -> "Ciclos, cultivos, cosechas, rendimiento y costos consolidados.";
            case "terrenos" -> "Listado de parcelas, ubicación y superficie registrada.";
            case "insumos" -> "Existencias, unidades y umbrales mínimos de reposición.";
            case "actividades" -> "Programación, cumplimiento y costos de las actividades agrícolas.";
            default -> "Información consolidada del sistema.";
        };
    }

    private static List<Columna> columnas(String tipo) {
        return switch (tipo) {
            case "produccion" -> List.of(
                new Columna("Código","codigo"),new Columna("Cultivo","cultivo"),new Columna("Terreno","terreno"),
                new Columna("Estado","estado"),new Columna("Inicio","fecha_inicio"),new Columna("Siembra","fecha_siembra"),
                new Columna("Kg","kg_producidos"),new Columna("Kg/ha","rendimiento_kg_ha"),new Columna("Costo USD","costo_total"));
            case "terrenos" -> List.of(new Columna("Terreno","nombre"),new Columna("Ubicación","ubicacion"),new Columna("Área ha","area_ha"));
            case "insumos" -> List.of(new Columna("Insumo","nombre"),new Columna("Categoría","categoria"),
                new Columna("Stock","stock"),new Columna("Unidad","unidad"),new Columna("Mínimo","stock_minimo"));
            case "actividades" -> List.of(new Columna("Actividad","actividad"),new Columna("Ciclo","ciclo"),
                new Columna("Terreno","terreno"),new Columna("Programada","programada_para"),
                new Columna("Obligatoria","obligatoria"),new Columna("Completada","completada_en"),new Columna("Costo","costo"));
            default -> List.of(new Columna("Dato","dato"));
        };
    }

    /** Columna: define la etiqueta y clave de datos de una columna del reporte. */
    private record Columna(String titulo, String clave) {}
}
