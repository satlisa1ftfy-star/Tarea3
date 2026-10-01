package com.coresales.service.requerimiento.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda los archivos adjuntos. Flujo (igual al sistema legado, que nombra el archivo
 * FileReqN{codigoRequerimiento}.{ext}):
 *   1) POST /documentos/temporal  -> se guarda en {dir}/tmp/{uuid}
 *   2) al registrar el requerimiento -> se mueve a {dir}/FileReqN{id}.{ext}
 * El directorio se configura con app.upload.dir (debe ser el mismo recurso que lee el sistema legado).
 */
@Component
public class DocumentoStorage {

    private static final Pattern UUID_RE = Pattern.compile("^[0-9a-fA-F-]{36}$");
    private static final Pattern EXT_RE = Pattern.compile("^\\.[A-Za-z0-9]{1,10}$");

    private final Path dir;

    public DocumentoStorage(@Value("${app.upload.dir:./uploads}") String directorio) {
        this.dir = Paths.get(directorio).toAbsolutePath().normalize();
    }

    public String guardarTemporal(MultipartFile archivo) throws IOException {
        Path tmp = dir.resolve("tmp");
        Files.createDirectories(tmp);
        String id = UUID.randomUUID().toString();
        archivo.transferTo(tmp.resolve(id));
        return id;
    }

    public boolean existeTemporal(String id) {
        return id != null && UUID_RE.matcher(id).matches() && Files.exists(dir.resolve("tmp").resolve(id));
    }

    public void confirmar(String idTemporal, String nombreFinal) throws IOException {
        if (!existeTemporal(idTemporal)) {
            throw new IOException("El archivo temporal no existe o expiró: " + idTemporal);
        }
        Files.createDirectories(dir);
        Files.move(dir.resolve("tmp").resolve(idTemporal), dir.resolve(nombreFinal).normalize(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    /** Extensión segura (".msg", ".docx") o "" si no es válida. */
    public static String extension(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }
        int i = nombreOriginal.lastIndexOf('.');
        String ext = i >= 0 ? nombreOriginal.substring(i) : "";
        return EXT_RE.matcher(ext).matches() ? ext.toLowerCase() : "";
    }
}
