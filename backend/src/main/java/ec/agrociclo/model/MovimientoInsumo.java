package ec.agrociclo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class MovimientoInsumo {
    private Integer id;
    private Integer insumoId;
    private Integer cicloId;
    private String tipo;
    private String folio;
    private LocalDate fecha;
    private BigDecimal cantidad;
    private BigDecimal costoUnitario;
    public MovimientoInsumo() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id=id; }
    public Integer getInsumoId() { return insumoId; }
    public void setInsumoId(Integer insumoId) { this.insumoId=insumoId; }
    public Integer getCicloId() { return cicloId; }
    public void setCicloId(Integer cicloId) { this.cicloId=cicloId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo=tipo; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio=folio; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha=fecha; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad=cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario=costoUnitario; }
}
