package ec.agrociclo.service;

import ec.agrociclo.repository.AutenticacionRepository;
import ec.agrociclo.repository.MapeadorModelos;
import ec.agrociclo.model.Usuario;

import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.exception.ErrorApi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * AutenticacionService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class AutenticacionService {
    private final AutenticacionRepository persistencia;
    private final BaseDatos db;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);
    private final SecureRandom azar = new SecureRandom();
    private final String hashAlterno = bcrypt.encode(token());
    private static final Pattern BEARER = Pattern.compile("^Bearer ([0-9a-f]{64})$", Pattern.CASE_INSENSITIVE);

    public AutenticacionService(BaseDatos db, AutenticacionRepository persistencia) {
        this.persistencia=persistencia; this.db = db; }

    @Transactional
    public Map<String, Object> entrar(String email, String password) {
        var cuenta = persistencia.entrarConsultarUsuarios(email.strip().toLowerCase(Locale.ROOT));
        var hash = cuenta == null ? hashAlterno : cuenta.get("password_hash").toString();
        boolean coincide = password.getBytes(StandardCharsets.UTF_8).length <= 72 && bcrypt.matches(password, hash);
        if (cuenta == null || !coincide) {
            throw new ErrorApi(401, "CREDENCIALES_INVALIDAS", "Revisa el correo y la contraseña.");
        }
        if ("RECHAZADO".equals(cuenta.get("estado_aprobacion")))
            throw new ErrorApi(403, "CUENTA_RECHAZADA", "Tu cuenta fue rechazada por el administrador. Contacta con soporte para más información.");
        if (!Boolean.TRUE.equals(cuenta.get("verificado")))
            throw new ErrorApi(403, "CORREO_PENDIENTE", "Verifica tu correo con el código de cuatro dígitos enviado a tu bandeja de entrada.");
        if (!"APROBADO".equals(cuenta.get("estado_aprobacion")))
            throw new ErrorApi(403, "APROBACION_PENDIENTE", "Tu correo ya está verificado. Espera la aprobación del administrador para ingresar.");
        var usuario = MapeadorModelos.usuario(cuenta);
        var token = token();
        persistencia.entrarActualizarSesiones(usuario.getId());
        persistencia.entrarActualizarSesiones2(huella(token), usuario.getId());
        return Map.of("token", token, "usuario", usuario);
    }

    public Usuario deCabecera(String valor) {
        var hash = huellaDeCabecera(valor);
        var fila = persistencia.deCabeceraConsultarSesiones(hash);
        if (fila == null) throw new ErrorApi(401, "SESION_INVALIDA", "La sesión expiró o dejó de ser válida.");
        return MapeadorModelos.usuario(fila);
    }

    public void salir(String valor) {
        persistencia.salirActualizarSesiones(huellaDeCabecera(valor));
    }

    public static String token() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String huellaDeCabecera(String valor) {
        var coincidencia = BEARER.matcher(valor == null ? "" : valor);
        if (!coincidencia.matches()) throw new ErrorApi(401, "SESION_REQUERIDA", "Inicia sesión para continuar.");
        return huella(coincidencia.group(1));
    }

    public static String huella(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
