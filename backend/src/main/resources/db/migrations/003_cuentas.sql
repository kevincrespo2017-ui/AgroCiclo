ALTER TABLE usuarios ADD COLUMN verificado boolean NOT NULL DEFAULT false;
-- Las cuentas existentes son cuentas provisionadas por el administrador.
UPDATE usuarios SET verificado = true;
CREATE TABLE tokens_cuenta (
 token_hash char(64) PRIMARY KEY,
 usuario_id integer NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
 tipo varchar(16) NOT NULL CHECK(tipo IN ('VERIFICAR','RECUPERAR')),
 expira_en timestamptz NOT NULL,
 creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX tokens_cuenta_usuario ON tokens_cuenta(usuario_id,tipo);
