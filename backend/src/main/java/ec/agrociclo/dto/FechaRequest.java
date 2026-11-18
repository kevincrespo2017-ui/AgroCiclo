package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * FechaRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record FechaRequest(@NotNull LocalDate fecha) {}
