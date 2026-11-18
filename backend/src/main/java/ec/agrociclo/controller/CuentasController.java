package ec.agrociclo.controller;

import ec.agrociclo.dto.*;
import ec.agrociclo.security.LimiteAcceso;
import ec.agrociclo.service.CuentasService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
/**
 * CuentasController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class CuentasController {
    private final CuentasService cuentas;
    private final LimiteAcceso limite;
    CuentasController(CuentasService cuentas, LimiteAcceso limite) { this.cuentas = cuentas; this.limite = limite; }
    private void limitar(HttpServletRequest req) { limite.iniciar("cuentas:" + req.getRemoteAddr()); }
    private Map<String,String> respuesta() { return Map.of("mensaje", "Si corresponde a tu cuenta, recibirás un mensaje. Revisa también la carpeta de spam."); }

    @PostMapping("/registro") Map<String,String> registro(@Valid @RequestBody RegistroRequest d, HttpServletRequest r) {
        limitar(r);
        boolean enviado = cuentas.registrar(d.nombre(), d.email(), d.password());
        return Map.of("mensaje", enviado
            ? "Te enviamos un código de cuatro dígitos. Verifica tu correo para continuar."
            : "La cuenta se registró o ya existe, pero no pudimos confirmar la entrega del correo. Revisa tu bandeja o solicita ayuda al administrador.");
    }
    @PostMapping("/reenviar") Map<String,String> reenviar(@Valid @RequestBody EmailDto d, HttpServletRequest r) { limitar(r); cuentas.solicitar(d.email(), "VERIFICAR"); return respuesta(); }
    @PostMapping("/recuperar") Map<String,String> recuperar(@Valid @RequestBody EmailDto d, HttpServletRequest r) { limitar(r); cuentas.solicitar(d.email(), "RECUPERAR"); return respuesta(); }
    @PostMapping("/verificar") Map<String,String> verificar(@Valid @RequestBody CodigoVerificacionRequest d, HttpServletRequest r) {
        limitar(r); cuentas.consumirCodigo(d.email(), d.codigo());
        return Map.of("mensaje", "Correo verificado. Tu cuenta quedó pendiente de aprobación por el administrador.");
    }
    @PostMapping("/restablecer") Map<String,String> restablecer(@Valid @RequestBody ClaveDto d, HttpServletRequest r) {
        limitar(r); cuentas.consumir(d.token(), "RECUPERAR", d.password());
        return Map.of("mensaje", "Contraseña actualizada. Inicia sesión con la nueva contraseña.");
    }
}
