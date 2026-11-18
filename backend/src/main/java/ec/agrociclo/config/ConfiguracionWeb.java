package ec.agrociclo.config;

import ec.agrociclo.service.AutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Set;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
/**
 * ConfiguracionWeb: Configura los componentes y el arranque de la aplicación.
 */
public class ConfiguracionWeb implements WebMvcConfigurer {
    private static final Set<String> PUBLICAS = Set.of("/api/auth/login", "/api/auth/registro", "/api/auth/verificar", "/api/auth/reenviar", "/api/auth/recuperar", "/api/auth/restablecer", "/api/salud", "/api/openapi.json");
    private final AutenticacionService autenticacion;
    private final String[] origenes;
    private final String directorioArchivos;

    public ConfiguracionWeb(AutenticacionService autenticacion, Environment env) {
        this.autenticacion = autenticacion;
        this.directorioArchivos = env.getProperty("agrociclo.archivos.directorio", "./uploads");
        this.origenes = Arrays.stream(env.getProperty("FRONTEND_ORIGINS", "").split(","))
            .map(String::strip).filter(origen -> !origen.isEmpty() && !origen.contains("*")).toArray(String[]::new);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path ruta = Paths.get(directorioArchivos).toAbsolutePath().normalize();
        String ubicacion = ruta.toUri().toString();
        if (!ubicacion.endsWith("/")) ubicacion += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(ubicacion);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origenes)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Authorization")
            .exposedHeaders("Content-Disposition")
            .maxAge(600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                res.setHeader("X-Content-Type-Options", "nosniff");
                if (PUBLICAS.contains(req.getRequestURI()) || CorsUtils.isPreFlightRequest(req)) return true;
                req.setAttribute("usuario", autenticacion.deCabecera(req.getHeader(HttpHeaders.AUTHORIZATION)));
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
