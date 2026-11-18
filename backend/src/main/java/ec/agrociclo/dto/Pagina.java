package ec.agrociclo.dto;
import java.util.List;
import java.util.Map;
/**
 * Pagina: Define los datos de entrada o salida que intercambia la API.
 */
public record Pagina(List<Map<String,Object>> datos, int pagina, int tam, int total) {}
