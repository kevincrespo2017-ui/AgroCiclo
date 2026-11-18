package ec.agrociclo.controller;

import ec.agrociclo.model.*;
import ec.agrociclo.dto.MovimientoResultado;
import ec.agrociclo.service.OperacionesService;
import ec.agrociclo.dto.*;

import ec.agrociclo.dto.Pagina;
import ec.agrociclo.model.Usuario;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
/**
 * OperacionesController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class OperacionesController {
    private final OperacionesService operaciones;

    public OperacionesController(OperacionesService operaciones) { this.operaciones = operaciones; }

    @PostMapping("/ciclos/{id}/actividades")
    @ResponseStatus(HttpStatus.CREATED)
    public Actividad programar(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody ActividadRequest datos) {
        return operaciones.programar(usuario, id, datos);
    }

    @PutMapping("/ciclos/{id}/actividades/{actividadId}")
    Actividad editar(@RequestAttribute Usuario usuario, @PathVariable int id, @PathVariable int actividadId,
                                @Valid @RequestBody ActividadRequest datos) {
        return operaciones.editarActividad(usuario, id, actividadId, datos);
    }

    @PostMapping("/ciclos/{id}/actividades/{actividadId}/completar")
    Actividad completar(@RequestAttribute Usuario usuario, @PathVariable int id, @PathVariable int actividadId,
                                   @Valid @RequestBody CompletarRequest datos) {
        return operaciones.completar(usuario, id, actividadId, datos);
    }

    @DeleteMapping("/ciclos/{id}/actividades/{actividadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrarActividad(@RequestAttribute Usuario usuario, @PathVariable int id, @PathVariable int actividadId) {
        operaciones.borrarActividad(usuario, id, actividadId);
    }

    @PostMapping("/ciclos/{id}/cosechas")
    @ResponseStatus(HttpStatus.CREATED)
    public Cosecha cosecha(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody CosechaRequest datos) {
        return operaciones.cosechar(usuario, id, datos);
    }

    @DeleteMapping("/ciclos/{id}/cosechas/{cosechaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@RequestAttribute Usuario usuario, @PathVariable int id, @PathVariable int cosechaId) {
        operaciones.retirarCosecha(usuario, id, cosechaId);
    }

    @GetMapping("/insumos/{id}/movimientos")
    public Pagina movimientos(@PathVariable int id, @RequestParam Map<String, String> filtros) {
        return operaciones.movimientos(id, filtros);
    }

    @PostMapping("/insumos/{id}/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResultado entrada(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody EntradaRequest datos) {
        return operaciones.entrada(usuario, id, datos);
    }

    @PostMapping("/ciclos/{id}/consumos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResultado consumo(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody ConsumoRequest datos) {
        return operaciones.consumo(usuario, id, datos);
    }
}
