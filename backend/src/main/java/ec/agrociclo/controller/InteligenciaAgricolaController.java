package ec.agrociclo.controller;

import ec.agrociclo.dto.FotoCultivoRequest;
import ec.agrociclo.model.Usuario;
import ec.agrociclo.service.InteligenciaAgricolaService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inteligencia-agricola")
/**
 * InteligenciaAgricolaController: publica los endpoints autenticados de clima y análisis visual
 * de cultivos, validando las coordenadas y el contenido de la fotografía recibida.
 */
public class InteligenciaAgricolaController {
    private final InteligenciaAgricolaService servicio;
    public InteligenciaAgricolaController(InteligenciaAgricolaService servicio) { this.servicio = servicio; }

    @GetMapping("/clima")
    public Map<?,?> clima(@RequestAttribute Usuario usuario, @RequestParam double lat, @RequestParam double lon) {
        return servicio.clima(lat, lon);
    }

    @PostMapping("/analizar-foto")
    public Map<String,String> analizar(@RequestAttribute Usuario usuario, @Valid @RequestBody FotoCultivoRequest datos) {
        return servicio.analizar(datos.imagenBase64(), datos.tipoMime());
    }
}
