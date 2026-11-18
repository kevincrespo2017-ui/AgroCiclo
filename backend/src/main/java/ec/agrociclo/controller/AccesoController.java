package ec.agrociclo.controller;

import ec.agrociclo.dto.*;
import ec.agrociclo.model.Usuario;
import ec.agrociclo.security.LimiteAcceso;
import ec.agrociclo.service.AutenticacionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
/**
 * AccesoController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class AccesoController {
    private final AutenticacionService auth;
    private final LimiteAcceso limite;

    public AccesoController(AutenticacionService auth, LimiteAcceso limite) {
        this.auth = auth;
        this.limite = limite;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody AccesoRequest datos, HttpServletRequest solicitud) {
        var ip = solicitud.getRemoteAddr();
        var vence = limite.iniciar(ip);
        var sesion = auth.entrar(datos.email(), datos.password());
        limite.confirmar(ip, vence);
        return sesion;
    }

    @GetMapping("/me")
    public Usuario perfil(@RequestAttribute Usuario usuario) { return usuario; }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) { auth.salir(autorizacion); }

}
