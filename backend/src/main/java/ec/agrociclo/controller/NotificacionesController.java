package ec.agrociclo.controller;

import ec.agrociclo.model.Usuario;
import ec.agrociclo.service.NotificacionesService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
/**
 * NotificacionesController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class NotificacionesController {
    private final NotificacionesService servicio;
    public NotificacionesController(NotificacionesService servicio) { this.servicio = servicio; }

    @GetMapping
    public Map<String,Object> listar(@RequestAttribute Usuario usuario) { return servicio.listar(usuario); }

    @PatchMapping("/{id}/leer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcar(@RequestAttribute Usuario usuario, @PathVariable long id) {
        servicio.marcar(usuario, id);
    }

    @PatchMapping("/leer-todas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodas(@RequestAttribute Usuario usuario) { servicio.marcarTodas(usuario); }
}
