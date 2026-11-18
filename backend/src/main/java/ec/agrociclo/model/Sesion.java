package ec.agrociclo.model;

import java.time.OffsetDateTime;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class Sesion {
    private String tokenHash;
    private Integer usuarioId;
    private OffsetDateTime expiraEn;
    public Sesion() {}
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash=tokenHash; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId=usuarioId; }
    public OffsetDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(OffsetDateTime expiraEn) { this.expiraEn=expiraEn; }
}
