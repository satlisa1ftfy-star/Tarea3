package com.coresales.service.user.auth.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de POST /api/auth/sesion (mismos campos que antes devolvía el Map).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"codigoIngreso", "codigoSesion", "codigoPersonaGr",
        "codigoPersonaRol", "codigoRol", "nombreRol"})
@Schema(description = "Ingreso registrado en GRMovAccesos.")
public class SesionResponse {

    @Schema(description = "Código del ingreso registrado (iCodigo_Ing).", example = "150234")
    private Integer codigoIngreso;

    @Schema(description = "Código de sesión generado (vCodigoSesion_Ac, 30 caracteres).",
            example = "3f9c1a7b2d4e4f6a8b0c1d2e3f4a5b")
    private String codigoSesion;

    @Schema(description = "Código GR de la persona (iCodigo_Per).", example = "6073")
    private Integer codigoPersonaGr;

    @Schema(description = "Código del rol elegido (iCodigo_PerRol).", example = "21589")
    private Integer codigoPersonaRol;

    @Schema(description = "Código del rol (siCodigo_Rol).", example = "3")
    private Integer codigoRol;

    @Schema(description = "Nombre del rol.", example = "Administrador")
    private String nombreRol;
}
