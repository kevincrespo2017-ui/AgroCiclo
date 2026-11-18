package ec.agrociclo.service;
import ec.agrociclo.repository.ReportesRepository;
import ec.agrociclo.dto.Pagina;
import ec.agrociclo.model.Usuario;
import ec.agrociclo.security.Permisos;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
/**
 * ReportesService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class ReportesService {
 private final ReportesRepository repository;
 private final ReportePdfService pdf;
 public ReportesService(ReportesRepository repository, ReportePdfService pdf) { this.repository=repository; this.pdf=pdf; }
 public Map<String,Object> indicadores() { return repository.indicadores(); }
 public List<Map<String,Object>> alertas() { return repository.alertas(); }
 public byte[] pdfGeneral(String tipo) throws java.io.IOException {
  return pdf.generar(tipo, repository.reporteGeneral(tipo));
 }
 public Pagina auditoria(Usuario actor,Map<String,String> filtros) { Permisos.exigirAdmin(actor); return repository.auditoria(actor,filtros); }
}
