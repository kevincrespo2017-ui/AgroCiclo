package ec.agrociclo.dto;

import jakarta.validation.constraints.Size;

/**
 * RechazoUsuarioRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record RechazoUsuarioRequest(@Size(max = 300) String motivo) {}
