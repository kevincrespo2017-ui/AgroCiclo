-- Incorpora el rol de supervisión sin alterar las cuentas existentes.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check
  CHECK (rol IN ('ADMIN', 'SUPERVISOR', 'OPERATIVO'));

ALTER TABLE notificaciones DROP CONSTRAINT IF EXISTS notificaciones_rol_destino_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_rol_destino_check
  CHECK (rol_destino IN ('ADMIN', 'SUPERVISOR', 'OPERATIVO'));
