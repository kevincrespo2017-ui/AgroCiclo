package ec.agrociclo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * AsistenteRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record AsistenteRequest(
    @NotBlank @Size(max = 1200) String mensaje,
    @Size(max = 8) List<@Valid MensajeChat> historial
) {
    /** MensajeChat: representa un mensaje dentro del intercambio con el asistente agrícola. */
public record MensajeChat(
        @NotBlank @Pattern(regexp = "user|assistant") String rol,
        @NotBlank @Size(max = 1200) String contenido
    ) {}
}
