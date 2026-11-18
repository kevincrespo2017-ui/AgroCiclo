package ec.agrociclo.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
/**
 * FotoCultivoRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record FotoCultivoRequest(
    @NotBlank @Size(max=8_000_000) String imagenBase64,
    @NotBlank @Pattern(regexp="image/(jpeg|png|webp)") String tipoMime
) {}
