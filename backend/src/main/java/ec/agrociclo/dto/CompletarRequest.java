package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CompletarRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record CompletarRequest(@NotNull LocalDate fecha,
        @NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal costo) {}
