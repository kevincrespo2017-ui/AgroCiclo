package ec.agrociclo.controller;

import ec.agrociclo.dto.RechazoUsuarioRequest;
import ec.agrociclo.model.Usuario;
import ec.agrociclo.service.AdministracionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
/**
 * UsuariosController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class UsuariosController {
    private final AdministracionService servicio;
    public UsuariosController(AdministracionService servicio) { this.servicio = servicio; }

    @GetMapping
    public List<Map<String,Object>> listar(@RequestAttribute Usuario usuario) { return servicio.usuarios(usuario); }

    @PatchMapping("/{id}/aprobar")
    public Map<String,Object> aprobar(@RequestAttribute Usuario usuario, @PathVariable int id) {
        return servicio.aprobar(usuario, id);
    }

    @PatchMapping("/{id}/rol")
    public Map<String,Object> cambiarRol(@RequestAttribute Usuario usuario, @PathVariable int id,
                                         @RequestBody Map<String,String> datos) {
        return servicio.cambiarRol(usuario, id, datos == null ? null : datos.get("rol"));
    }

    @PatchMapping("/{id}/rechazar")
    public Map<String,Object> rechazar(@RequestAttribute Usuario usuario, @PathVariable int id,
                                       @Valid @RequestBody(required = false) RechazoUsuarioRequest datos) {
        return servicio.rechazar(usuario, id, datos == null ? null : datos.motivo());
    }
}
