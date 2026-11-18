package ec.agrociclo.dto;
import jakarta.validation.constraints.*;

/**
 * TokenDto: Define los datos de entrada o salida que intercambia la API.
 */
public record TokenDto(@NotBlank @Pattern(regexp="[0-9a-f]{64}") String token) {}
