package ec.agrociclo.model;
import java.util.List;
import java.util.Locale;
/** Lista cerrada de tablas y columnas permitidas: nunca procede de SQL enviado por el cliente. */
public enum TipoCatalogo {
    TERRENOS("nombre","ubicacion","area_ha","imagen_url","latitud","longitud"),
    CULTIVOS("nombre","dias_estimados"),
    INSUMOS("nombre","categoria","unidad","stock_minimo");
    private final List<String> columnas;
    TipoCatalogo(String... columnas) { this.columnas=List.of(columnas); }
    public List<String> columnas() { return columnas; }
    public String tabla() { return name().toLowerCase(Locale.ROOT); }
    public static TipoCatalogo de(String tipo) { return valueOf(tipo.toUpperCase(Locale.ROOT)); }
}
