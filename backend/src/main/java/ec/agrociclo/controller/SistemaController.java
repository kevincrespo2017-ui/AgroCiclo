package ec.agrociclo.controller;

import ec.agrociclo.exception.ErrorApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ec.agrociclo.repository.BaseDatos;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
/**
 * SistemaController: Expone endpoints REST y coordina las solicitudes HTTP del módulo.
 */
public class SistemaController {
    private final BaseDatos db;
    private final JsonNode openapi;
    private final String swaggerVersion;
    private final ApplicationAvailability estado;

    public SistemaController(BaseDatos db, ApplicationAvailability estado, @Value("${agrociclo.swagger.version}") String version) throws IOException {
        this.db = db;
        this.estado = estado;
        this.swaggerVersion = version;
        try (var recurso = new ClassPathResource("openapi.yaml").getInputStream()) {
            this.openapi = new ObjectMapper(new YAMLFactory()).readTree(recurso);
        }
    }

    @GetMapping("/api/salud")
    public Map<String, String> salud() {
        if (estado.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) {
            throw new ErrorApi(503, "API_INICIANDO", "La aplicación todavía está iniciando.");
        }
        try {
            db.una("SELECT 1");
            return Map.of("estado", "ok", "base_de_datos", "conectada");
        } catch (DataAccessException error) {
            throw new ErrorApi(503, "BASE_NO_DISPONIBLE", "La base de datos no está disponible en este momento.");
        }
    }

    @GetMapping("/api/openapi.json")
    public JsonNode openapi() { return openapi; }

    @GetMapping(value = {"/docs", "/docs/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String swagger() {
        var directorio = "/webjars/swagger-ui/" + swaggerVersion;
        return """
                <!doctype html><html lang="es"><head>
                <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>AgroCiclo | API REST</title><link rel="stylesheet" href="%s/swagger-ui.css">
                </head><body><main id="swagger-ui"></main>
                <script src="%s/swagger-ui-bundle.js"></script>
                <script>window.onload = () => SwaggerUIBundle({url: '/api/openapi.json',
                  dom_id:'#swagger-ui', persistAuthorization:false});</script>
                </body></html>
                """.formatted(directorio, directorio);
    }
}
