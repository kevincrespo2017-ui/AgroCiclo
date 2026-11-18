package ec.agrociclo.controller;

import ec.agrociclo.dto.AsistenteRequest;
import ec.agrociclo.model.Usuario;
import ec.agrociclo.security.LimiteAcceso;
import ec.agrociclo.service.AsistenteService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asistente")
/**
 * AsistenteController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class AsistenteController {
    private final AsistenteService servicio;
    private final LimiteAcceso limite;
    public AsistenteController(AsistenteService servicio, LimiteAcceso limite) { this.servicio = servicio; this.limite = limite; }
    @PostMapping("/chat")
    public Map<String,String> chat(@RequestAttribute Usuario usuario, @Valid @RequestBody AsistenteRequest datos) {
        limite.iniciar("asistente:" + usuario.getId());
        return servicio.responder(datos.mensaje(), datos.historial());
    }
}
