package ec.agrociclo.dto;
import jakarta.validation.constraints.*;

/**
 * AccesoRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record AccesoRequest(@NotBlank @Email @Size(max = 254) String email,
                  @NotEmpty @Size(max = 128) String password) {}
