package ec.agrociclo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TerrenoRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record TerrenoRequest(
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 120) String ubicacion,
    @NotNull @DecimalMin("0.001") @DecimalMax("100000") @Digits(integer = 6, fraction = 3) BigDecimal areaHa,
    @Size(max = 500) String imagenUrl,
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitud,
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitud
) {
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String,Object>();
        datos.put("nombre", nombre.strip());
        datos.put("ubicacion", ubicacion.strip());
        datos.put("area_ha", areaHa);
        datos.put("imagen_url", imagenUrl == null || imagenUrl.isBlank() ? null : imagenUrl.strip());
        datos.put("latitud", latitud);
        datos.put("longitud", longitud);
        return datos;
    }
}
