package ec.agrociclo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * CodigoVerificacionRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record CodigoVerificacionRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Pattern(regexp = "[0-9]{4}") String codigo
) {}
