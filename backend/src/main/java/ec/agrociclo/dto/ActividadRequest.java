package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * ActividadRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record ActividadRequest(@NotBlank @Size(max = 100) String nombre, @NotNull LocalDate programadaPara,
        @NotNull Boolean obligatoria, @Size(max = 500) String nota) {}
