package ec.agrociclo.service;

import ec.agrociclo.repository.CuentasRepository;
import ec.agrociclo.repository.MapeadorModelos;
import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.exception.ErrorApi;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * CuentasService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class CuentasService {
    private final CuentasRepository persistencia;
    private final BaseDatos db;
    private final Correo correo;
    private final NotificacionesService notificaciones;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);
    private final SecureRandom azar = new SecureRandom();

    public CuentasService(BaseDatos db, Correo correo, CuentasRepository persistencia, NotificacionesService notificaciones) {
        this.persistencia = persistencia;
        this.db = db;
        this.correo = correo;
        this.notificaciones = notificaciones;
    }

    private String email(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private void clave(String value) {
        if (value.length() < 8 || value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw ErrorApi.campo("password", "Usa al menos 8 caracteres y como máximo 72 bytes.");
    }

    @Transactional
    public boolean registrar(String nombre, String email, String password) {
        clave(password);
        email = email(email);
        var usuario = persistencia.registrarConsultarUsuarios(nombre.strip(), email, bcrypt.encode(password));
        if (usuario == null) return false;
        int id = ((Number) usuario.get("id")).intValue();
        boolean enviado = emitir(id, email, "VERIFICAR");
        notificaciones.paraRol("ADMIN", enviado ? "Nuevo registro pendiente" : "Registro pendiente de revisión manual",
            nombre.strip() + " (" + email + ") creó una cuenta. " + (enviado ? "Se generó un código de verificación." : "No se pudo entregar el código; puedes aprobar la cuenta manualmente."),
            "USUARIO", "/usuarios");
        return enviado;
    }

    @Transactional
    public boolean solicitar(String email, String tipo) {
        var u = persistencia.solicitarConsultarUsuarios(email(email));
        if (u == null) return false;
        if (tipo.equals("VERIFICAR") && Boolean.TRUE.equals(u.get("verificado"))) return false;
        if (tipo.equals("RECUPERAR") && (!Boolean.TRUE.equals(u.get("verificado")) || !"APROBADO".equals(u.get("estado_aprobacion")))) return false;
        int id = ((Number) u.get("id")).intValue();
        if (persistencia.solicitarConsultarTokensCuenta(id, tipo) != null) return false;
        return emitir(id, u.get("email").toString(), tipo);
    }

    private boolean emitir(int id, String email, String tipo) {
        persistencia.emitirActualizarTokensCuenta(id, tipo);
        if (tipo.equals("VERIFICAR")) {
            String codigo = String.format(Locale.ROOT, "%04d", azar.nextInt(10_000));
            persistencia.emitirCodigoActualizarTokensCuenta(bcrypt.encode(codigo), id, tipo);
            try { correo.codigo(email, codigo); return true; }
            catch (ErrorApi error) {
                if (error.estado() < 500) throw error;
                notificaciones.paraRol("ADMIN", "No se pudo enviar el código", "La cuenta " + email + " requiere revisión manual porque el servicio de correo no entregó el código.", "USUARIO", "/usuarios");
                return false;
            }
        }
        String token = ec.agrociclo.service.AutenticacionService.token();
        persistencia.emitirActualizarTokensCuenta2(AutenticacionService.huella(token), id, tipo);
        correo.enlace(email, tipo, token);
        return true;
    }

    @Transactional
    public void consumirCodigo(String email, String codigo) {
        var u = persistencia.solicitarConsultarUsuarios(email(email));
        if (u == null) throw invalido();
        int id = ((Number) u.get("id")).intValue();
        if ("RECHAZADO".equals(u.get("estado_aprobacion")))
            throw new ErrorApi(403, "CUENTA_RECHAZADA", "Esta cuenta fue rechazada. Contacta al administrador.");
        var token = persistencia.consumirConsultarCodigoCuenta(id, "VERIFICAR");
        if (token == null || !bcrypt.matches(codigo, token.get("token_hash").toString().trim())) throw invalido();
        persistencia.consumirEliminarTokensCuenta(id, "VERIFICAR");
        persistencia.consumirActualizarUsuarios(id);
        notificaciones.paraUsuario(id, "Correo verificado", "Tu correo fue confirmado. Tu cuenta está pendiente de aprobación por el administrador.", "CUENTA", "/");
        db.auditar(MapeadorModelos.usuario(u), "VERIFICAR_CORREO", "usuarios", id, Map.of("resultado", "VERIFICADO"));
    }

    @Transactional
    public void consumir(String token, String tipo, String password) {
        if (tipo.equals("RECUPERAR")) clave(password);
        var u = persistencia.consumirConsultarUsuarios(AutenticacionService.huella(token));
        if (u == null) throw invalido();
        var t = persistencia.consumirConsultarTokensCuenta(AutenticacionService.huella(token), tipo);
        if (t == null) throw invalido();
        int id = ((Number) u.get("id")).intValue();
        if (tipo.equals("VERIFICAR")) {
            persistencia.consumirActualizarUsuarios(id);
            notificaciones.paraUsuario(id, "Correo verificado", "Tu correo fue confirmado. Tu cuenta está pendiente de aprobación por el administrador.", "CUENTA", "/");
        } else {
            persistencia.consumirActualizarUsuarios2(bcrypt.encode(password), id);
            persistencia.consumirActualizarSesiones(id);
        }
        db.auditar(MapeadorModelos.usuario(u), tipo, "usuarios", id, Map.of("resultado", "COMPLETADO"));
    }

    private ErrorApi invalido() {
        return new ErrorApi(400, "CODIGO_INVALIDO", "El código venció, ya se utilizó o no es correcto. Solicita uno nuevo.");
    }
}
