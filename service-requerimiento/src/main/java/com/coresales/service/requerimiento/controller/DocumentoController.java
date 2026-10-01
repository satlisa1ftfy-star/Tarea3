package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.config.DocumentoStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/requerimiento/documentos")
@CrossOrigin("http://localhost:5173")
@Tag(name = "Gestión de Requerimientos", description = "Servicios de Gestión de Requerimientos")
public class DocumentoController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final DocumentoStorage storage;

    public DocumentoController(DocumentoStorage storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/temporal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir archivo temporal",
            description = "Guarda el archivo y devuelve archivoTemporalId para enviarlo en archivosAdjuntos al registrar.")
    public ResponseEntity<Map<String, Object>> subir(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            return ResponseEntity.badRequest().build();
        }
        String id = storage.guardarTemporal(file);
        return ResponseEntity.ok(Map.of(
                "archivoTemporalId", id,
                "nombreOriginal", file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                "tamano", file.getSize()));
    }
}
