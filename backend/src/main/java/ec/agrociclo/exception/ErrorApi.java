package ec.agrociclo.exception;

import java.util.Map;

/**
 * ErrorApi: Define el tratamiento consistente de errores de la API.
 */
public class ErrorApi extends RuntimeException {
    private final int estado;
    private final String codigo;
    private final Map<String, String> campos;

    public ErrorApi(int estado, String codigo, String mensaje) {
        this(estado, codigo, mensaje, Map.of());
    }

    public ErrorApi(int estado, String codigo, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.campos = campos;
    }

    public int estado() { return estado; }
    public String codigo() { return codigo; }
    public Map<String, String> campos() { return campos; }

    public static ErrorApi noEncontrado(String sujeto) {
        return new ErrorApi(404, "NO_ENCONTRADO", "No se encontró " + sujeto + ".");
    }

    public static ErrorApi conflicto(String codigo, String mensaje) {
        return new ErrorApi(409, codigo, mensaje);
    }

    public static ErrorApi invalido(String codigo, String mensaje) {
        return new ErrorApi(422, codigo, mensaje);
    }

    public static ErrorApi campo(String campo, String mensaje) {
        return new ErrorApi(422, "DATOS_INVALIDOS", "Revisa los datos ingresados.", Map.of(campo, mensaje));
    }
}
