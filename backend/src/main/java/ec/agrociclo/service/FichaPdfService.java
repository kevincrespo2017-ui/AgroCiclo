package ec.agrociclo.service;

import ec.agrociclo.validation.Validaciones;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
/**
 * FichaPdfService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class FichaPdfService {
    private static final Color VERDE = new Color(36, 88, 65);
    private static final Color TEXTO = new Color(21, 52, 36);
    private static final Color SECUNDARIO = new Color(61, 85, 70);
    private static final Color LINEA = new Color(220, 232, 221);
    private static final float MARGEN = 46;
    private static final Locale ECUADOR = Locale.forLanguageTag("es-EC");

    public byte[] generar(Map<String, Object> ciclo) throws IOException {
        var numero = NumberFormat.getNumberInstance(ECUADOR);
        numero.setMaximumFractionDigits(3);
        var moneda = NumberFormat.getCurrencyInstance(ECUADOR);
        var filas = List.of(
            new Fila("Área del ciclo", numero.format(decimal(ciclo, "area_ha")) + " ha"),
            new Fila("Inicio del ciclo", valor(ciclo, "fecha_inicio", "Pendiente")),
            new Fila("Siembra registrada", valor(ciclo, "fecha_siembra", "Pendiente")),
            new Fila("Cosecha prevista", valor(ciclo, "fecha_estimada", "Pendiente")),
            new Fila("Cierre", valor(ciclo, "fecha_cierre", "Abierto")),
            new Fila("Producción registrada", numero.format(decimal(ciclo, "kg_producidos")) + " kg"),
            new Fila("Rendimiento", ciclo.get("rendimiento_kg_ha") == null ? "Sin cosecha registrada" :
                numero.format(decimal(ciclo, "rendimiento_kg_ha")) + " kg/ha"),
            new Fila("Costos de actividades", moneda.format(decimal(ciclo, "costo_actividades"))),
            new Fila("Costos de insumos aplicados", moneda.format(decimal(ciclo, "costo_insumos"))),
            new Fila("Costo total", moneda.format(decimal(ciclo, "costo_total"))),
            new Fila("Actividades obligatorias pendientes", valor(ciclo, "obligatorias_pendientes", "0"))
        );
        try (var pdf = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            var hoja = new PDPage(PDRectangle.A4);
            pdf.addPage(hoja);
            pdf.getDocumentInformation().setTitle("Ficha de ciclo " + ciclo.get("codigo"));
            pdf.getDocumentInformation().setAuthor("AgroCiclo");
            var regular = fuente(pdf, "Regular");
            var negrita = fuente(pdf, "Bold");
            float ancho = hoja.getMediaBox().getWidth();
            float alto = hoja.getMediaBox().getHeight();
            try (var lienzo = new PDPageContentStream(pdf, hoja)) {
                lienzo.setNonStrokingColor(VERDE);
                lienzo.addRect(0, alto - 8, ancho, 8);
                lienzo.fill();
                escribir(lienzo, negrita, 10, MARGEN, alto - 50, VERDE, "AGROCICLO / PRODUCCIÓN AGRÍCOLA");
                float y = alto - 83;
                for (var linea : lineas(negrita, "Ficha del ciclo " + ciclo.get("codigo"), 26, ancho - 2 * MARGEN)) {
                    escribir(lienzo, negrita, 26, MARGEN, y, TEXTO, linea);
                    y -= 30;
                }
                for (var linea : lineas(regular, ciclo.get("cultivo") + " · " + ciclo.get("terreno"), 12, ancho - 2 * MARGEN)) {
                    escribir(lienzo, regular, 12, MARGEN, y, SECUNDARIO, linea);
                    y -= 15;
                }
                escribir(lienzo, regular, 12, MARGEN, y, SECUNDARIO, "Estado " + ciclo.get("estado"));
                y -= 19;
                lienzo.setLineWidth(0.45f);
                lienzo.setStrokingColor(LINEA);
                for (var fila : filas) {
                    lienzo.moveTo(MARGEN, y);
                    lienzo.lineTo(ancho - MARGEN, y);
                    lienzo.stroke();
                    escribir(lienzo, negrita, 11, MARGEN + 4, y - 17, TEXTO, fila.titulo);
                    escribir(lienzo, regular, 11, MARGEN + 251, y - 17, SECUNDARIO, fila.valor);
                    y -= 25;
                }
                var notas = valor(ciclo, "notas", "");
                if (!notas.isBlank()) {
                    y -= 12;
                    escribir(lienzo, negrita, 11, MARGEN, y, TEXTO, "Observaciones");
                    y -= 16;
                    for (var linea : lineas(regular, notas, 10.5f, ancho - 2 * MARGEN)) {
                        escribir(lienzo, regular, 10.5f, MARGEN, y, SECUNDARIO, linea);
                        y -= 13;
                    }
                }
                escribir(lienzo, regular, 9, MARGEN, y - 16, SECUNDARIO,
                    "Emitido el " + Validaciones.hoy() + " · Kilogramos por hectárea del ciclo · Costos en USD");
            }
            pdf.save(bytes);
            return bytes.toByteArray();
        }
    }

    private static PDType0Font fuente(PDDocument pdf, String peso) throws IOException {
        try (var archivo = new ClassPathResource("tipografias/LiberationSans-" + peso + ".ttf").getInputStream()) {
            return PDType0Font.load(pdf, archivo);
        }
    }

    private static void escribir(PDPageContentStream lienzo, PDType0Font fuente, float tam,
                                 float x, float y, Color color, String frase) throws IOException {
        lienzo.beginText();
        lienzo.setNonStrokingColor(color);
        lienzo.setFont(fuente, tam);
        lienzo.newLineAtOffset(x, y);
        lienzo.showText(textoDisponible(fuente, frase));
        lienzo.endText();
    }

    private static String textoDisponible(PDType0Font fuente, String texto) throws IOException {
        var resultado = new StringBuilder();
        for (int cp : texto.replaceAll("\\s+", " ").codePoints().toArray()) {
            var caracter = new String(Character.toChars(cp));
            try {
                fuente.encode(caracter);
                resultado.append(caracter);
            } catch (IllegalArgumentException error) {
                resultado.append('?');
            }
        }
        return resultado.toString();
    }

    private static List<String> lineas(PDType0Font fuente, String texto, float tam, float ancho) throws IOException {
        var resultado = new ArrayList<String>();
        var pendiente = textoDisponible(fuente, texto);
        while (fuente.getStringWidth(pendiente) * tam / 1000 > ancho) {
            int corte = 1;
            while (corte < pendiente.length() && fuente.getStringWidth(pendiente.substring(0, corte + 1)) * tam / 1000 <= ancho) corte++;
            var limitePalabra = pendiente.lastIndexOf(' ', corte);
            if (limitePalabra > 0) corte = limitePalabra;
            resultado.add(pendiente.substring(0, corte).stripTrailing());
            pendiente = pendiente.substring(corte).stripLeading();
        }
        resultado.add(pendiente);
        return resultado;
    }

    private static String valor(Map<String, Object> fila, String llave, String defecto) {
        var valor = fila.get(llave);
        return valor == null ? defecto : valor.toString();
    }

    private static BigDecimal decimal(Map<String, Object> fila, String llave) {
        return new BigDecimal(fila.get(llave).toString());
    }

    /** Fila: representa una fila de etiqueta y valor dentro de la ficha PDF. */
    private record Fila(String titulo, String valor) {}
}
