package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MovimientoRequest: Define los datos de entrada o salida que intercambia la API.
 */
public interface MovimientoRequest {
        String folio();
        LocalDate fecha();
        BigDecimal cantidad();
        BigDecimal costoUnitario();
    }
