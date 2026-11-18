package ec.agrociclo.service;

import ec.agrociclo.exception.ErrorApi;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
/**
 * InteligenciaAgricolaService: consulta pronósticos de Open-Meteo y envía fotografías agrícolas
 * al modelo multimodal de Groq para generar observaciones preventivas orientativas.
 */
public class InteligenciaAgricolaService {
    private final Environment env;
    private final RestClient http = RestClient.create();
    public InteligenciaAgricolaService(Environment env) { this.env = env; }

    public Map<?,?> clima(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180)
            throw ErrorApi.campo("coordenadas", "Las coordenadas están fuera de rango.");
        try {
            Map<?,?> r = http.get().uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m&daily=precipitation_probability_max,temperature_2m_max,temperature_2m_min&forecast_days=3&timezone=auto", lat, lon)
                .retrieve().body(Map.class);
            if (r == null) throw new ErrorApi(502,"CLIMA_RESPUESTA","No se recibió información climática.");
            return r;
        } catch (RestClientResponseException e) {
            throw new ErrorApi(502,"CLIMA_PROVEEDOR","El servicio meteorológico no está disponible.");
        } catch (ErrorApi e) { throw e; }
        catch (Exception e) { throw new ErrorApi(502,"CLIMA_CONEXION","No se pudo consultar el clima. Comprueba tu conexión."); }
    }

    @SuppressWarnings("unchecked")
    public Map<String,String> analizar(String base64, String mime) {
        String key = env.getProperty("GROQ_API_KEY", "").strip();
        if (key.isBlank()) throw new ErrorApi(503,"IA_NO_CONFIGURADA","Configura GROQ_API_KEY en application.properties.");
        String model = env.getProperty("GROQ_VISION_MODEL","qwen/qwen3.8-27b");
        String data = base64.contains(",") ? base64.substring(base64.indexOf(',')+1) : base64;
        if (data.length() > 7_500_000) throw ErrorApi.campo("imagenBase64","La imagen supera el tamaño permitido.");
        Map<String,Object> image = Map.of("type","image_url","image_url",Map.of("url","data:"+mime+";base64,"+data));
        Map<String,Object> text = Map.of("type","text","text","Analiza esta fotografía agrícola. Describe únicamente lo que se observa, posibles signos de plaga, enfermedad o deficiencia y recomendaciones preventivas prudentes. Si no hay evidencia suficiente, dilo. No presentes el resultado como diagnóstico definitivo. Responde en español.");
        Map<String,Object> body = Map.of("model",model,"temperature",0.7,"max_completion_tokens",700,
            "messages",List.of(Map.of("role","user","content",List.of(text,image))));
        try {
            Map<?,?> r = http.post().uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization","Bearer "+key).body(body).retrieve().body(Map.class);
            List<?> choices = r != null && r.get("choices") instanceof List<?> l ? l : List.of();
            if (choices.isEmpty()) throw new ErrorApi(502,"FOTO_RESPUESTA","La IA no devolvió un análisis.");
            Object message = ((Map<?,?>)choices.get(0)).get("message");
            Object content = message instanceof Map<?,?> m ? m.get("content") : null;
            if (!(content instanceof String s) || s.isBlank()) throw new ErrorApi(502,"FOTO_VACIA","No se pudo obtener el análisis.");
            return Map.of("analisis",s.strip());
        } catch (RestClientResponseException e) {
            throw new ErrorApi(e.getStatusCode().value()==429?429:502,"FOTO_PROVEEDOR",e.getStatusCode().value()==429?"Se alcanzó el límite gratuito de análisis. Intenta más tarde.":"El proveedor de IA no pudo analizar la imagen. Verifica el modelo configurado.");
        } catch (ErrorApi e) { throw e; }
        catch (Exception e) { throw new ErrorApi(502,"FOTO_CONEXION","No se pudo conectar con el servicio de análisis de imágenes."); }
    }
}
