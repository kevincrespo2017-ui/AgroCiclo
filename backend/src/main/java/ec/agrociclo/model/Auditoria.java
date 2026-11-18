package ec.agrociclo.model;

import java.time.OffsetDateTime;
import java.util.Map;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class Auditoria {
    private Integer id;
    private Integer usuarioId;
    private OffsetDateTime fechaHora;
    private String accion;
    private String entidad;
    private Integer registroId;
    private Map<String,Object> detalle;
    public Auditoria() {}
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id=id; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId=usuarioId; }
    public OffsetDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(OffsetDateTime fechaHora) { this.fechaHora=fechaHora; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion=accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad=entidad; }
    public Integer getRegistroId() { return registroId; }
    public void setRegistroId(Integer registroId) { this.registroId=registroId; }
    public Map<String,Object> getDetalle() { return detalle; }
    public void setDetalle(Map<String,Object> detalle) { this.detalle=detalle; }
}
