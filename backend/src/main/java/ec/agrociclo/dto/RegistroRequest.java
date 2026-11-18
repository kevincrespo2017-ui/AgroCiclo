package ec.agrociclo.dto;
import jakarta.validation.constraints.*;

/**
 * RegistroRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record RegistroRequest(@NotBlank @Size(max=100) String nombre,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=72) String password) {}
