package ec.agrociclo.service;
/** Puerto de envío de correo; su implementación SMTP se configura por propiedades. */
public interface Correo {
    void enlace(String destino, String tipo, String token);
    void codigo(String destino, String codigo);
    void estadoCuenta(String destino, boolean aprobado, String motivo);
}
