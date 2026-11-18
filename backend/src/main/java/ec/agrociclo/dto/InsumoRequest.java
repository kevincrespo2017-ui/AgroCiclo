package ec.agrociclo.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * InsumoRequest: Define los datos de entrada o salida que intercambia la API.
 */
public record InsumoRequest(@NotBlank @Size(max = 100) String nombre,
                  @NotNull @Pattern(regexp = "FERTILIZANTE|SEMILLA|FITOSANITARIO|OTRO") String categoria,
                  @NotNull @Pattern(regexp = "KG|L|UN") String unidad,
                  @NotNull @DecimalMin("0") @DecimalMax("100000") @Digits(integer = 6, fraction = 3) BigDecimal stockMinimo) {
    public Map<String, Object> datos() { return campos(nombre.strip(), categoria, unidad, stockMinimo); }
     private static Map<String,Object> campos(Object... valores) { var datos=new java.util.LinkedHashMap<String,Object>(); String[] claves={"nombre","categoria","unidad","stock_minimo"}; for(int i=0;i<claves.length;i++) datos.put(claves[i],valores[i]); return datos; }
}
