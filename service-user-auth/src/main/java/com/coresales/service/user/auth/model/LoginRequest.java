package com.coresales.service.user.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body de POST /api/auth/login.
 * Antes era Map<String, String>; con un DTO Swagger muestra el campo real
 * (en vez de additionalProp1, additionalProp2...).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para iniciar sesión con el usuario de dominio (Windows).")
public class LoginRequest {

    @Schema(
            description = "Usuario de dominio (Windows).",
            example = "JGUILLEN",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String usuarioWindows;
}
