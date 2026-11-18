package ec.agrociclo.config;

import ec.agrociclo.repository.MapeadorModelos;
import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.service.OperacionesService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
/**
 * Arranque: Configura los componentes y el arranque de la aplicación.
 */
public class Arranque implements ApplicationRunner {
    private final BaseDatos db;
    private final Environment env;
    private final TransactionTemplate transaccion;

    public Arranque(BaseDatos db, Environment env, TransactionTemplate transaccion) {
        this.db = db;
        this.env = env;
        this.transaccion = transaccion;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        var migraciones = new PathMatchingResourcePatternResolver().getResources("classpath:db/migrations/*.sql");
        if (migraciones.length == 0) throw new IllegalStateException("Faltan las migraciones SQL.");
        Arrays.sort(migraciones, Comparator.comparing(Resource::getFilename));
        transaccion.executeWithoutResult(estado -> {
            db.instruccion("SELECT pg_advisory_xact_lock(923001)");
            db.instruccion("""
                    CREATE TABLE IF NOT EXISTS migraciones (
                      nombre text PRIMARY KEY,
                      aplicada_en timestamptz NOT NULL DEFAULT now()
                    )
                    """);
            for (var sql : migraciones) {
                if (db.una("SELECT nombre FROM migraciones WHERE nombre = ?", sql.getFilename()) != null) continue;
                db.instruccion(leer(sql));
                db.ejecutar("INSERT INTO migraciones(nombre) VALUES (?)", sql.getFilename());
            }
        });
        transaccion.executeWithoutResult(estado -> {
            db.instruccion("SELECT pg_advisory_xact_lock(923002)");
            configurarUsuario("ADMIN", "Administración");
            configurarUsuario("OPERATIVO", "Operaciones agrícolas");
            configurarUsuario("SUPERVISOR", "Supervisión agrícola");
            if (env.getProperty("SEED_DEMO_DATA", Boolean.class, false)) cargarDemo();
        });
    }

    private void configurarUsuario(String rol, String nombre) {
        var email = env.getProperty(rol + "_EMAIL", "").strip().toLowerCase(Locale.ROOT);
        var clave = env.getProperty(rol + "_PASSWORD", "");
        if (email.isEmpty() && clave.isEmpty()) return;
        if (email.length() > 254 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") ||
            clave.length() < 8 || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Configura correo y contraseña de 8 a 72 bytes para " + rol + ".");
        }
        var cuenta = db.una("SELECT rol FROM usuarios WHERE email = ?", email);
        if (cuenta == null) {
            db.ejecutar("INSERT INTO usuarios(nombre,email,password_hash,rol,verificado) VALUES (?,?,?,?,true)",
                nombre, email, new BCryptPasswordEncoder(12).encode(clave), rol);
        } else if (!rol.equals(cuenta.get("rol"))) {
            throw new IllegalArgumentException("El correo de " + rol + " ya pertenece a otro rol.");
        }
    }

    private void cargarDemo() {
        var marca = db.una("INSERT INTO cargas_demo VALUES ('agrociclo-v1') ON CONFLICT DO NOTHING RETURNING nombre");
        if (marca == null) return;
        var admin = db.una("SELECT id, nombre, email, rol FROM usuarios WHERE rol = 'ADMIN' ORDER BY id LIMIT 1");
        if (admin == null) throw new IllegalStateException("La carga ficticia requiere una cuenta administradora.");
        db.instruccion(leer(new ClassPathResource("db/demo.sql")));
        db.auditar(MapeadorModelos.usuario(admin), "CARGAR_DEMO", "cargas_demo", 1, Map.of("tipo", "Datos ficticios para sustentación"));
    }

    private static String leer(Resource recurso) {
        try {
            return recurso.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo leer " + recurso.getFilename(), error);
        }
    }
}
