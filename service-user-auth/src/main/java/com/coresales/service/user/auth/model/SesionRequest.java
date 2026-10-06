package com.coresales.service.user.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body de POST /api/auth/sesion.
 * Antes era Map<String, Object>; con un DTO Swagger muestra el campo real
 * (en vez de additionalProp1, additionalProp2...).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rol elegido por el usuario para registrar su inicio de sesión.")
public class SesionRequest {

    @Schema(
            description = "Código del rol elegido (iCodigo_PerRol). Reemplace el ejemplo por el "
                    + "'codigoPersonaRol' de SU rol, que devuelve GET /api/auth/roles/{usuarioWindows}.",
            example = "21589",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer codigoPersonaRol;
}
