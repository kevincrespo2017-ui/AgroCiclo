package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PlanRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record PlanRequest(
        @NotNull @Pattern(regexp = "[A-Za-z0-9_-]{2,30}") String codigo,
        @NotNull @Positive Integer terrenoId,
        @NotNull @Positive Integer cultivoId,
        @NotNull @DecimalMin("0.001") @DecimalMax("100000") @Digits(integer = 6, fraction = 3) BigDecimal areaHa,
        @NotNull LocalDate fechaInicio,
        @Size(max = 500) String notas
    ) {}
