package ec.agrociclo.service;

import ec.agrociclo.model.Usuario;
import ec.agrociclo.repository.NotificacionesRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
/**
 * NotificacionesService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class NotificacionesService {
    private final NotificacionesRepository repo;
    public NotificacionesService(NotificacionesRepository repo) { this.repo = repo; }

    public Map<String,Object> listar(Usuario usuario) {
        return Map.of("datos", repo.listar(usuario), "no_leidas", repo.noLeidas(usuario));
    }
    public void marcar(Usuario usuario, long id) { repo.marcar(usuario, id); }
    public void marcarTodas(Usuario usuario) { repo.marcarTodas(usuario); }
    public void paraUsuario(int id, String titulo, String mensaje, String tipo, String ruta) {
        repo.paraUsuario(id, titulo, mensaje, tipo, ruta);
    }
    public void paraRol(String rol, String titulo, String mensaje, String tipo, String ruta) {
        repo.paraRol(rol, titulo, mensaje, tipo, ruta);
    }
}
