package ec.agrociclo.security;
import ec.agrociclo.model.Usuario;
import ec.agrociclo.exception.ErrorApi;
/**
 * Permisos: concentra las reglas de autorización para operaciones exclusivas del administrador
 * y operaciones compartidas entre administrador y supervisor.
 */
public final class Permisos {
    private Permisos() {}
    public static void exigirAdmin(Usuario usuario) {
        if (!"ADMIN".equals(usuario.getRol()))
            throw new ErrorApi(403,"PERMISO_REQUERIDO","Esta operación requiere el rol administrador.");
    }
    public static void exigirAdminOSupervisor(Usuario usuario) {
        if (!"ADMIN".equals(usuario.getRol()) && !"SUPERVISOR".equals(usuario.getRol()))
            throw new ErrorApi(403,"PERMISO_REQUERIDO","Esta operación requiere permisos de supervisión.");
    }
}
