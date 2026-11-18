package ec.agrociclo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
/**
 * Conexion: Configura los componentes y el arranque de la aplicación.
 */
public class Conexion {
    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(Environment env) {
        var config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(6);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(12_000);
        config.setIdleTimeout(60_000);
        var url = env.getProperty("DATABASE_URL", "").trim();

        if (url.startsWith("jdbc:postgresql://")) {
            config.setJdbcUrl(url);
            config.setUsername(env.getProperty("PGUSER"));
            config.setPassword(env.getProperty("PGPASSWORD"));
        } else if (StringUtils.hasText(url)) {
            configurarUrl(config, url);
        } else {
            var host = env.getProperty("PGHOST", "127.0.0.1");
            var port = env.getProperty("PGPORT", "5432");
            var nombre = env.getProperty("PGDATABASE", "agrociclo");
            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + nombre);
            config.setUsername(env.getProperty("PGUSER", "agrociclo"));
            config.setPassword(env.getRequiredProperty("PGPASSWORD"));
        }
        return new HikariDataSource(config);
    }

    private static void configurarUrl(HikariConfig config, String url) {
        var uri = URI.create(url);
        if (!"postgresql".equals(uri.getScheme()) && !"postgres".equals(uri.getScheme())) {
            throw new IllegalArgumentException("DATABASE_URL debe usar PostgreSQL.");
        }
        var datos = uri.getRawUserInfo();
        if (uri.getHost() == null || datos == null || !datos.contains(":")) {
            throw new IllegalArgumentException("DATABASE_URL requiere host, usuario y contraseña.");
        }
        var partes = datos.split(":", 2);
        var autoridad = uri.getRawAuthority();
        var hostYpuerto = autoridad.substring(autoridad.lastIndexOf('@') + 1);
        var consulta = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        config.setJdbcUrl("jdbc:postgresql://" + hostYpuerto + uri.getRawPath() + consulta);
        config.setUsername(decodificar(partes[0]));
        config.setPassword(decodificar(partes[1]));
    }

    private static String decodificar(String dato) {
        return URLDecoder.decode(dato.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
