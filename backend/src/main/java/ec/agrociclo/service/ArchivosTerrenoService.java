package ec.agrociclo.service;

import ec.agrociclo.exception.ErrorApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * ArchivosTerrenoService: valida y almacena fotografías de terrenos fuera del código fuente.
 * El nombre físico se genera en el servidor; nunca se utiliza el nombre enviado por el usuario.
 */
@Service
public class ArchivosTerrenoService {
    private final Path directorio;

    public ArchivosTerrenoService(@Value("${agrociclo.archivos.directorio:./uploads}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize().resolve("terrenos");
    }

    public String guardar(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) throw ErrorApi.campo("archivo", "Selecciona una fotografía.");
        if (archivo.getSize() > 5L * 1024 * 1024) throw ErrorApi.campo("archivo", "La fotografía no puede superar 5 MB.");
        byte[] contenido = archivo.getBytes();
        String extension = extensionValida(contenido);
        if (extension == null) throw ErrorApi.campo("archivo", "Formato no permitido. Utiliza JPG, PNG o WebP.");
        Files.createDirectories(directorio);
        String nombre = UUID.randomUUID() + extension;
        Path destino = directorio.resolve(nombre).normalize();
        if (!destino.startsWith(directorio)) throw ErrorApi.campo("archivo", "Nombre de archivo no válido.");
        Files.write(destino, contenido);
        return "/uploads/terrenos/" + nombre;
    }

    public void eliminar(String url) {
        if (url == null || !url.startsWith("/uploads/terrenos/")) return;
        try {
            Path archivo = directorio.resolve(Path.of(url).getFileName()).normalize();
            if (archivo.startsWith(directorio)) Files.deleteIfExists(archivo);
        } catch (Exception ignored) { /* La limpieza no debe ocultar el error original. */ }
    }

    private static String extensionValida(byte[] b) {
        if (b.length >= 3 && (b[0]&255)==0xFF && (b[1]&255)==0xD8 && (b[2]&255)==0xFF) return ".jpg";
        if (b.length >= 8 && (b[0]&255)==0x89 && b[1]==0x50 && b[2]==0x4E && b[3]==0x47) return ".png";
        if (b.length >= 12 && b[0]=='R' && b[1]=='I' && b[2]=='F' && b[3]=='F' &&
            b[8]=='W' && b[9]=='E' && b[10]=='B' && b[11]=='P') return ".webp";
        return null;
    }
}
