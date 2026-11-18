package ec.agrociclo.controller;

import ec.agrociclo.model.*;
import ec.agrociclo.repository.ConsultaCiclos;
import ec.agrociclo.service.CiclosService;
import ec.agrociclo.dto.*;

import ec.agrociclo.dto.Pagina;
import ec.agrociclo.model.Usuario;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ciclos")
/**
 * CiclosController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class CiclosController {
    private final CiclosService ciclos;
    private final ConsultaCiclos consulta;

    public CiclosController(CiclosService ciclos, ConsultaCiclos consulta) {
        this.ciclos = ciclos;
        this.consulta = consulta;
    }

    @GetMapping
    public Pagina listar(@RequestParam Map<String, String> filtros) { return consulta.buscar(filtros); }

    @GetMapping("/{id}")
    public Map<String, Object> detalle(@PathVariable int id) { return ciclos.detalle(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ciclo planificar(@RequestAttribute Usuario usuario, @Valid @RequestBody PlanRequest datos) {
        return ciclos.crear(usuario, datos);
    }

    @PutMapping("/{id}")
    public Ciclo editar(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody PlanRequest datos) {
        return ciclos.editar(usuario, id, datos);
    }

    @PostMapping("/{id}/sembrar")
    public Ciclo sembrar(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody FechaRequest datos) {
        return ciclos.sembrar(usuario, id, datos.fecha());
    }

    @PostMapping("/{id}/cerrar")
    public Ciclo cerrar(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody FechaRequest datos) {
        return ciclos.cerrar(usuario, id, datos.fecha());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestAttribute Usuario usuario, @PathVariable int id) { ciclos.eliminar(usuario, id); }
}
