package ec.agrociclo.model;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class Cultivo {
    private Integer id;
    private String nombre;
    private Integer diasEstimados;
    public Cultivo() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id=id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre=nombre; }
    public Integer getDiasEstimados() { return diasEstimados; }
    public void setDiasEstimados(Integer diasEstimados) { this.diasEstimados=diasEstimados; }
}
