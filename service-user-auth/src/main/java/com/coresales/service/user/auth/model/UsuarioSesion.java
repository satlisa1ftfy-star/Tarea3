package com.coresales.service.user.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Confirma si un usuario de dominio (Windows) está registrado en
 * Gestión de Requerimientos y cuántos roles activos tiene asignados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Perfil del usuario autenticado.")
public class UsuarioSesion {

    @Schema(description = "Usuario de dominio (Windows).", example = "JGUILLEN")
    private String usuarioWindows;

    @Schema(description = "Cantidad de roles activos.", example = "2")
    private Integer numeroRoles;

    @Schema(description = "Código GR de la persona (iCodigo_Per).", example = "6073")
    private Integer codigoPersonaGr;

    @Schema(description = "Nombre completo.", example = "GUILLEN TAMARA JULY PATRICIA")
    private String nombreCompleto;

    @Schema(description = "Código de persona en Organización (cCodPer).", example = "0027")
    private String codigoPersonaOrganizacion;
}
