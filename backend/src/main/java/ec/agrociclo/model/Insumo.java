package ec.agrociclo.model;

import java.math.BigDecimal;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class Insumo {
    private Integer id;
    private String nombre;
    private String categoria;
    private String unidad;
    private BigDecimal stock;
    private BigDecimal stockMinimo;
    public Insumo() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id=id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre=nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria=categoria; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad=unidad; }
    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock=stock; }
    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo=stockMinimo; }
}
