package ec.agrociclo.dto;
import jakarta.validation.constraints.*;

/**
 * EmailDto: Define los datos de entrada o salida que intercambia la API.
 */
public record EmailDto(@NotBlank @Email @Size(max=254) String email) {}
