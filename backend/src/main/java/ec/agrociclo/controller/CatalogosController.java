package ec.agrociclo.controller;

import ec.agrociclo.dto.*;
import ec.agrociclo.service.CatalogosService;
import ec.agrociclo.service.ArchivosTerrenoService;

import ec.agrociclo.model.TipoCatalogo;
import ec.agrociclo.model.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
/**
 * CatalogosController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class CatalogosController {
    private final CatalogosService catalogos;
    private final ArchivosTerrenoService archivos;

    public CatalogosController(CatalogosService catalogos, ArchivosTerrenoService archivos) {
        this.catalogos = catalogos;
        this.archivos = archivos;
    }

    @GetMapping("/{tipo:terrenos|cultivos|insumos}")
    public List<?> listar(@PathVariable String tipo) { return catalogos.listar(TipoCatalogo.de(tipo)); }

    @PostMapping("/terrenos")
    @ResponseStatus(HttpStatus.CREATED)
    public Object terreno(@RequestAttribute Usuario usuario, @Valid @RequestBody TerrenoRequest datos) {
        return catalogos.crear(usuario, TipoCatalogo.TERRENOS, datos.datos());
    }

    @PutMapping("/terrenos/{id}")
    public Object terreno(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody TerrenoRequest datos) {
        return catalogos.editar(usuario, TipoCatalogo.TERRENOS, id, datos.datos());
    }

    @PostMapping(value = "/terrenos/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object imagenTerreno(@RequestAttribute Usuario usuario, @PathVariable int id,
                                @RequestParam("archivo") MultipartFile archivo) throws java.io.IOException {
        String url = archivos.guardar(archivo);
        try {
            return catalogos.actualizarImagenTerreno(usuario, id, url);
        } catch (RuntimeException error) {
            archivos.eliminar(url);
            throw error;
        }
    }

    @PostMapping("/cultivos")
    @ResponseStatus(HttpStatus.CREATED)
    public Object cultivo(@RequestAttribute Usuario usuario, @Valid @RequestBody CultivoRequest datos) {
        return catalogos.crear(usuario, TipoCatalogo.CULTIVOS, datos.datos());
    }

    @PutMapping("/cultivos/{id}")
    public Object cultivo(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody CultivoRequest datos) {
        return catalogos.editar(usuario, TipoCatalogo.CULTIVOS, id, datos.datos());
    }

    @PostMapping("/insumos")
    @ResponseStatus(HttpStatus.CREATED)
    public Object insumo(@RequestAttribute Usuario usuario, @Valid @RequestBody InsumoRequest datos) {
        return catalogos.crear(usuario, TipoCatalogo.INSUMOS, datos.datos());
    }

    @PutMapping("/insumos/{id}")
    public Object insumo(@RequestAttribute Usuario usuario, @PathVariable int id, @Valid @RequestBody InsumoRequest datos) {
        return catalogos.editar(usuario, TipoCatalogo.INSUMOS, id, datos.datos());
    }

    @DeleteMapping("/{tipo:terrenos|cultivos|insumos}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestAttribute Usuario usuario, @PathVariable String tipo, @PathVariable int id) {
        catalogos.borrar(usuario, TipoCatalogo.de(tipo), id);
    }

}
