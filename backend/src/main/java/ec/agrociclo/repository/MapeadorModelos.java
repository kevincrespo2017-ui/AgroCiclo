package ec.agrociclo.repository;
import ec.agrociclo.model.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.Map;

/**
 * MapeadorModelos: Centraliza las consultas y operaciones de persistencia en PostgreSQL.
 */
public final class MapeadorModelos {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private MapeadorModelos() {}
    public static <T> T convertir(Map<String,Object> fila, Class<T> tipo) {
        return fila == null ? null : JSON.convertValue(fila,tipo);
    }
    public static Usuario usuario(Map<String,Object> fila) { return convertir(fila,Usuario.class); }
}
