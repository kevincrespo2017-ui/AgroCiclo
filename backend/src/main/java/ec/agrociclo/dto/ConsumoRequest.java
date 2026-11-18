package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ConsumoRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record ConsumoRequest(@NotNull @Positive Integer insumoId, @NotNull @Pattern(regexp = "[A-Za-z0-9_-]{2,30}") String folio,
        @NotNull LocalDate fecha,
        @NotNull @DecimalMin("0.001") @DecimalMax("1000000") @Digits(integer = 7, fraction = 3) BigDecimal cantidad,
        @NotNull @DecimalMin("0") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2) BigDecimal costoUnitario
    ) implements MovimientoRequest {}
