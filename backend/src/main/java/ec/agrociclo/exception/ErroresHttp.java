package ec.agrociclo.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
/**
 * ErroresHttp: Define el tratamiento consistente de errores de la API.
 */
public class ErroresHttp {
    @ExceptionHandler(ErrorApi.class)
    public ResponseEntity<?> propio(ErrorApi error) {
        var detalle = new LinkedHashMap<String, Object>();
        detalle.put("codigo", error.codigo());
        detalle.put("mensaje", error.getMessage());
        if (!error.campos().isEmpty()) detalle.put("campos", error.campos());
        return ResponseEntity.status(error.estado()).body(Map.of("error", detalle));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> formulario(MethodArgumentNotValidException error) {
        var campos = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(campo ->
            campos.putIfAbsent(nombreJson(campo.getField()), campo.getDefaultMessage()));
        return propio(new ErrorApi(422, "DATOS_INVALIDOS", "Revisa los datos ingresados.", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> cuerpo(HttpMessageNotReadableException error) {
        if (error.getCause() instanceof JsonParseException) {
            return propio(new ErrorApi(400, "JSON_INVALIDO", "El cuerpo de la solicitud debe ser JSON válido."));
        }
        if (error.getCause() instanceof JsonMappingException mapeo && !mapeo.getPath().isEmpty()) {
            String llave = mapeo.getPath().get(0).getFieldName();
            if (llave != null) return propio(ErrorApi.campo(llave, "Revisa este campo."));
        }
        return propio(ErrorApi.invalido("DATOS_INVALIDOS", "Revisa el cuerpo de la solicitud."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> tipo(MethodArgumentTypeMismatchException error) {
        return propio(ErrorApi.campo(error.getName(), "Revisa este campo."));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> restricciones(ConstraintViolationException error) {
        return propio(ErrorApi.invalido("DATOS_INVALIDOS", "Revisa los datos ingresados."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integridad(DataIntegrityViolationException error) {
        var causa = error.getMostSpecificCause();
        if (causa instanceof SQLException sql) {
            if ("23505".equals(sql.getSQLState())) {
                var detalle = sql.getMessage();
                String mensaje = "Ya existe un registro con ese nombre.";
                if (detalle.contains("cosechas_folio_unico")) mensaje = "Ya existe una cosecha con ese folio en este ciclo.";
                else if (detalle.contains("movimientos_folio_unico")) mensaje = "Ese folio ya está registrado para el insumo y tipo de movimiento.";
                else if (detalle.contains("ciclos_codigo_key")) mensaje = "Ya existe un ciclo con ese código.";
                return propio(ErrorApi.conflicto("DUPLICADO", mensaje));
            }
            if ("23503".equals(sql.getSQLState())) {
                return propio(ErrorApi.conflicto("REGISTRO_VINCULADO", "El registro está vinculado con otros datos."));
            }
        }
        return interno(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> ruta() {
        return propio(new ErrorApi(404, "RUTA_NO_ENCONTRADA", "La ruta solicitada no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> metodo() {
        return propio(new ErrorApi(405, "METODO_NO_PERMITIDO", "La ruta no admite ese método HTTP."));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessResourceFailureException.class)
    public ResponseEntity<?> baseNoDisponible() {
        return propio(new ErrorApi(503, "BASE_NO_DISPONIBLE", "No se pudo conectar con los datos. Intenta más tarde."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> interno(Exception error) {
        LoggerFactory.getLogger(ErroresHttp.class).error("Fallo al atender una solicitud de la API ({})", error.getClass().getSimpleName());
        return propio(new ErrorApi(500, "ERROR_INTERNO", "Ocurrió un problema al procesar la solicitud."));
    }

    private static String nombreJson(String nombre) {
        return nombre.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }
}
