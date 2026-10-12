package com.coresales.service.user.auth.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de POST /api/auth/login (mismos campos que antes devolvía el Map:
 * token, expiresAt y perfil), así el frontend no cambia.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"token", "expiresAt", "perfil"})
@Schema(description = "Token JWT emitido y perfil del usuario.")
public class LoginResponse {

    @Schema(
            description = "JWT a enviar en el header Authorization: Bearer <token>.",
            example = "(aquí llega el token JWT real; cópielo en Authorize)"
    )
    private String token;

    @Schema(
            description = "Fecha y hora de expiración del token (UTC, ISO-8601).",
            example = "2026-09-30T16:53:55Z"
    )
    private String expiresAt;

    @Schema(description = "Perfil del usuario autenticado.")
    private UsuarioSesion perfil;
}
