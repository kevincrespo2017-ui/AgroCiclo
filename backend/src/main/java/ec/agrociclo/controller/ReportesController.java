package ec.agrociclo.controller;

import ec.agrociclo.service.ReportesService;
import ec.agrociclo.service.FichaPdfService;

import ec.agrociclo.repository.ConsultaCiclos;
import ec.agrociclo.dto.Pagina;
import ec.agrociclo.model.Usuario;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
/**
 * ReportesController: expone indicadores, listados paginados, fichas PDF por ciclo,
 * reportes PDF consolidados y consulta de auditoría.
 */
public class ReportesController {
    private final ConsultaCiclos ciclos;
    private final ReportesService reportes;
    private final FichaPdfService ficha;

    public ReportesController(ConsultaCiclos ciclos, ReportesService reportes, FichaPdfService ficha) {
        this.ciclos = ciclos;
        this.reportes = reportes;
        this.ficha = ficha;
    }

    @GetMapping("/reportes/indicadores")
    public Map<String, Object> indicadores() { return reportes.indicadores(); }

    @GetMapping("/alertas")
    public List<Map<String, Object>> alertas() { return reportes.alertas(); }

    @GetMapping("/reportes/ciclos")
    public Pagina ciclos(@RequestParam Map<String, String> consulta) { return ciclos.buscar(consulta); }

    @GetMapping("/reportes/ciclos/{id}/pdf")
    public ResponseEntity<byte[]> ficha(@PathVariable int id) throws IOException {
        var resumen = ciclos.resumen(id);
        var archivo = ContentDisposition.attachment().filename("AgroCiclo-" + resumen.get("codigo") + ".pdf").build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, archivo.toString()).body(ficha.generar(resumen));
    }

    @GetMapping("/reportes/general.pdf")
    public ResponseEntity<byte[]> reporteGeneral(@RequestParam String tipo) throws IOException {
        byte[] contenido = reportes.pdfGeneral(tipo);
        var archivo = ContentDisposition.attachment().filename("AgroCiclo-reporte-" + tipo + ".pdf").build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, archivo.toString()).body(contenido);
    }

    @GetMapping("/auditoria")
    public Pagina auditoria(@RequestAttribute Usuario usuario, @RequestParam Map<String, String> consulta) {
        return reportes.auditoria(usuario, consulta);
    }
}
