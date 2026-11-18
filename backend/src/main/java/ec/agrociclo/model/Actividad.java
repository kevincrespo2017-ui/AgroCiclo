package ec.agrociclo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class Actividad {
    private Integer id;
    private Integer cicloId;
    private String nombre;
    private LocalDate programadaPara;
    private Boolean obligatoria;
    private LocalDate completadaEn;
    private BigDecimal costo;
    private String nota;
    public Actividad() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id=id; }
    public Integer getCicloId() { return cicloId; }
    public void setCicloId(Integer cicloId) { this.cicloId=cicloId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre=nombre; }
    public LocalDate getProgramadaPara() { return programadaPara; }
    public void setProgramadaPara(LocalDate programadaPara) { this.programadaPara=programadaPara; }
    public Boolean getObligatoria() { return obligatoria; }
    public void setObligatoria(Boolean obligatoria) { this.obligatoria=obligatoria; }
    public LocalDate getCompletadaEn() { return completadaEn; }
    public void setCompletadaEn(LocalDate completadaEn) { this.completadaEn=completadaEn; }
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo=costo; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota=nota; }
}
