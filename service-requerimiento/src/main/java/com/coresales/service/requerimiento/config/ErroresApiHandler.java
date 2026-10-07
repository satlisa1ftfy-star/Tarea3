package com.coresales.service.requerimiento.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Devuelve el motivo de las validaciones (ResponseStatusException) en el cuerpo de la respuesta
 * como {"message": "..."}, que es lo que muestra el frontend. Sin esto el 400 llega sin mensaje.
 */
@RestControllerAdvice
public class ErroresApiHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> alValidar(ResponseStatusException e) {
        String motivo = e.getReason() != null ? e.getReason() : e.getStatusCode().toString();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", e.getStatusCode().value(),
                "message", motivo
        ));
    }
}
