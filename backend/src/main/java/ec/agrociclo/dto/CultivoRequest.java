package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.util.Map;

/**
 * CultivoRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record CultivoRequest(@NotBlank @Size(max = 100) String nombre,
                   @NotNull @Min(1) @Max(730) Integer diasEstimados) {
    public Map<String, Object> datos() { return campos(nombre.strip(), diasEstimados); }
     private static Map<String,Object> campos(Object... valores) { var datos=new java.util.LinkedHashMap<String,Object>(); String[] claves={"nombre","dias_estimados"}; for(int i=0;i<claves.length;i++) datos.put(claves[i],valores[i]); return datos; }
}
