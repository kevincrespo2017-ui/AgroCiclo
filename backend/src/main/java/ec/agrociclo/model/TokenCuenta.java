package ec.agrociclo.model;

import java.time.OffsetDateTime;

/** Modelo de dominio independiente de HTTP, servicios y persistencia. */
public class TokenCuenta {
    private String tokenHash;
    private Integer usuarioId;
    private String tipo;
    private OffsetDateTime expiraEn;
    private OffsetDateTime creadoEn;
    public TokenCuenta() {}
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash=tokenHash; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId=usuarioId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo=tipo; }
    public OffsetDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(OffsetDateTime expiraEn) { this.expiraEn=expiraEn; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn=creadoEn; }
}
