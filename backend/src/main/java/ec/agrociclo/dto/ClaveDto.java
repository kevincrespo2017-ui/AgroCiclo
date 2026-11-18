package ec.agrociclo.dto;
import jakarta.validation.constraints.*;

/**
 * ClaveDto: Define los datos de entrada o salida que intercambia la API.
 */
public record ClaveDto(@NotBlank @Pattern(regexp="[0-9a-f]{64}") String token,@NotBlank @Size(min=8,max=72) String password) {}
