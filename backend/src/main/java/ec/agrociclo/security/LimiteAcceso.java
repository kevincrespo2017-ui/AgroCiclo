package ec.agrociclo.security;

import ec.agrociclo.exception.ErrorApi;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
/**
 * LimiteAcceso: Aplica controles de autenticación, autorización y protección.
 */
public class LimiteAcceso {
    private static final long VENTANA = Duration.ofMinutes(5).toMillis();
    private final Map<String, Ventana> intentos = new HashMap<>();
    private long ultimaLimpieza = 0;

    public synchronized long iniciar(String ip) {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaLimpieza >= VENTANA) {
            intentos.values().removeIf(ventana -> ventana.vence <= ahora);
            ultimaLimpieza = ahora;
        }
        var anterior = intentos.get(ip);
        if (anterior == null || anterior.vence <= ahora) anterior = new Ventana(1, ahora + VENTANA);
        else {
            if (anterior.intentos >= 12) {
                throw new ErrorApi(429, "LIMITE_LOGIN", "Demasiados intentos. Espera unos minutos y vuelve a intentar.");
            }
            anterior = new Ventana(anterior.intentos + 1, anterior.vence);
        }
        intentos.put(ip, anterior);
        return anterior.vence;
    }

    public synchronized void confirmar(String ip, long vence) {
        var ventana = intentos.get(ip);
        if (ventana == null || ventana.vence != vence) return;
        if (ventana.intentos == 1) intentos.remove(ip);
        else intentos.put(ip, new Ventana(ventana.intentos - 1, ventana.vence));
    }

    /** Ventana: conserva el contador y vencimiento de una ventana de limitación. */
    private record Ventana(int intentos, long vence) {}
}
