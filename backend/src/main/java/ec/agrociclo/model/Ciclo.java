package ec.agrociclo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class Ciclo {
    private Integer id;
    private String codigo;
    private Integer terrenoId;
    private Integer cultivoId;
    private BigDecimal areaHa;
    private String estado;
    private LocalDate fechaInicio;
    private LocalDate fechaSiembra;
    private LocalDate fechaEstimada;
    private LocalDate fechaCierre;
    private String notas;
    public Ciclo() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id=id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo=codigo; }
    public Integer getTerrenoId() { return terrenoId; }
    public void setTerrenoId(Integer terrenoId) { this.terrenoId=terrenoId; }
    public Integer getCultivoId() { return cultivoId; }
    public void setCultivoId(Integer cultivoId) { this.cultivoId=cultivoId; }
    public BigDecimal getAreaHa() { return areaHa; }
    public void setAreaHa(BigDecimal areaHa) { this.areaHa=areaHa; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado=estado; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio=fechaInicio; }
    public LocalDate getFechaSiembra() { return fechaSiembra; }
    public void setFechaSiembra(LocalDate fechaSiembra) { this.fechaSiembra=fechaSiembra; }
    public LocalDate getFechaEstimada() { return fechaEstimada; }
    public void setFechaEstimada(LocalDate fechaEstimada) { this.fechaEstimada=fechaEstimada; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre=fechaCierre; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas=notas; }
}
