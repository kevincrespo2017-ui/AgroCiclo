package ec.agrociclo.validation;

import ec.agrociclo.exception.ErrorApi;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validaciones: Centraliza las validaciones de datos y reglas comunes.
 */
public final class Validaciones {
    public static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static final LocalDate PRIMERA = LocalDate.of(1900, 1, 1);
    private static final LocalDate ULTIMA = LocalDate.of(2199, 12, 31);

    private Validaciones() {}

    public static LocalDate hoy() { return LocalDate.now(ZONA); }
    public static String nota(String valor) { return valor == null ? "" : valor.strip(); }
    public static String folio(String valor) { return valor.strip().toUpperCase(Locale.ROOT); }

    public static int id(int valor) {
        if (valor < 1) throw ErrorApi.campo("id", "Usa un identificador positivo.");
        return valor;
    }

    public static LocalDate fecha(LocalDate valor, String campo) {
        if (valor.isBefore(PRIMERA) || valor.isAfter(ULTIMA)) {
            throw ErrorApi.campo(campo, "Ingresa una fecha entre 1900 y 2199.");
        }
        return valor;
    }

    public static void fechaReal(LocalDate valor, LocalDate desde, String mensaje) {
        fecha(valor, "fecha");
        if (valor.isAfter(hoy()) || desde != null && valor.isBefore(desde)) {
            throw ErrorApi.invalido("FECHA_INVALIDA", mensaje);
        }
    }

    public static LocalDate fechaFiltro(Map<String, String> consulta, String campo) {
        var valor = consulta.get(campo);
        if (valor == null) return null;
        try {
            return fecha(LocalDate.parse(valor), campo);
        } catch (DateTimeParseException error) {
            throw ErrorApi.campo(campo, "Usa una fecha AAAA-MM-DD.");
        }
    }

    public static void parametros(Map<String, String> consulta, Set<String> permitidos) {
        consulta.keySet().stream().filter(campo -> !permitidos.contains(campo)).findFirst()
            .ifPresent(campo -> { throw ErrorApi.campo(campo, "Parámetro desconocido."); });
    }

    public static int enteroConsulta(Map<String, String> consulta, String campo, int defecto, int maximo) {
        var valor = consulta.get(campo);
        if (valor == null) return defecto;
        try {
            if (!valor.matches("[0-9]+")) throw new NumberFormatException();
            int numero = Integer.parseInt(valor);
            if (numero > 0 && numero <= maximo) return numero;
        } catch (NumberFormatException ignored) {
            // La respuesta es la misma para números fuera de rango y texto inválido.
        }
        throw ErrorApi.campo(campo, "Usa un entero positivo hasta " + maximo + ".");
    }
}
