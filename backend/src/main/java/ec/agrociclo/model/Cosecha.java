package ec.agrociclo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class Cosecha {
    private Integer id;
    private Integer cicloId;
    private String folio;
    private LocalDate fecha;
    private BigDecimal kilogramos;
    private String nota;
    public Cosecha() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id=id; }
    public Integer getCicloId() { return cicloId; }
    public void setCicloId(Integer cicloId) { this.cicloId=cicloId; }
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio=folio; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha=fecha; }
    public BigDecimal getKilogramos() { return kilogramos; }
    public void setKilogramos(BigDecimal kilogramos) { this.kilogramos=kilogramos; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota=nota; }
}
