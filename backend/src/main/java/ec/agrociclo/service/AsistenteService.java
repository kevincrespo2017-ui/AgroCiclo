package ec.agrociclo.service;

import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.dto.AsistenteRequest.MensajeChat;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
/**
 * AsistenteService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class AsistenteService {
    private final Environment env;
    private final RestClient cliente = RestClient.create();
    public AsistenteService(Environment env) { this.env = env; }

    @SuppressWarnings("unchecked")
    public Map<String,String> responder(String pregunta, List<MensajeChat> historial) {
        String clave = env.getProperty("GROQ_API_KEY", "").strip();
        if (clave.isBlank()) throw new ErrorApi(503, "IA_NO_CONFIGURADA", "El asistente todavía no está configurado. El administrador debe agregar GROQ_API_KEY en application.properties.");
        String modelo = env.getProperty("GROQ_MODEL", "openai/gpt-oss-120b");
        var mensajes = new java.util.ArrayList<Map<String,String>>();
        mensajes.add(Map.of("role", "system", "content", "Eres AgroCiclo AI, un asistente de apoyo agrícola en español. Responde de forma clara, práctica y prudente. Ayuda con gestión de terrenos, planificación de cultivos, suelo, riego, fertilización, plagas, cosechas e interpretación de funciones de AgroCiclo. No inventes datos de la cuenta ni afirmes haber consultado información privada. Para diagnósticos de plagas o enfermedades, explica que la identificación definitiva requiere revisión local y recomienda consultar a un técnico agrícola cuando corresponda. Usa listas breves cuando ayuden."));
        if (historial != null) {
            historial.stream().filter(m -> m != null && ("user".equals(m.rol()) || "assistant".equals(m.rol())))
                .limit(8).forEach(m -> mensajes.add(Map.of("role", m.rol(), "content", m.contenido().strip())));
        }
        mensajes.add(Map.of("role", "user", "content", pregunta.strip()));
        var cuerpo = Map.of(
            "model", modelo,
            "temperature", 0.35,
            "max_completion_tokens", 700,
            "messages", mensajes
        );
        try {
            Map<?,?> respuesta = cliente.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + clave)
                .header("Content-Type", "application/json")
                .body(cuerpo)
                .retrieve().body(Map.class);
            if (respuesta == null || !(respuesta.get("choices") instanceof List<?> opciones) || opciones.isEmpty())
                throw new ErrorApi(502, "IA_RESPUESTA_INVALIDA", "El asistente no devolvió una respuesta válida.");
            Object mensaje = ((Map<?,?>) opciones.get(0)).get("message");
            Object contenido = mensaje instanceof Map<?,?> m ? m.get("content") : null;
            if (!(contenido instanceof String texto) || texto.isBlank())
                throw new ErrorApi(502, "IA_RESPUESTA_VACIA", "El asistente no generó contenido. Intenta nuevamente.");
            return Map.of("respuesta", texto.strip());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429)
                throw new ErrorApi(429, "IA_LIMITE", "Se alcanzó el límite gratuito de consultas de IA. Espera un momento o vuelve a intentarlo más tarde.");
            throw new ErrorApi(502, "IA_PROVEEDOR", "El servicio de IA no está disponible en este momento. Revisa la clave y el modelo configurados.");
        } catch (ErrorApi e) {
            throw e;
        } catch (Exception e) {
            throw new ErrorApi(502, "IA_CONEXION", "No se pudo conectar con el asistente. Revisa la conexión y vuelve a intentar.");
        }
    }
}
